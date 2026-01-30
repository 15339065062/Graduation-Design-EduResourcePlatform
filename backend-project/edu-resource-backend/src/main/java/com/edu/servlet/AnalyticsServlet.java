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
import java.sql.SQLException;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/analytics/event"})
public class AnalyticsServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);

        Integer userId = null;
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (JwtUtil.validateToken(token)) {
                userId = JwtUtil.getUserIdFromToken(token);
            }
        }

        Map<String, Object> body;
        try {
            body = JsonUtil.parseJsonMap(req);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }

        Object eventNameObj = body.get("eventName");
        if (eventNameObj == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("eventName is required"));
            return;
        }

        String eventName = String.valueOf(eventNameObj);
        Object propsObj = body.get("properties");
        String propsJson = propsObj == null ? null : JsonUtil.toJson(propsObj);

        String ip = req.getRemoteAddr();
        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 255) {
            ua = ua.substring(0, 255);
        }

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "INSERT INTO analytics_event (user_id, event_name, event_properties, ip_address, user_agent, create_time) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (userId != null) ps.setInt(1, userId); else ps.setNull(1, java.sql.Types.INTEGER);
                ps.setString(2, eventName);
                ps.setString(3, propsJson);
                ps.setString(4, ip);
                ps.setString(5, ua);
                ps.executeUpdate();
            }
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

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8081");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }
}

