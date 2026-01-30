package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.model.OperationLog;
import com.edu.model.RoleChangeRequest;
import com.edu.model.User;
import com.edu.util.DBUtil;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;
import com.edu.util.PasswordUtil;

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
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    // Simple rate limiter: IP -> timestamp of last request
    private static final java.util.Map<String, Long> requestTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 500; // 500ms between requests

    private boolean checkRateLimit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ip = req.getRemoteAddr();
        long now = System.currentTimeMillis();
        Long lastTime = requestTimestamps.get(ip);
        
        if (lastTime != null && (now - lastTime) < RATE_LIMIT_MS) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(429, "Too many requests. Please try again later."));
            return false;
        }
        
        requestTimestamps.put(ip, now);
        // Clean up old entries periodically (omitted for simplicity, but in production use a real cache)
        if (requestTimestamps.size() > 10000) requestTimestamps.clear(); 
        
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        if (!checkRateLimit(req, resp)) return;
        if (!checkAdminPermission(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if ("/users".equals(pathInfo)) {
            listUsers(req, resp);
        } else if ("/role-requests".equals(pathInfo)) {
            listRoleRequests(req, resp);
        } else if ("/logs".equals(pathInfo)) {
            listLogs(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        if (!checkRateLimit(req, resp)) return;
        if (!checkAdminPermission(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if ("/users/status".equals(pathInfo)) {
            updateUserStatus(req, resp);
        } else if ("/users/password".equals(pathInfo)) {
            resetUserPassword(req, resp);
        } else if ("/users/role".equals(pathInfo)) {
            updateUserRole(req, resp);
        } else if ("/role-requests/audit".equals(pathInfo)) {
            auditRoleRequest(req, resp);
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

    private boolean checkAdminPermission(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
            return false;
        }
        token = token.substring(7);
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
            return false;
        }
        String role = JwtUtil.getRoleFromToken(token);
        if (!"admin".equals(role)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Access denied"));
            return false;
        }
        return true;
    }

    private Integer getCurrentAdminId(HttpServletRequest req) {
        String token = req.getHeader("Authorization").substring(7);
        return JwtUtil.getUserIdFromToken(token);
    }

    // --- User Management ---

    private boolean verifyAdminPassword(Integer adminId, String password) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT password FROM user WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, adminId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        return PasswordUtil.verifyPassword(password, hashedPassword);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void listUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT id, username, nickname, phone, role, status, create_time FROM user ORDER BY create_time DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setNickname(rs.getString("nickname"));
                    user.setPhone(rs.getString("phone"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    user.setCreatedAt(rs.getTimestamp("create_time"));
                    users.add(user);
                }
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(users));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void updateUserStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.parseJsonMap(req);
        if (body.get("userId") == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("User ID is required"));
            return;
        }
        Integer userId = ((Number) body.get("userId")).intValue();
        String status = (String) body.get("status");
        String adminPassword = (String) body.get("adminPassword");
        
        Integer adminId = getCurrentAdminId(req);
        
        // Verify admin password for sensitive operation
        if (adminPassword == null || !verifyAdminPassword(adminId, adminPassword)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Invalid password verification"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE user SET status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            logOperation(conn, getCurrentAdminId(req), "User", "Update Status", userId, "Status changed to " + status);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("User status updated", null));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void resetUserPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.parseJsonMap(req);
        if (body.get("userId") == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("User ID is required"));
            return;
        }
        Integer userId = ((Number) body.get("userId")).intValue();
        String newPassword = (String) body.get("newPassword");

        if (newPassword == null || newPassword.isEmpty()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Password required"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE user SET password = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, PasswordUtil.hashPassword(newPassword));
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            logOperation(conn, getCurrentAdminId(req), "User", "Reset Password", userId, "Password reset by admin");
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("Password reset successful", null));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void updateUserRole(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.parseJsonMap(req);
        if (body.get("userId") == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("User ID is required"));
            return;
        }
        Integer userId = ((Number) body.get("userId")).intValue();
        String role = (String) body.get("role");

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE user SET role = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, role);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            logOperation(conn, getCurrentAdminId(req), "User", "Update Role", userId, "Role changed to " + role);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("User role updated", null));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    // --- Role Request Management ---

    private void listRoleRequests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT r.*, u.username, u.nickname, a.username as auditor_name " +
                         "FROM role_change_request r " +
                         "JOIN user u ON r.user_id = u.id " +
                         "LEFT JOIN user a ON r.auditor_id = a.id " +
                         "ORDER BY r.create_time DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<RoleChangeRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    RoleChangeRequest request = new RoleChangeRequest();
                    request.setId(rs.getInt("id"));
                    request.setUserId(rs.getInt("user_id"));
                    request.setCurrentRole(rs.getString("current_role"));
                    request.setTargetRole(rs.getString("target_role"));
                    request.setReason(rs.getString("reason"));
                    request.setProofMaterial(rs.getString("proof_material"));
                    request.setStatus(rs.getString("status"));
                    request.setCreateTime(rs.getTimestamp("create_time"));
                    request.setAuditTime(rs.getTimestamp("audit_time"));
                    request.setAuditorId(rs.getInt("auditor_id"));
                    request.setAuditRemark(rs.getString("audit_remark"));
                    request.setUsername(rs.getString("username"));
                    request.setNickname(rs.getString("nickname"));
                    request.setAuditorName(rs.getString("auditor_name"));
                    requests.add(request);
                }
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(requests));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void auditRoleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.parseJsonMap(req);
        if (body.get("requestId") == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Request ID is required"));
            return;
        }
        Integer requestId = ((Number) body.get("requestId")).intValue();
        String status = (String) body.get("status"); // 'approved' or 'rejected'
        String remark = (String) body.get("remark");
        Integer adminId = getCurrentAdminId(req);

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update request status
                String updateReqSql = "UPDATE role_change_request SET status = ?, audit_time = NOW(), auditor_id = ?, audit_remark = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateReqSql)) {
                    ps.setString(1, status);
                    ps.setInt(2, adminId);
                    ps.setString(3, remark);
                    ps.setInt(4, requestId);
                    ps.executeUpdate();
                }

                // If approved, update user role
                if ("approved".equals(status)) {
                    String getUserSql = "SELECT user_id, target_role FROM role_change_request WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(getUserSql)) {
                        ps.setInt(1, requestId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                int userId = rs.getInt("user_id");
                                String targetRole = rs.getString("target_role");
                                
                                String updateUserSql = "UPDATE user SET role = ? WHERE id = ?";
                                try (PreparedStatement upPs = conn.prepareStatement(updateUserSql)) {
                                    upPs.setString(1, targetRole);
                                    upPs.setInt(2, userId);
                                    upPs.executeUpdate();
                                }
                                
                                // Send notification
                                createNotification(conn, userId, "身份申请通过", "您的身份切换申请已通过，当前身份为：" + targetRole);
                            }
                        }
                    }
                } else {
                    // Send notification for rejection
                    String getUserSql = "SELECT user_id FROM role_change_request WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(getUserSql)) {
                        ps.setInt(1, requestId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                createNotification(conn, rs.getInt("user_id"), "身份申请驳回", "您的身份切换申请被驳回。原因：" + remark);
                            }
                        }
                    }
                }

                logOperation(conn, adminId, "RoleRequest", "Audit", requestId, "Status: " + status + ", Remark: " + remark);
                conn.commit();
                JsonUtil.sendJsonResponse(resp, ApiResponse.success("Audit completed", null));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    // --- Logs ---

    private void listLogs(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT l.*, u.username FROM operation_log l LEFT JOIN user u ON l.user_id = u.id ORDER BY l.create_time DESC LIMIT 100";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<OperationLog> logs = new ArrayList<>();
                while (rs.next()) {
                    OperationLog log = new OperationLog();
                    log.setId(rs.getInt("id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setUsername(rs.getString("username"));
                    log.setModule(rs.getString("module"));
                    log.setOperation(rs.getString("operation"));
                    log.setTargetId(rs.getInt("target_id"));
                    log.setDetails(rs.getString("details"));
                    log.setIpAddress(rs.getString("ip_address"));
                    log.setCreateTime(rs.getTimestamp("create_time"));
                    logs.add(log);
                }
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(logs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    // --- Helpers ---

    private void logOperation(Connection conn, Integer userId, String module, String operation, Integer targetId, String details) throws SQLException {
        String sql = "INSERT INTO operation_log (user_id, module, operation, target_id, details, create_time) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, module);
            ps.setString(3, operation);
            if (targetId != null) ps.setInt(4, targetId); else ps.setNull(4, java.sql.Types.INTEGER);
            ps.setString(5, details);
            ps.executeUpdate();
        }
    }

    private void createNotification(Connection conn, Integer userId, String title, String content) throws SQLException {
        String sql = "INSERT INTO notification (user_id, title, content, type, create_time) VALUES (?, ?, ?, 'system', NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }
}
