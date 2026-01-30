package com.edu.servlet;

import com.edu.dto.ChangePasswordRequest;
import com.edu.dto.LoginRequest;
import com.edu.dto.RegisterRequest;
import com.edu.dto.UpdateProfileRequest;
import com.edu.model.ApiResponse;
import com.edu.model.User;
import com.edu.util.DBUtil;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;
import com.edu.util.PasswordUtil;

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
import java.util.List;
import java.util.Map;

@WebServlet("/api/user/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 1024 * 1024 * 10,  // 10 MB
    maxRequestSize = 1024 * 1024 * 15 // 15 MB
)
public class UserServlet extends HttpServlet {

    private static final String UPLOAD_DIR = "C:/Users/macha/Desktop/Graduation-Design-EduResourcePlatform/storage/uploads";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if ("/login".equals(pathInfo)) {
            login(req, resp);
        } else if ("/register".equals(pathInfo)) {
            register(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            logout(req, resp);
        } else if ("/refresh".equals(pathInfo)) {
            refreshToken(req, resp);
        } else if ("/avatar".equals(pathInfo)) {
            uploadAvatar(req, resp);
        } else if ("/role-request".equals(pathInfo)) {
            submitRoleRequest(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if ("/current".equals(pathInfo) || "/profile".equals(pathInfo)) {
            getCurrentUser(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if ("/profile".equals(pathInfo)) {
            updateProfile(req, resp);
        } else if ("/password".equals(pathInfo)) {
            changePassword(req, resp);
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

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest loginRequest;
        try {
            loginRequest = JsonUtil.parseJsonRequest(req, LoginRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }
        
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Username and password are required"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM user WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, loginRequest.getUsername());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                if ("disabled".equals(status)) {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("Account is disabled. Please contact admin."));
                    return;
                }

                String hashedPassword = rs.getString("password");
                if (PasswordUtil.verifyPassword(loginRequest.getPassword(), hashedPassword)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setNickname(rs.getString("nickname"));
                    user.setPhone(rs.getString("phone"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(status);
                    user.setAvatar(rs.getString("avatar"));
                    
                    String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
                    
                    ApiResponse<Object> response = new ApiResponse<>(true, "Login successful", user);
                    response.setData(new LoginResponse(user, token));
                    JsonUtil.sendJsonResponse(resp, response);
                } else {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid username or password"));
                }
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid username or password"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("数据库错误"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisterRequest registerRequest;
        try {
            registerRequest = JsonUtil.parseJsonRequest(req, RegisterRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }
        
        if (registerRequest.getUsername() == null || registerRequest.getPassword() == null || 
            registerRequest.getPhone() == null || registerRequest.getRole() == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("All fields are required"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String checkSql = "SELECT * FROM user WHERE username = ?";
            ps = conn.prepareStatement(checkSql);
            ps.setString(1, registerRequest.getUsername());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Username already exists"));
                return;
            }
            
            String hashedPassword = PasswordUtil.hashPassword(registerRequest.getPassword());
            String insertSql = "INSERT INTO user (username, password, phone, role, nickname) VALUES (?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, registerRequest.getUsername());
            ps.setString(2, hashedPassword);
            ps.setString(3, registerRequest.getPhone());
            ps.setString(4, registerRequest.getRole());
            ps.setString(5, registerRequest.getNickname() != null ? registerRequest.getNickname() : registerRequest.getUsername());
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt(1));
                    user.setUsername(registerRequest.getUsername());
                    user.setNickname(registerRequest.getNickname() != null ? registerRequest.getNickname() : registerRequest.getUsername());
                    user.setPhone(registerRequest.getPhone());
                    user.setRole(registerRequest.getRole());
                    JsonUtil.sendJsonResponse(resp, ApiResponse.success("Registration successful", user));
                }
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Registration failed"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonUtil.sendJsonResponse(resp, ApiResponse.success("Logout successful", null));
    }

    private void getCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
            return;
        }
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM user WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                if ("disabled".equals(status)) {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "Account is disabled"));
                    return;
                }

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setNickname(rs.getString("nickname"));
                user.setPhone(rs.getString("phone"));
                user.setRole(rs.getString("role"));
                user.setAvatar(rs.getString("avatar"));
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(user));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("User not found"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void updateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        System.out.println("Received Authorization header: " + token);
        
        if (token == null) {
            System.out.println("Authorization header is null");
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Authorization header is missing"));
            return;
        }
        
        if (!token.startsWith("Bearer ")) {
            System.out.println("Invalid token format, expected 'Bearer <token>'");
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token format"));
            return;
        }
        
        token = token.substring(7);
        System.out.println("Extracted token: " + token);
        
        if (!JwtUtil.validateToken(token)) {
            System.out.println("Token validation failed: " + token);
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
            return;
        }
        System.out.println("Token validation succeeded: " + token);
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        
        UpdateProfileRequest updateRequest;
        try {
            updateRequest = JsonUtil.parseJsonRequest(req, UpdateProfileRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE user SET nickname = ?, phone = ?, avatar = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, updateRequest.getNickname());
            ps.setString(2, updateRequest.getPhone());
            ps.setString(3, updateRequest.getAvatar());
            ps.setInt(4, userId);
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                // Query updated user info
                String selectSql = "SELECT * FROM user WHERE id = ?";
                ps = conn.prepareStatement(selectSql);
                ps.setInt(1, userId);
                rs = ps.executeQuery();
                
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setNickname(rs.getString("nickname"));
                    user.setPhone(rs.getString("phone"));
                    user.setRole(rs.getString("role"));
                    user.setAvatar(rs.getString("avatar"));
                    
                    // Return updated user info in Chinese
                    JsonUtil.sendJsonResponse(resp, ApiResponse.success("个人资料更新成功", user));
                } else {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("无法获取更新后的用户信息"));
                }
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("更新个人资料失败"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("数据库错误"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
            return;
        }
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        
        ChangePasswordRequest changeRequest;
        try {
            changeRequest = JsonUtil.parseJsonRequest(req, ChangePasswordRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request format"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM user WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (PasswordUtil.verifyPassword(changeRequest.getOldPassword(), hashedPassword)) {
                    String newHashedPassword = PasswordUtil.hashPassword(changeRequest.getNewPassword());
                    String updateSql = "UPDATE user SET password = ? WHERE id = ?";
                    ps = conn.prepareStatement(updateSql);
                    ps.setString(1, newHashedPassword);
                    ps.setInt(2, userId);
                    
                    int result = ps.executeUpdate();
                    
                    if (result > 0) {
                        JsonUtil.sendJsonResponse(resp, ApiResponse.success("密码修改成功", null));
                    } else {
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("修改密码失败"));
                    }
                } else {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("旧密码不正确"));
                }
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("用户不存在"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void uploadAvatar(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Integer userId = null;
        String avatarUrl = null;

        try {
            // Get user ID from token
            String token = req.getHeader("Authorization");
            
            if (token == null || !token.startsWith("Bearer ")) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
                return;
            }
            
            token = token.substring(7);
            
            if (!JwtUtil.validateToken(token)) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
                return;
            }
            
            userId = JwtUtil.getUserIdFromToken(token);
            
            Part filePart = null;
            try {
                filePart = req.getPart("file");
            } catch (Exception e) {
                // If "file" part is not found or other error, try iterating
                for (Part part : req.getParts()) {
                    if (part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                        filePart = part;
                        break;
                    }
                }
            }
            
            if (filePart != null && filePart.getSize() > 0) {
                 if (filePart.getSize() > 1024 * 1024) {
                     JsonUtil.sendJsonResponse(resp, ApiResponse.error("File too large (max 1MB)"));
                     return;
                 }

                String submittedFileName = filePart.getSubmittedFileName();
                String fileExtension = "";
                if (submittedFileName != null && submittedFileName.contains(".")) {
                    fileExtension = submittedFileName.substring(submittedFileName.lastIndexOf(".") + 1).toLowerCase();
                } else {
                    fileExtension = "jpg"; // Default
                }
                
                String newFileName = userId + "_" + System.currentTimeMillis() + "." + fileExtension;

                String uploadDir = UPLOAD_DIR;
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String filePath = uploadDir + File.separator + newFileName;
                filePart.write(filePath);
                
                // Use a special API endpoint for avatar retrieval instead of direct static file access
                avatarUrl = "/api/resources/avatar/" + newFileName;
            }
            
            if (avatarUrl != null) {
                // Update user avatar in database
                Connection conn = null;
                PreparedStatement ps = null;
                try {
                    conn = DBUtil.getConnection();
                    String sql = "UPDATE user SET avatar = ? WHERE id = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, avatarUrl);
                    ps.setInt(2, userId);

                    int result = ps.executeUpdate();
                    if (result > 0) {
                        // Return avatar URL
                        JsonUtil.sendJsonResponse(resp, ApiResponse.success("头像上传成功", avatarUrl));
                    } else {
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("更新头像失败"));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("数据库错误"));
                } finally {
                    DBUtil.close(conn, ps);
                }
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("未上传文件"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    private void submitRoleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
            return;
        }
        token = token.substring(7);
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
            return;
        }
        Integer userId = JwtUtil.getUserIdFromToken(token);

        Map<String, Object> body = JsonUtil.parseJsonMap(req);
        String currentRole = (String) body.get("currentRole");
        String targetRole = (String) body.get("targetRole");
        String reason = (String) body.get("reason");

        if (targetRole == null || reason == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Missing required fields"));
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            // Check if there is a pending request
            String checkSql = "SELECT id FROM role_change_request WHERE user_id = ? AND status = 'pending'";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        JsonUtil.sendJsonResponse(resp, ApiResponse.error("You already have a pending request."));
                        return;
                    }
                }
            }

            String sql = "INSERT INTO role_change_request (user_id, current_role, target_role, reason, status, create_time) VALUES (?, ?, ?, ?, 'pending', NOW())";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, currentRole);
                ps.setString(3, targetRole);
                ps.setString(4, reason);
                ps.executeUpdate();
            }
            
            // Log operation
            String logSql = "INSERT INTO operation_log (user_id, module, operation, details, create_time) VALUES (?, 'RoleRequest', 'Submit', ?, NOW())";
            try (PreparedStatement ps = conn.prepareStatement(logSql)) {
                ps.setInt(1, userId);
                ps.setString(2, "Request to change role to " + targetRole);
                ps.executeUpdate();
            }

            JsonUtil.sendJsonResponse(resp, ApiResponse.success("Request submitted successfully", null));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        }
    }

    private void refreshToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid or expired token"));
            return;
        }
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);
        String role = JwtUtil.getRoleFromToken(token);
        
        if (userId != null && username != null) {
            String newToken = JwtUtil.generateToken(userId, username, role);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success("Token refreshed", newToken));
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token claims"));
        }
    }

    private static class LoginResponse {
        private User user;
        private String token;

        public LoginResponse(User user, String token) {
            this.user = user;
            this.token = token;
        }

        public User getUser() {
            return user;
        }

        public String getToken() {
            return token;
        }
    }
}
