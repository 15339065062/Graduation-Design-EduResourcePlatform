package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.util.DBUtil;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(urlPatterns = {"/api/chat/media/*"})
public class ChatMediaServlet extends HttpServlet {
    private static final String UPLOAD_DIR = "C:/Users/macha/Desktop/Graduation-Design-EduResourcePlatform/storage/uploads";
    private static final String CHAT_DIR = "chat";
    private static final String CHAT_THUMB_DIR = "chat_thumbs";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid path"));
            return;
        }

        String p = pathInfo;
        boolean thumb = false;
        if (p.startsWith("/thumb/")) {
            thumb = true;
            p = p.substring("/thumb/".length());
        } else if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.contains("..") || p.contains("/") || p.contains("\\")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid file"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            String mimeFromDb = getMimeTypeIfAllowed(conn, userId, p, thumb);
            if (mimeFromDb == null) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Forbidden"));
                return;
            }
            req.setAttribute("mimeFromDb", mimeFromDb);
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
            return;
        }

        File file = new File(UPLOAD_DIR + File.separator + (thumb ? CHAT_THUMB_DIR : CHAT_DIR) + File.separator + p);
        if (!file.exists()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mime = (String) req.getAttribute("mimeFromDb");
        if (mime == null || mime.isBlank()) {
            mime = getServletContext().getMimeType(file.getName());
        }
        if (mime == null) mime = "application/octet-stream";

        resp.setHeader("Cache-Control", "public, max-age=31536000");
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setContentType(mime);
        resp.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file); OutputStream os = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) >= 0) {
                os.write(buf, 0, n);
            }
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private String getMimeTypeIfAllowed(Connection conn, int userId, String fileName, boolean thumb) throws SQLException {
        String field = thumb ? "thumb_file_name" : "media_file_name";
        String sql =
                "SELECT c.user1_id, c.user2_id, m.media_mime_type " +
                        "FROM chat_message m JOIN chat_conversation c ON m.conversation_id = c.id " +
                        "WHERE m." + field + " = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int u1 = rs.getInt("user1_id");
                int u2 = rs.getInt("user2_id");
                if (!(userId == u1 || userId == u2)) return null;
                return rs.getString("media_mime_type");
            }
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
        String tokenParam = req.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            if (JwtUtil.validateToken(tokenParam)) {
                return JwtUtil.getUserIdFromToken(tokenParam);
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

