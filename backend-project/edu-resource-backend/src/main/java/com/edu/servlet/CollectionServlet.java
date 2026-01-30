package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.model.Resource;
import com.edu.model.User;
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
import java.util.List;

@WebServlet("/api/collection/*")
public class CollectionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();

        if ("/toggle".equals(pathInfo)) {
            toggleCollection(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("====== CollectionServlet doGet HIT ======");
        System.out.println("Request URI: " + req.getRequestURI());
        System.out.println("Path Info: " + req.getPathInfo());
        
        setCorsHeaders(resp);
        // Prevent caching
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        resp.setHeader("Pragma", "no-cache"); // HTTP 1.0
        resp.setHeader("Expires", "0"); // Proxies
        
        String pathInfo = req.getPathInfo();

        if ("/list".equals(pathInfo)) {
            listCollections(req, resp);
        } else if ("/status".equals(pathInfo)) {
            checkStatus(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
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

    private void toggleCollection(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        ToggleCollectionRequest toggleRequest;
        try {
            toggleRequest = JsonUtil.parseJsonRequest(req, ToggleCollectionRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request body"));
            return;
        }

        Integer resourceId = toggleRequest.getResourceId();

        if (resourceId == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("resourceId is required"));
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Check if already collected
            String checkSql = "SELECT id FROM collection WHERE user_id = ? AND resource_id = ?";
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            rs = ps.executeQuery();

            boolean isCollected = rs.next();
            rs.close();
            ps.close();

            boolean newStatus;
            if (isCollected) {
                // Remove collection
                String deleteSql = "DELETE FROM collection WHERE user_id = ? AND resource_id = ?";
                ps = conn.prepareStatement(deleteSql);
                ps.setInt(1, userId);
                ps.setInt(2, resourceId);
                ps.executeUpdate();
                ps.close();
                
                newStatus = false;
            } else {
                // Add collection
                String insertSql = "INSERT INTO collection (user_id, resource_id) VALUES (?, ?)";
                ps = conn.prepareStatement(insertSql);
                ps.setInt(1, userId);
                ps.setInt(2, resourceId);
                ps.executeUpdate();
                ps.close();
                
                newStatus = true;
            }
            
            // Get new count
            String countSql = "SELECT COUNT(*) FROM collection WHERE resource_id = ?";
            ps = conn.prepareStatement(countSql);
            ps.setInt(1, resourceId);
            rs = ps.executeQuery();
            int newCount = 0;
            if (rs.next()) {
                newCount = rs.getInt(1);
            }
            rs.close();
            ps.close();

            String updateResourceSql = "UPDATE resource SET collection_count = ? WHERE id = ?";
            ps = conn.prepareStatement(updateResourceSql);
            ps.setInt(1, newCount);
            ps.setInt(2, resourceId);
            ps.executeUpdate();
            ps.close();

            conn.commit();

            JsonUtil.sendJsonResponse(resp, ApiResponse.success("Success", 
                new ToggleResponse(newStatus, newCount)));

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void listCollections(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;
        
        System.out.println("listCollections called for userId: " + userId);
        
        // Pagination parameters
        int page = 1;
        int pageSize = 20;
        try {
            String pageStr = req.getParameter("page");
            if (pageStr != null) page = Integer.parseInt(pageStr);
            String pageSizeStr = req.getParameter("pageSize");
            if (pageSizeStr != null) pageSize = Integer.parseInt(pageSizeStr);
        } catch (NumberFormatException e) {
            // Ignore invalid params, use defaults
        }
        
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100; // Limit max page size

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            
            // Get total count
            String countSql = "SELECT COUNT(*) FROM collection WHERE user_id = ?";
            ps = conn.prepareStatement(countSql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            int total = 0;
            if (rs.next()) {
                total = rs.getInt(1);
            }
            rs.close();
            ps.close();
            
            // Get data
            String sql = "SELECT r.*, u.username as uploader_name, u.nickname as uploader_nickname, c.create_time as collected_at " +
                         "FROM collection c " +
                         "LEFT JOIN resource r ON c.resource_id = r.id " +
                         "LEFT JOIN user u ON r.uploader_id = u.id " +
                         "WHERE c.user_id = ? " +
                         "ORDER BY c.create_time DESC " +
                         "LIMIT ? OFFSET ?";
            
            System.out.println("Executing SQL: " + sql + " with params: " + userId + ", " + pageSize + ", " + ((page - 1) * pageSize));
            
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, (page - 1) * pageSize);
            rs = ps.executeQuery();

            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                System.out.println("Found collection resource id: " + rs.getInt("id"));
                Resource resource = new Resource();
                resource.setId(rs.getInt("id"));
                try {
                    resource.setName(rs.getString("name"));
                } catch (SQLException e) {
                    // Fallback if column name is different
                    System.out.println("Column 'name' not found, trying 'title'");
                    resource.setName(rs.getString("title"));
                }
                resource.setDescription(rs.getString("description"));
                resource.setCategory(rs.getString("category"));
                resource.setDownloadCount(rs.getInt("download_count"));
                resource.setViewCount(rs.getInt("view_count"));
                try {
                    resource.setCreatedAt(rs.getTimestamp("created_at"));
                } catch (SQLException e) {
                    // Fallback
                    System.out.println("Column 'created_at' not found, trying 'create_time'");
                    resource.setCreatedAt(rs.getTimestamp("create_time"));
                }
                
                User uploader = new User();
                int uploaderId = rs.getInt("uploader_id");
                if (rs.wasNull()) {
                    System.out.println("Uploader is null for resource " + resource.getId());
                    uploader.setUsername("Unknown");
                    uploader.setNickname("Unknown");
                } else {
                    uploader.setId(uploaderId);
                    uploader.setUsername(rs.getString("uploader_name"));
                    uploader.setNickname(rs.getString("uploader_nickname"));
                }
                resource.setUploader(uploader);
                
                resources.add(resource);
            }
            
            System.out.println("Total resources found: " + resources.size());

            JsonUtil.sendJsonResponse(resp, ApiResponse.success(new PageResponse<>(resources, total, page, pageSize)));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    // Helper class for pagination response
    private static class PageResponse<T> {
        private List<T> list;
        private int total;
        private int page;
        private int pageSize;
        
        public PageResponse(List<T> list, int total, int page, int pageSize) {
            this.list = list;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
        
        // Getters needed for JsonUtil serialization
        public List<T> getList() { return list; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
    }

    private void checkStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = getUserIdFromRequest(req, resp);
        if (userId == null) return;

        String resourceIdStr = req.getParameter("resourceId");
        if (resourceIdStr == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("resourceId is required"));
            return;
        }

        int resourceId;
        try {
            resourceId = Integer.parseInt(resourceIdStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resourceId"));
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT COUNT(*) FROM collection WHERE user_id = ? AND resource_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            rs = ps.executeQuery();

            boolean isCollected = false;
            if (rs.next()) {
                isCollected = rs.getInt(1) > 0;
            }

            JsonUtil.sendJsonResponse(resp, ApiResponse.success(isCollected));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
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

    private static class ToggleResponse {
        private boolean isCollected;
        private int count;

        public ToggleResponse(boolean isCollected, int count) {
            this.isCollected = isCollected;
            this.count = count;
        }

        public boolean isCollected() {
            return isCollected;
        }

        public int getCount() {
            return count;
        }
    }

    private static class ToggleCollectionRequest {
        private Integer resourceId;

        public Integer getResourceId() {
            return resourceId;
        }

        public void setResourceId(Integer resourceId) {
            this.resourceId = resourceId;
        }
    }
}
