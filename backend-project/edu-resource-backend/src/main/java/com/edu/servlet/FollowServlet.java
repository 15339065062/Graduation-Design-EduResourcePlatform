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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/follow/*"})
public class FollowServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
            return;
        }
        if ("/following".equals(pathInfo)) {
            listFollowing(req, resp);
            return;
        }
        if ("/followers".equals(pathInfo)) {
            listFollowers(req, resp);
            return;
        }
        if ("/status".equals(pathInfo)) {
            status(req, resp);
            return;
        }
        JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
            return;
        }
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        int targetId = Integer.parseInt(pathInfo.substring(1));
        if (targetId == userId) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Cannot follow yourself"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            if (!userExists(conn, targetId)) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("User not found"));
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO user_follow (follower_id, followee_id, create_time) VALUES (?, ?, NOW())")) {
                ps.setInt(1, userId);
                ps.setInt(2, targetId);
                ps.executeUpdate();
            }
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("ok", null));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
            return;
        }
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        int targetId = Integer.parseInt(pathInfo.substring(1));

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM user_follow WHERE follower_id = ? AND followee_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, targetId);
            ps.executeUpdate();
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("ok", null));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void listFollowing(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        int page = parseInt(req.getParameter("page"), 1);
        int pageSize = parseInt(req.getParameter("pageSize"), 30);
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        try (Connection conn = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM user_follow WHERE follower_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql = "SELECT u.id, u.username, u.nickname, u.avatar, u.role, uf.create_time " +
                    "FROM user_follow uf JOIN user u ON uf.followee_id = u.id " +
                    "WHERE uf.follower_id = ? ORDER BY uf.create_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> u = new HashMap<>();
                        u.put("id", rs.getInt("id"));
                        u.put("username", rs.getString("username"));
                        u.put("nickname", rs.getString("nickname"));
                        u.put("avatar", rs.getString("avatar"));
                        u.put("role", rs.getString("role"));
                        u.put("followTime", rs.getTimestamp("create_time").getTime());
                        list.add(u);
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

    private void listFollowers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        int page = parseInt(req.getParameter("page"), 1);
        int pageSize = parseInt(req.getParameter("pageSize"), 30);
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        try (Connection conn = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM user_follow WHERE followee_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql = "SELECT u.id, u.username, u.nickname, u.avatar, u.role, uf.create_time " +
                    "FROM user_follow uf JOIN user u ON uf.follower_id = u.id " +
                    "WHERE uf.followee_id = ? ORDER BY uf.create_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> u = new HashMap<>();
                        u.put("id", rs.getInt("id"));
                        u.put("username", rs.getString("username"));
                        u.put("nickname", rs.getString("nickname"));
                        u.put("avatar", rs.getString("avatar"));
                        u.put("role", rs.getString("role"));
                        u.put("followTime", rs.getTimestamp("create_time").getTime());
                        list.add(u);
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

    private void status(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        String targetStr = req.getParameter("userId");
        if (targetStr == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("userId is required"));
            return;
        }
        int targetId;
        try {
            targetId = Integer.parseInt(targetStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid userId"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            boolean isFollowing = existsFollow(conn, userId, targetId);
            boolean isFollowedBy = existsFollow(conn, targetId, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("isFollowing", isFollowing);
            data.put("isFollowedBy", isFollowedBy);
            data.put("isFriend", isFollowing && isFollowedBy);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private boolean existsFollow(Connection conn, int followerId, int followeeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM user_follow WHERE follower_id = ? AND followee_id = ? LIMIT 1")) {
            ps.setInt(1, followerId);
            ps.setInt(2, followeeId);
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

    private int parseInt(String v, int defaultValue) {
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8081");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }
}

