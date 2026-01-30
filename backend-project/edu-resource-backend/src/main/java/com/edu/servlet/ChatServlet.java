package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.util.DBUtil;
import com.edu.util.HtmlSanitizer;
import com.edu.util.ImageProcessor;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/chat/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024L * 1024 * 50,
        maxRequestSize = 1024L * 1024 * 60
)
public class ChatServlet extends HttpServlet {
    private static final String UPLOAD_DIR = "C:/Users/macha/Desktop/Graduation-Design-EduResourcePlatform/storage/uploads";
    private static final String CHAT_DIR = "chat";
    private static final String CHAT_THUMB_DIR = "chat_thumbs";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
            return;
        }
        if ("/conversations".equals(pathInfo)) {
            listConversations(req, resp);
            return;
        }
        if ("/messages".equals(pathInfo)) {
            listMessages(req, resp);
            return;
        }
        if ("/conversation-with".equals(pathInfo)) {
            getConversationWith(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if ("/send".equals(pathInfo)) {
            sendMessage(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void listConversations(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        int page = parseInt(req.getParameter("page"), 1);
        int pageSize = parseInt(req.getParameter("pageSize"), 30);
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        try (Connection conn = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM chat_conversation WHERE user1_id = ? OR user2_id = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql =
                    "SELECT c.id, c.user1_id, c.user2_id, c.last_message_preview, c.last_message_time, " +
                            "u.id AS other_id, u.username AS other_username, u.nickname AS other_nickname, u.avatar AS other_avatar " +
                            "FROM chat_conversation c " +
                            "JOIN user u ON u.id = CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END " +
                            "WHERE c.user1_id = ? OR c.user2_id = ? " +
                            "ORDER BY COALESCE(c.last_message_time, c.create_time) DESC " +
                            "LIMIT ? OFFSET ?";
            List<Map<String, Object>> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                ps.setInt(3, userId);
                ps.setInt(4, pageSize);
                ps.setInt(5, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> c = new HashMap<>();
                        c.put("id", rs.getLong("id"));
                        c.put("lastMessagePreview", rs.getString("last_message_preview"));
                        c.put("lastMessageTime", rs.getTimestamp("last_message_time") == null ? null : rs.getTimestamp("last_message_time").getTime());
                        Map<String, Object> other = new HashMap<>();
                        other.put("id", rs.getInt("other_id"));
                        other.put("username", rs.getString("other_username"));
                        other.put("nickname", rs.getString("other_nickname"));
                        other.put("avatar", rs.getString("other_avatar"));
                        c.put("otherUser", other);
                        list.add(c);
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", (int) Math.ceil((double) total / pageSize));
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void listMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        String convStr = req.getParameter("conversationId");
        if (convStr == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("conversationId is required"));
            return;
        }
        long conversationId;
        try {
            conversationId = Long.parseLong(convStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid conversationId"));
            return;
        }

        int page = parseInt(req.getParameter("page"), 1);
        int pageSize = parseInt(req.getParameter("pageSize"), 30);
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        try (Connection conn = DBUtil.getConnection()) {
            if (!isConversationParticipant(conn, conversationId, userId)) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Forbidden"));
                return;
            }

            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM chat_message WHERE conversation_id = ?")) {
                ps.setLong(1, conversationId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql =
                    "SELECT m.id, m.conversation_id, m.sender_id, m.msg_type, m.content_text, m.media_file_name, m.media_mime_type, m.media_file_size, m.width, m.height, m.thumb_file_name, m.create_time, " +
                            "u.username, u.nickname, u.avatar " +
                            "FROM chat_message m JOIN user u ON m.sender_id = u.id " +
                            "WHERE m.conversation_id = ? ORDER BY m.create_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, conversationId);
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", rs.getLong("id"));
                        m.put("conversationId", rs.getLong("conversation_id"));
                        m.put("senderId", rs.getInt("sender_id"));
                        m.put("msgType", rs.getString("msg_type"));
                        m.put("text", rs.getString("content_text"));
                        String fileName = rs.getString("media_file_name");
                        String thumbName = rs.getString("thumb_file_name");
                        if (fileName != null && !fileName.isBlank()) {
                            m.put("mediaUrl", "/api/chat/media/" + fileName);
                        } else {
                            m.put("mediaUrl", null);
                        }
                        if (thumbName != null && !thumbName.isBlank()) {
                            m.put("thumbUrl", "/api/chat/media/thumb/" + thumbName);
                        } else {
                            m.put("thumbUrl", null);
                        }
                        m.put("mimeType", rs.getString("media_mime_type"));
                        m.put("fileSize", rs.getObject("media_file_size") == null ? null : rs.getLong("media_file_size"));
                        m.put("width", rs.getObject("width") == null ? null : rs.getInt("width"));
                        m.put("height", rs.getObject("height") == null ? null : rs.getInt("height"));
                        m.put("createTime", rs.getTimestamp("create_time").getTime());
                        Map<String, Object> sender = new HashMap<>();
                        sender.put("id", rs.getInt("sender_id"));
                        sender.put("username", rs.getString("username"));
                        sender.put("nickname", rs.getString("nickname"));
                        sender.put("avatar", rs.getString("avatar"));
                        m.put("sender", sender);
                        list.add(m);
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", (int) Math.ceil((double) total / pageSize));
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void getConversationWith(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        String otherStr = req.getParameter("userId");
        if (otherStr == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("userId is required"));
            return;
        }
        int otherId;
        try {
            otherId = Integer.parseInt(otherStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid userId"));
            return;
        }
        if (otherId == userId) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid userId"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            if (!userExists(conn, otherId)) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("User not found"));
                return;
            }
            long conversationId = getOrCreateConversation(conn, userId, otherId);
            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversationId);
            data.put("otherUserId", otherId);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void sendMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        Integer toUserId;
        String msgType;
        String text;
        Part filePart = null;

        String ct = req.getContentType();
        boolean isMultipart = ct != null && ct.toLowerCase().startsWith("multipart/");
        if (isMultipart) {
            toUserId = parseIntParam(req.getParameter("toUserId"));
            msgType = req.getParameter("msgType");
            text = req.getParameter("text");
            for (Part p : req.getParts()) {
                if ("file".equals(p.getName()) && p.getSize() > 0) {
                    filePart = p;
                    break;
                }
            }
        } else {
            Map<String, Object> body;
            try {
                body = JsonUtil.parseJsonMap(req);
            } catch (Exception e) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
                return;
            }
            Object to = body.get("toUserId");
            toUserId = parseIntObject(to);
            msgType = body.get("msgType") == null ? "text" : String.valueOf(body.get("msgType"));
            Object t = body.get("text");
            text = t == null ? "" : String.valueOf(t);
        }

        if (toUserId == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("toUserId is required"));
            return;
        }
        if (toUserId == userId) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Cannot message yourself"));
            return;
        }
        if (msgType == null || msgType.isBlank()) msgType = "text";

        text = text == null ? "" : HtmlSanitizer.sanitizePlainText(text.trim());
        if ("text".equals(msgType) && text.isBlank()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("text is required"));
            return;
        }
        if (!"text".equals(msgType) && filePart == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("file is required"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!userExists(conn, toUserId)) {
                    conn.rollback();
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("User not found"));
                    return;
                }

                long conversationId = getOrCreateConversation(conn, userId, toUserId);

                String storedFileName = null;
                String thumbFileName = null;
                String mimeType = null;
                Long fileSize = null;
                Integer width = null;
                Integer height = null;

                if (filePart != null) {
                    String originalName = filePart.getSubmittedFileName();
                    String ext = getFileExtension(originalName);
                    if (ext.isBlank()) ext = guessExtByContentType(filePart.getContentType());
                    if (ext.isBlank()) ext = "bin";
                    String baseName = "m_" + conversationId + "_" + System.currentTimeMillis();

                    if ("image".equals(msgType)) {
                        File raw = File.createTempFile("chat_upload_", ".bin");
                        filePart.write(raw.getAbsolutePath());
                        String imgMime = ImageProcessor.detectImageMimeType(raw);
                        if (imgMime == null || !imgMime.startsWith("image/")) {
                            raw.delete();
                            conn.rollback();
                            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Unsupported image"));
                            return;
                        }

                        storedFileName = baseName + ".jpg";
                        File out = new File(UPLOAD_DIR + File.separator + CHAT_DIR + File.separator + storedFileName);
                        ImageProcessor.ProcessedImage processed = ImageProcessor.processAndSave(raw, imgMime, out, 1080, 1080);
                        width = processed.width();
                        height = processed.height();
                        mimeType = processed.mimeType();
                        fileSize = processed.fileSize();

                        thumbFileName = baseName + "_t.jpg";
                        File thumb = new File(UPLOAD_DIR + File.separator + CHAT_THUMB_DIR + File.separator + thumbFileName);
                        ImageProcessor.processAndSave(out, mimeType, thumb, 360, 360);
                        raw.delete();
                    } else if ("video".equals(msgType) || "audio".equals(msgType)) {
                        if (!isAllowedMedia(msgType, ext, filePart.getContentType())) {
                            conn.rollback();
                            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Unsupported media"));
                            return;
                        }

                        storedFileName = baseName + "." + ext;
                        File out = new File(UPLOAD_DIR + File.separator + CHAT_DIR + File.separator + storedFileName);
                        out.getParentFile().mkdirs();
                        filePart.write(out.getAbsolutePath());
                        mimeType = filePart.getContentType();
                        if (mimeType == null || mimeType.isBlank()) {
                            mimeType = guessMimeByExt(ext);
                        }
                        fileSize = out.length();
                    } else {
                        conn.rollback();
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid msgType"));
                        return;
                    }
                }

                String insert = "INSERT INTO chat_message (conversation_id, sender_id, msg_type, content_text, media_file_name, media_file_path, media_mime_type, media_file_size, width, height, thumb_file_name, create_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                long messageId;
                try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, conversationId);
                    ps.setInt(2, userId);
                    ps.setString(3, msgType);
                    if (text != null && !text.isBlank()) ps.setString(4, text); else ps.setNull(4, Types.LONGVARCHAR);
                    if (storedFileName != null) ps.setString(5, storedFileName); else ps.setNull(5, Types.VARCHAR);
                    if (storedFileName != null) ps.setString(6, storedFileName); else ps.setNull(6, Types.VARCHAR);
                    if (mimeType != null) ps.setString(7, mimeType); else ps.setNull(7, Types.VARCHAR);
                    if (fileSize != null) ps.setLong(8, fileSize); else ps.setNull(8, Types.BIGINT);
                    if (width != null) ps.setInt(9, width); else ps.setNull(9, Types.INTEGER);
                    if (height != null) ps.setInt(10, height); else ps.setNull(10, Types.INTEGER);
                    if (thumbFileName != null) ps.setString(11, thumbFileName); else ps.setNull(11, Types.VARCHAR);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        messageId = rs.getLong(1);
                    }
                }

                String preview = buildPreview(msgType, text);
                try (PreparedStatement ps = conn.prepareStatement("UPDATE chat_conversation SET last_message_preview = ?, last_message_time = NOW() WHERE id = ?")) {
                    ps.setString(1, preview);
                    ps.setLong(2, conversationId);
                    ps.executeUpdate();
                }

                conn.commit();

                Map<String, Object> data = new HashMap<>();
                data.put("id", messageId);
                data.put("conversationId", conversationId);
                data.put("senderId", userId);
                data.put("msgType", msgType);
                data.put("text", text);
                if (storedFileName != null) data.put("mediaUrl", "/api/chat/media/" + storedFileName); else data.put("mediaUrl", null);
                if (thumbFileName != null) data.put("thumbUrl", "/api/chat/media/thumb/" + thumbFileName); else data.put("thumbUrl", null);
                data.put("mimeType", mimeType);
                data.put("fileSize", fileSize);
                data.put("width", width);
                data.put("height", height);
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private long getOrCreateConversation(Connection conn, int a, int b) throws SQLException {
        int u1 = Math.min(a, b);
        int u2 = Math.max(a, b);
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM chat_conversation WHERE user1_id = ? AND user2_id = ?")) {
            ps.setInt(1, u1);
            ps.setInt(2, u2);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO chat_conversation (user1_id, user2_id, create_time, update_time) VALUES (?, ?, NOW(), NOW())", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, u1);
            ps.setInt(2, u2);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private boolean isConversationParticipant(Connection conn, long conversationId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM chat_conversation WHERE id = ? AND (user1_id = ? OR user2_id = ?)")) {
            ps.setLong(1, conversationId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean userExists(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM user WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String buildPreview(String msgType, String text) {
        if ("text".equals(msgType)) {
            String s = text == null ? "" : text.trim();
            if (s.length() > 50) s = s.substring(0, 50);
            return s;
        }
        if ("image".equals(msgType)) return "[图片]";
        if ("video".equals(msgType)) return "[视频]";
        if ("audio".equals(msgType)) return "[语音]";
        return "[消息]";
    }

    private boolean isAllowedMedia(String msgType, String ext, String contentType) {
        String e = ext == null ? "" : ext.toLowerCase();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if ("video".equals(msgType)) {
            if (e.equals("mp4") || e.equals("webm") || e.equals("mov") || e.equals("ogg")) return true;
            return ct.startsWith("video/");
        }
        if ("audio".equals(msgType)) {
            if (e.equals("mp3") || e.equals("wav") || e.equals("ogg")) return true;
            return ct.startsWith("audio/");
        }
        return false;
    }

    private String guessMimeByExt(String ext) {
        String e = ext == null ? "" : ext.toLowerCase();
        if ("mp4".equals(e)) return "video/mp4";
        if ("webm".equals(e)) return "video/webm";
        if ("mov".equals(e)) return "video/quicktime";
        if ("ogg".equals(e)) return "video/ogg";
        if ("mp3".equals(e)) return "audio/mpeg";
        if ("wav".equals(e)) return "audio/wav";
        return "application/octet-stream";
    }

    private String guessExtByContentType(String contentType) {
        if (contentType == null) return "";
        String ct = contentType.toLowerCase();
        if (ct.contains("mp4")) return "mp4";
        if (ct.contains("webm")) return "webm";
        if (ct.contains("quicktime")) return "mov";
        if (ct.contains("ogg")) return "ogg";
        if (ct.contains("mpeg")) return "mp3";
        if (ct.contains("wav")) return "wav";
        return "";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    private Integer parseIntParam(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntObject(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        String s = String.valueOf(v);
        if ("null".equalsIgnoreCase(s)) return null;
        return parseIntParam(s);
    }

    private int parseInt(String v, int defaultValue) {
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer getUserIdFromRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (JwtUtil.validateToken(token)) {
                return JwtUtil.getUserIdFromToken(token);
            }
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
        return null;
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8081");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }
}

