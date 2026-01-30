package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.util.DBUtil;
import com.edu.util.HtmlSanitizer;
import com.edu.util.ImageProcessor;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;
import com.edu.util.NotificationUtil;
import com.edu.util.SensitiveWordUtil;

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
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/api/comment/v2", "/api/comment/v2/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 5,
        maxRequestSize = 1024 * 1024 * 16
)
public class CommentV2Servlet extends HttpServlet {
    private static final String UPLOAD_DIR = "C:/Users/macha/Desktop/Graduation-Design-EduResourcePlatform/storage/uploads";
    private static final String COMMENT_DIR = "comments";
    private static final String COMMENT_THUMB_DIR = "comments_thumbs";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            listRootComments(req, resp);
            return;
        }
        if (pathInfo.matches("/\\d+/replies")) {
            listReplies(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            createComment(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            updateComment(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            deleteComment(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void listRootComments(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String resourceIdStr = req.getParameter("resourceId");
        if (resourceIdStr == null || resourceIdStr.isBlank()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("resourceId is required"));
            return;
        }

        int page = 1;
        int pageSize = 20;
        try {
            String pageStr = req.getParameter("page");
            if (pageStr != null) page = Integer.parseInt(pageStr);
            String pageSizeStr = req.getParameter("pageSize");
            if (pageSizeStr != null) pageSize = Integer.parseInt(pageSizeStr);
        } catch (NumberFormatException ignored) {
        }
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 50) pageSize = 50;
        int offset = (page - 1) * pageSize;

        int resourceId;
        try {
            resourceId = Integer.parseInt(resourceIdStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resourceId"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM comment WHERE resource_id = ? AND parent_id IS NULL")) {
                ps.setInt(1, resourceId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql = "SELECT c.id, c.resource_id, c.user_id, c.content, c.parent_id, c.reply_to_user_id, c.root_id, c.image_count, c.create_time, " +
                    "u.username, u.nickname, u.avatar " +
                    "FROM comment c LEFT JOIN user u ON c.user_id = u.id " +
                    "WHERE c.resource_id = ? AND c.parent_id IS NULL " +
                    "ORDER BY c.create_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> roots = new ArrayList<>();
            List<Integer> rootIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, resourceId);
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> c = mapCommentRow(rs);
                        int id = rs.getInt("id");
                        rootIds.add(id);
                        roots.add(c);
                    }
                }
            }

            Map<Integer, Integer> replyCounts = new HashMap<>();
            if (!rootIds.isEmpty()) {
                String ids = rootIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                String countSql = "SELECT root_id, COUNT(*) AS cnt FROM comment WHERE root_id IN (" + ids + ") GROUP BY root_id";
                try (PreparedStatement ps = conn.prepareStatement(countSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int rid = rs.getInt("root_id");
                        int cnt = rs.getInt("cnt");
                        replyCounts.put(rid, Math.max(0, cnt - 1));
                    }
                }
            }

            Map<Integer, List<Map<String, Object>>> previewReplies = new HashMap<>();
            if (!rootIds.isEmpty()) {
                String ids = rootIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                String replySql =
                        "SELECT * FROM (" +
                                "SELECT c.id, c.resource_id, c.user_id, c.content, c.parent_id, c.reply_to_user_id, c.root_id, c.image_count, c.create_time, " +
                                "u.username, u.nickname, u.avatar, ru.nickname AS reply_to_nickname, ru.username AS reply_to_username, " +
                                "ROW_NUMBER() OVER (PARTITION BY c.parent_id ORDER BY c.create_time ASC) AS rn " +
                                "FROM comment c " +
                                "LEFT JOIN user u ON c.user_id = u.id " +
                                "LEFT JOIN user ru ON c.reply_to_user_id = ru.id " +
                                "WHERE c.parent_id IN (" + ids + ") " +
                                ") t WHERE t.rn <= 2 ORDER BY t.parent_id ASC, t.create_time ASC";
                try (PreparedStatement ps = conn.prepareStatement(replySql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> r = mapCommentRowWithReplyTo(rs);
                        int pid = rs.getInt("parent_id");
                        previewReplies.computeIfAbsent(pid, k -> new ArrayList<>()).add(r);
                    }
                }
            }

            Map<Integer, List<Map<String, Object>>> imagesByComment = loadImagesByCommentIds(conn, rootIds, previewReplies);

            for (Map<String, Object> c : roots) {
                int id = (int) c.get("id");
                c.put("replyCount", replyCounts.getOrDefault(id, 0));
                List<Map<String, Object>> previews = previewReplies.getOrDefault(id, new ArrayList<>());
                for (Map<String, Object> r : previews) {
                    int rid = (int) r.get("id");
                    r.put("images", imagesByComment.getOrDefault(rid, new ArrayList<>()));
                }
                c.put("previewReplies", previews);
                c.put("images", imagesByComment.getOrDefault(id, new ArrayList<>()));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("list", roots);
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

    private void listReplies(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        String[] parts = pathInfo.split("/");
        int parentId = Integer.parseInt(parts[1]);

        int page = 1;
        int pageSize = 20;
        try {
            String pageStr = req.getParameter("page");
            if (pageStr != null) page = Integer.parseInt(pageStr);
            String pageSizeStr = req.getParameter("pageSize");
            if (pageSizeStr != null) pageSize = Integer.parseInt(pageSizeStr);
        } catch (NumberFormatException ignored) {
        }
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 50) pageSize = 50;
        int offset = (page - 1) * pageSize;

        try (Connection conn = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM comment WHERE parent_id = ?")) {
                ps.setInt(1, parentId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql = "SELECT c.id, c.resource_id, c.user_id, c.content, c.parent_id, c.reply_to_user_id, c.root_id, c.image_count, c.create_time, " +
                    "u.username, u.nickname, u.avatar, ru.nickname AS reply_to_nickname, ru.username AS reply_to_username " +
                    "FROM comment c " +
                    "LEFT JOIN user u ON c.user_id = u.id " +
                    "LEFT JOIN user ru ON c.reply_to_user_id = ru.id " +
                    "WHERE c.parent_id = ? ORDER BY c.create_time ASC LIMIT ? OFFSET ?";
            List<Map<String, Object>> list = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, parentId);
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> c = mapCommentRowWithReplyTo(rs);
                        int id = rs.getInt("id");
                        ids.add(id);
                        list.add(c);
                    }
                }
            }

            Map<Integer, List<Map<String, Object>>> images = loadImagesByCommentIds(conn, ids);
            for (Map<String, Object> c : list) {
                int id = (int) c.get("id");
                c.put("images", images.getOrDefault(id, new ArrayList<>()));
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

    private void createComment(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        Integer resourceId;
        Integer parentId;
        Integer replyToUserId;
        String content;
        List<Part> imageParts = new ArrayList<>();

        String ct = req.getContentType();
        boolean isMultipart = ct != null && ct.toLowerCase().startsWith("multipart/");
        if (isMultipart) {
            resourceId = parseIntParam(req.getParameter("resourceId"));
            parentId = parseIntParam(req.getParameter("parentId"));
            replyToUserId = parseIntParam(req.getParameter("replyToUserId"));
            content = req.getParameter("content");
            if (content == null) content = "";
            for (Part part : req.getParts()) {
                if ("images".equals(part.getName()) && part.getSize() > 0) {
                    imageParts.add(part);
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
            resourceId = parseIntObject(body.get("resourceId"));
            parentId = parseIntObject(body.get("parentId"));
            replyToUserId = parseIntObject(body.get("replyToUserId"));
            Object c = body.get("content");
            content = c == null ? "" : String.valueOf(c);
        }

        content = SensitiveWordUtil.filter(content);
        content = HtmlSanitizer.sanitizePlainText(content.trim());
        if (imageParts.size() > 3) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Too many images"));
            return;
        }

        if (resourceId == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("resourceId is required"));
            return;
        }
        if (content.isBlank() && imageParts.isEmpty()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("content or images is required"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!isAllowComments(conn, resourceId)) {
                    conn.rollback();
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Comments are disabled"));
                    return;
                }

                Integer rootId = null;
                if (parentId != null) {
                    rootId = getRootIdForParent(conn, parentId, resourceId);
                    if (rootId == null) {
                        conn.rollback();
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid parentId"));
                        return;
                    }
                }

                String sql = "INSERT INTO comment (resource_id, user_id, content, parent_id, reply_to_user_id, root_id, image_count, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
                int commentId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, resourceId);
                    ps.setInt(2, userId);
                    ps.setString(3, content);
                    if (parentId != null) ps.setInt(4, parentId); else ps.setNull(4, Types.INTEGER);
                    if (replyToUserId != null) ps.setInt(5, replyToUserId); else ps.setNull(5, Types.INTEGER);
                    if (rootId != null) ps.setInt(6, rootId); else ps.setNull(6, Types.INTEGER);
                    ps.setInt(7, imageParts.size());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        commentId = rs.getInt(1);
                    }
                }

                if (parentId == null) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE comment SET root_id = ? WHERE id = ?")) {
                        ps.setInt(1, commentId);
                        ps.setInt(2, commentId);
                        ps.executeUpdate();
                        rootId = commentId;
                    }
                }

                List<Map<String, Object>> savedImages = new ArrayList<>();
                for (int i = 0; i < imageParts.size(); i++) {
                    Part part = imageParts.get(i);
                    String fileName = "c_" + commentId + "_" + System.currentTimeMillis() + "_" + i + ".jpg";
                    File raw = File.createTempFile("comment_upload_", ".bin");
                    part.write(raw.getAbsolutePath());
                    String mime = ImageProcessor.detectImageMimeType(raw);
                    if (mime == null || (!mime.startsWith("image/"))) {
                        raw.delete();
                        conn.rollback();
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Unsupported image"));
                        return;
                    }

                    File out = new File(UPLOAD_DIR + File.separator + COMMENT_DIR + File.separator + fileName);
                    ImageProcessor.ProcessedImage processed = ImageProcessor.processAndSave(raw, mime, out, 1080, 1080);

                    File thumb = new File(UPLOAD_DIR + File.separator + COMMENT_THUMB_DIR + File.separator + fileName);
                    ImageProcessor.processAndSave(out, processed.mimeType(), thumb, 360, 360);

                    raw.delete();

                    String ins = "INSERT INTO comment_image (comment_id, file_name, file_path, mime_type, file_size, width, height, sort_order, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                    try (PreparedStatement ps = conn.prepareStatement(ins)) {
                        ps.setInt(1, commentId);
                        ps.setString(2, fileName);
                        ps.setString(3, fileName);
                        ps.setString(4, processed.mimeType());
                        ps.setLong(5, processed.fileSize());
                        ps.setInt(6, processed.width());
                        ps.setInt(7, processed.height());
                        ps.setInt(8, i);
                        ps.executeUpdate();
                    }

                    Map<String, Object> img = new HashMap<>();
                    img.put("url", "/api/comment/image/" + fileName);
                    img.put("thumbUrl", "/api/comment/image/thumb/" + fileName);
                    img.put("width", processed.width());
                    img.put("height", processed.height());
                    savedImages.add(img);
                }

                syncCommentCount(conn, resourceId);

                notifyUsersOnCreate(conn, resourceId, userId, parentId, replyToUserId);

                conn.commit();

                Map<String, Object> created = getCommentWithUser(conn, commentId);
                created.put("images", savedImages);
                created.put("replyCount", 0);
                created.put("previewReplies", new ArrayList<>());
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(created));
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

    private void updateComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        String idStr = req.getPathInfo().substring(1);
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid id"));
            return;
        }

        Map<String, Object> body;
        try {
            body = JsonUtil.parseJsonMap(req);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }

        Object contentObj = body.get("content");
        if (contentObj == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("content is required"));
            return;
        }
        String content = SensitiveWordUtil.filter(String.valueOf(contentObj));
        content = HtmlSanitizer.sanitizePlainText(content.trim());
        if (content.isBlank()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("content is required"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            int ownerId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM comment WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
                        return;
                    }
                    ownerId = rs.getInt(1);
                }
            }
            if (ownerId != userId && !"admin".equals(JwtUtil.getRoleFromToken(getRawToken(req)))) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Forbidden"));
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE comment SET content = ?, update_time = NOW() WHERE id = ?")) {
                ps.setString(1, content);
                ps.setInt(2, id);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.success("ok", null));
                } else {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void deleteComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        String idStr = req.getPathInfo().substring(1);
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid id"));
            return;
        }

        String role = JwtUtil.getRoleFromToken(getRawToken(req));

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int ownerId;
                int resourceId;
                try (PreparedStatement ps = conn.prepareStatement("SELECT user_id, resource_id FROM comment WHERE id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
                            return;
                        }
                        ownerId = rs.getInt("user_id");
                        resourceId = rs.getInt("resource_id");
                    }
                }
                if (ownerId != userId && !"admin".equals(role) && !isResourceOwner(conn, resourceId, userId)) {
                    conn.rollback();
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Forbidden"));
                    return;
                }

                List<String> imageFiles = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT file_name FROM comment_image WHERE comment_id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            imageFiles.add(rs.getString(1));
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comment WHERE id = ?")) {
                    ps.setInt(1, id);
                    int deleted = ps.executeUpdate();
                    if (deleted <= 0) {
                        conn.rollback();
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
                        return;
                    }
                }

                syncCommentCount(conn, resourceId);
                conn.commit();

                for (String fn : imageFiles) {
                    new File(UPLOAD_DIR + File.separator + COMMENT_DIR + File.separator + fn).delete();
                    new File(UPLOAD_DIR + File.separator + COMMENT_THUMB_DIR + File.separator + fn).delete();
                }

                JsonUtil.sendJsonResponse(resp, ApiResponse.success("ok", null));
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

    private Map<String, Object> getCommentWithUser(Connection conn, int commentId) throws SQLException {
        String sql = "SELECT c.id, c.resource_id, c.user_id, c.content, c.parent_id, c.reply_to_user_id, c.root_id, c.image_count, c.create_time, " +
                "u.username, u.nickname, u.avatar, ru.nickname AS reply_to_nickname, ru.username AS reply_to_username " +
                "FROM comment c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "LEFT JOIN user ru ON c.reply_to_user_id = ru.id " +
                "WHERE c.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new HashMap<>();
                return mapCommentRowWithReplyTo(rs);
            }
        }
    }

    private Map<String, Object> mapCommentRow(ResultSet rs) throws SQLException {
        Map<String, Object> comment = new HashMap<>();
        comment.put("id", rs.getInt("id"));
        comment.put("resourceId", rs.getInt("resource_id"));
        comment.put("userId", rs.getInt("user_id"));
        comment.put("content", rs.getString("content"));
        Object parent = rs.getObject("parent_id");
        comment.put("parentId", parent == null ? null : rs.getInt("parent_id"));
        comment.put("createTime", rs.getTimestamp("create_time").getTime());
        Map<String, Object> user = new HashMap<>();
        user.put("id", rs.getInt("user_id"));
        user.put("username", rs.getString("username"));
        user.put("nickname", rs.getString("nickname"));
        user.put("avatar", rs.getString("avatar"));
        comment.put("user", user);
        return comment;
    }

    private Map<String, Object> mapCommentRowWithReplyTo(ResultSet rs) throws SQLException {
        Map<String, Object> comment = mapCommentRow(rs);
        int replyToUserId = rs.getInt("reply_to_user_id");
        if (!rs.wasNull()) {
            Map<String, Object> replyTo = new HashMap<>();
            replyTo.put("userId", replyToUserId);
            replyTo.put("nickname", rs.getString("reply_to_nickname"));
            replyTo.put("username", rs.getString("reply_to_username"));
            comment.put("replyTo", replyTo);
        } else {
            comment.put("replyTo", null);
        }
        return comment;
    }

    private Map<Integer, List<Map<String, Object>>> loadImagesByCommentIds(Connection conn, List<Integer> rootIds, Map<Integer, List<Map<String, Object>>> previewReplies) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        ids.addAll(rootIds);
        if (previewReplies != null) {
            for (List<Map<String, Object>> replies : previewReplies.values()) {
                for (Map<String, Object> r : replies) {
                    ids.add((Integer) r.get("id"));
                }
            }
        }
        ids = ids.stream().distinct().toList();
        return loadImagesByCommentIds(conn, ids);
    }

    private Map<Integer, List<Map<String, Object>>> loadImagesByCommentIds(Connection conn, List<Integer> ids) throws SQLException {
        Map<Integer, List<Map<String, Object>>> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = "SELECT comment_id, file_name, mime_type, file_size, width, height, sort_order FROM comment_image WHERE comment_id IN (" + in + ") ORDER BY comment_id ASC, sort_order ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int commentId = rs.getInt("comment_id");
                String fileName = rs.getString("file_name");
                Map<String, Object> img = new HashMap<>();
                img.put("url", "/api/comment/image/" + fileName);
                img.put("thumbUrl", "/api/comment/image/thumb/" + fileName);
                img.put("mimeType", rs.getString("mime_type"));
                img.put("fileSize", rs.getLong("file_size"));
                img.put("width", rs.getObject("width") == null ? null : rs.getInt("width"));
                img.put("height", rs.getObject("height") == null ? null : rs.getInt("height"));
                map.computeIfAbsent(commentId, k -> new ArrayList<>()).add(img);
            }
        }
        return map;
    }

    private boolean isAllowComments(Connection conn, int resourceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT allow_comments FROM resource WHERE id = ?")) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getBoolean(1);
            }
        }
    }

    private boolean isResourceOwner(Connection conn, int resourceId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uploader_id FROM resource WHERE id = ?")) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getInt(1) == userId;
            }
        }
    }

    private Integer getRootIdForParent(Connection conn, int parentId, int resourceId) throws SQLException {
        String sql = "SELECT id, root_id, resource_id FROM comment WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                if (rs.getInt("resource_id") != resourceId) return null;
                Object rootObj = rs.getObject("root_id");
                if (rootObj == null) return rs.getInt("id");
                return rs.getInt("root_id");
            }
        }
    }

    private void syncCommentCount(Connection conn, int resourceId) throws SQLException {
        int cnt;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM comment WHERE resource_id = ?")) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                cnt = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE resource SET comment_count = ? WHERE id = ?")) {
            ps.setInt(1, cnt);
            ps.setInt(2, resourceId);
            ps.executeUpdate();
        }
    }

    private void notifyUsersOnCreate(Connection conn, int resourceId, int actorUserId, Integer parentId, Integer replyToUserId) throws SQLException {
        Integer uploaderId = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT uploader_id, title FROM resource WHERE id = ?")) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    uploaderId = rs.getInt("uploader_id");
                }
            }
        }

        if (uploaderId != null && uploaderId != actorUserId && parentId == null) {
            NotificationUtil.createResourceNotification(conn, uploaderId, "资源收到新评论", "您的资源收到了新评论");
        }

        Integer targetUserId = null;
        if (replyToUserId != null) {
            targetUserId = replyToUserId;
        } else if (parentId != null) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM comment WHERE id = ?")) {
                ps.setInt(1, parentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) targetUserId = rs.getInt(1);
                }
            }
        }

        if (targetUserId != null && targetUserId != actorUserId) {
            NotificationUtil.createResourceNotification(conn, targetUserId, "收到新回复", "有人回复了你的评论");
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

    private String getRawToken(HttpServletRequest req) {
        String token = req.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) return null;
        return token.substring(7);
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

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8081");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }
}

