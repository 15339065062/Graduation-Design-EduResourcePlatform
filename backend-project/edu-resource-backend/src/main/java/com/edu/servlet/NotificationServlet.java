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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/notification/*"})
public class NotificationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            listNotifications(req, resp);
            return;
        }
        if ("/unread-count".equals(pathInfo)) {
            unreadCount(req, resp);
            return;
        }
        if ("/stream".equals(pathInfo)) {
            stream(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if ("/mark-read".equals(pathInfo)) {
            markRead(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void listNotifications(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

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
        if (pageSize > 100) pageSize = 100;
        int offset = (page - 1) * pageSize;

        try (Connection conn = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notification WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql = "SELECT id, title, content, type, is_read, create_time FROM notification WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> n = new HashMap<>();
                        n.put("id", rs.getInt("id"));
                        n.put("title", rs.getString("title"));
                        n.put("content", rs.getString("content"));
                        n.put("type", rs.getString("type"));
                        n.put("isRead", rs.getBoolean("is_read"));
                        n.put("createTime", rs.getTimestamp("create_time").getTime());
                        list.add(n);
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

    private void unreadCount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(rs.getInt(1)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void markRead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        Map<String, Object> body;
        try {
            body = JsonUtil.parseJsonMap(req);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }

        Object idObj = body.get("id");
        if (idObj == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("id is required"));
            return;
        }
        int id = ((Number) idObj).intValue();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE notification SET is_read = 1 WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.success("ok", null));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Notification not found"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void stream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        resp.flushBuffer();

        long start = System.currentTimeMillis();
        long timeoutMs = 25000;
        try (PrintWriter writer = resp.getWriter()) {
            int lastUnread = -1;
            while (System.currentTimeMillis() - start < timeoutMs) {
                int unread = 0;
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        unread = rs.getInt(1);
                    }
                } catch (SQLException e) {
                    unread = lastUnread;
                }

                if (unread != lastUnread) {
                    writer.write("event: unreadCount\n");
                    writer.write("data: " + unread + "\n\n");
                    writer.flush();
                    lastUnread = unread;
                } else {
                    writer.write("event: ping\n");
                    writer.write("data: 1\n\n");
                    writer.flush();
                }

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException ignored) {
                    break;
                }
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

