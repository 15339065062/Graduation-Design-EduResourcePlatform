package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.util.DBUtil;
import com.edu.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;

@WebServlet(urlPatterns = {"/api/health"})
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection conn = DBUtil.getConnection()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("ok", null));
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(500, "db_down"));
        }
    }
}

