package com.edu.servlet;

import com.edu.model.Comment;
import com.edu.model.ApiResponse;
import com.edu.util.DBUtil;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;
import com.edu.util.SensitiveWordUtil;
import com.edu.util.HtmlSanitizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/comment", "/api/comment/*"})
public class CommentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        String resourceIdStr = req.getParameter("resourceId");

        if (pathInfo == null || "/".equals(pathInfo)) {
            if (resourceIdStr != null) {
                getCommentsByResource(req, resp);
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Missing resourceId parameter"));
            }
        } else if (pathInfo.matches("/\\d+")) {
            getCommentDetail(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("====== CommentServlet doPost HIT ======");
        System.out.println("Request URI: " + req.getRequestURI());
        System.out.println("Path Info: " + req.getPathInfo());
        
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo)) {
            createComment(req, resp);
        } else {
            System.out.println("Invalid endpoint in doPost: " + pathInfo);
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            updateComment(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            deleteComment(req, resp);
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

    private void getCommentsByResource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String resourceIdStr = req.getParameter("resourceId");
        String pageStr = req.getParameter("page");
        String pageSizeStr = req.getParameter("pageSize");

        if (resourceIdStr == null || resourceIdStr.isEmpty()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource ID is required"));
            return;
        }

        try {
            int resourceId = Integer.parseInt(resourceIdStr);
            int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
            int pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;
            int offset = (page - 1) * pageSize;

            Connection conn = DBUtil.getConnection();
            String countSql = "SELECT COUNT(*) FROM comment WHERE resource_id = ? AND parent_id IS NULL";
            String sql = "SELECT c.*, u.username, u.nickname, u.avatar FROM comment c " +
                         "LEFT JOIN user u ON c.user_id = u.id " +
                         "WHERE c.resource_id = ? AND c.parent_id IS NULL " + 
                         "ORDER BY c.create_time DESC LIMIT ? OFFSET ?";

            PreparedStatement countStmt = conn.prepareStatement(countSql);
            countStmt.setInt(1, resourceId);
            ResultSet countRs = countStmt.executeQuery();
            countRs.next();
            int total = countRs.getInt(1);
            countRs.close();
            countStmt.close();

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, resourceId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> comments = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> comment = new HashMap<>();
                int commentId = rs.getInt("id");
                comment.put("id", commentId);
                comment.put("resourceId", rs.getInt("resource_id"));
                comment.put("userId", rs.getInt("user_id"));
                comment.put("content", rs.getString("content"));
                comment.put("parentId", rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null);
                comment.put("createTime", rs.getTimestamp("create_time").getTime());
                comment.put("username", rs.getString("username"));
                comment.put("nickname", rs.getString("nickname"));
                comment.put("avatar", rs.getString("avatar"));
                
                // Fetch replies for this comment
                List<Map<String, Object>> replies = getRepliesForComment(conn, commentId);
                comment.put("replies", replies);
                
                comments.add(comment);
            }

            rs.close();
            stmt.close();
            conn.close();

            Map<String, Object> data = new HashMap<>();
            data.put("comments", comments);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", (int) Math.ceil((double) total / pageSize));

            JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resource ID format"));
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to get comments: " + e.getMessage()));
        }
    }
    
    private List<Map<String, Object>> getRepliesForComment(Connection conn, int parentId) throws SQLException {
        String sql = "SELECT c.*, u.username, u.nickname, u.avatar, ru.nickname as reply_to_nickname, ru.username as reply_to_username " +
                     "FROM comment c " +
                     "LEFT JOIN user u ON c.user_id = u.id " +
                     "LEFT JOIN user ru ON c.reply_to_user_id = ru.id " +
                     "WHERE c.parent_id = ? ORDER BY c.create_time ASC";
        
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, parentId);
        ResultSet rs = stmt.executeQuery();
        
        List<Map<String, Object>> replies = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> reply = new HashMap<>();
            reply.put("id", rs.getInt("id"));
            reply.put("resourceId", rs.getInt("resource_id"));
            reply.put("userId", rs.getInt("user_id"));
            reply.put("content", rs.getString("content"));
            reply.put("parentId", rs.getInt("parent_id"));
            reply.put("createTime", rs.getTimestamp("create_time").getTime());
            reply.put("username", rs.getString("username"));
            reply.put("nickname", rs.getString("nickname"));
            reply.put("avatar", rs.getString("avatar"));
            
            // Add reply to user info
            int replyToUserId = rs.getInt("reply_to_user_id");
            if (!rs.wasNull()) {
                reply.put("replyToUserId", replyToUserId);
                reply.put("replyToNickname", rs.getString("reply_to_nickname"));
                reply.put("replyToUsername", rs.getString("reply_to_username"));
            }
            
            replies.add(reply);
        }
        
        rs.close();
        stmt.close();
        return replies;
    }

    private void getCommentDetail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        String commentIdStr = pathInfo.substring(1);

        try {
            int commentId = Integer.parseInt(commentIdStr);

            Connection conn = DBUtil.getConnection();
            String sql = "SELECT c.*, u.username, u.nickname, u.avatar FROM comment c " +
                         "LEFT JOIN user u ON c.user_id = u.id WHERE c.id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, commentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> comment = new HashMap<>();
                comment.put("id", rs.getInt("id"));
                comment.put("resourceId", rs.getInt("resource_id"));
                comment.put("userId", rs.getInt("user_id"));
                comment.put("content", rs.getString("content"));
                comment.put("parentId", rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null);
                comment.put("createTime", rs.getTimestamp("create_time").getTime());
                comment.put("username", rs.getString("username"));
                comment.put("nickname", rs.getString("nickname"));
                comment.put("avatar", rs.getString("avatar"));

                rs.close();
                stmt.close();
                conn.close();

                JsonUtil.sendJsonResponse(resp, ApiResponse.success(comment));
            } else {
                rs.close();
                stmt.close();
                conn.close();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid comment ID format"));
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to get comment: " + e.getMessage()));
        }
    }

    private void createComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("Starting createComment...");
        // Get user ID from token
        String token = req.getHeader("Authorization");
        Integer userId = null;
        
        if (token == null || !token.startsWith("Bearer ")) {
            System.out.println("No token provided");
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Unauthorized"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            System.out.println("Invalid token");
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token"));
            return;
        }
        
        userId = JwtUtil.getUserIdFromToken(token);
        System.out.println("User ID from token: " + userId);
        
        Comment comment = JsonUtil.parseJsonRequest(req, Comment.class);

        if (comment == null) {
            System.out.println("Failed to parse JSON body");
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request body"));
            return;
        }
        
        System.out.println("Received comment: " + comment);

        if (comment.getResourceId() == null || userId == null || comment.getContent() == null || comment.getContent().isEmpty()) {
            System.out.println("Missing required fields");
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Missing required fields"));
            return;
        }

        // Filter sensitive words
        String filteredContent = SensitiveWordUtil.filter(comment.getContent());
        comment.setContent(HtmlSanitizer.sanitizePlainText(filteredContent));

        try {
            Connection conn = DBUtil.getConnection();
            Integer rootId = null;
            if (comment.getParentId() != null) {
                rootId = getRootIdForParent(conn, comment.getParentId(), comment.getResourceId());
                if (rootId == null) {
                    conn.close();
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid parentId"));
                    return;
                }
            }

            String sql = "INSERT INTO comment (resource_id, user_id, content, parent_id, reply_to_user_id, root_id, image_count, create_time) VALUES (?, ?, ?, ?, ?, ?, 0, NOW())";

            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, comment.getResourceId());
            stmt.setInt(2, userId);
            stmt.setString(3, comment.getContent());
            if (comment.getParentId() != null) {
                stmt.setInt(4, comment.getParentId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            if (comment.getReplyToUserId() != null) {
                stmt.setInt(5, comment.getReplyToUserId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            if (rootId != null) {
                stmt.setInt(6, rootId);
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int commentId = rs.getInt(1);
                    if (comment.getParentId() == null) {
                        PreparedStatement upRoot = conn.prepareStatement("UPDATE comment SET root_id = ? WHERE id = ?");
                        upRoot.setInt(1, commentId);
                        upRoot.setInt(2, commentId);
                        upRoot.executeUpdate();
                        upRoot.close();
                    }
                    syncCommentCount(conn, comment.getResourceId());
                    
                    // Get the created comment with user info
                    String getCommentSql = "SELECT c.*, u.username, u.nickname, u.avatar FROM comment c " +
                                         "LEFT JOIN user u ON c.user_id = u.id WHERE c.id = ?";
                    PreparedStatement getStmt = conn.prepareStatement(getCommentSql);
                    getStmt.setInt(1, commentId);
                    ResultSet commentRs = getStmt.executeQuery();
                    
                    Map<String, Object> commentData = new HashMap<>();
                    if (commentRs.next()) {
                        commentData.put("id", commentRs.getInt("id"));
                        commentData.put("resourceId", commentRs.getInt("resource_id"));
                        commentData.put("userId", commentRs.getInt("user_id"));
                        commentData.put("content", commentRs.getString("content"));
                        commentData.put("parentId", commentRs.getObject("parent_id"));
                        commentData.put("createTime", commentRs.getTimestamp("create_time").getTime());
                        commentData.put("username", commentRs.getString("username"));
                        commentData.put("nickname", commentRs.getString("nickname"));
                        commentData.put("avatar", commentRs.getString("avatar"));
                    }
                    
                    commentRs.close();
                    getStmt.close();
                    rs.close();
                    stmt.close();
                    conn.close();

                    JsonUtil.sendJsonResponse(resp, ApiResponse.success(commentData));
                } else {
                    rs.close();
                    stmt.close();
                    conn.close();
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to create comment"));
                }
            } else {
                stmt.close();
                conn.close();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to create comment"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to create comment: " + e.getMessage()));
        }
    }

    private void updateComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        int userId = JwtUtil.getUserIdFromToken(token);

        String pathInfo = req.getPathInfo();
        String commentIdStr = pathInfo.substring(1);

        Comment comment = JsonUtil.parseJsonRequest(req, Comment.class);

        if (comment == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid request body"));
            return;
        }

        if (comment.getContent() == null || comment.getContent().isEmpty()) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Content is required"));
            return;
        }

        // Filter sensitive words
        String filteredContent = SensitiveWordUtil.filter(comment.getContent());
        comment.setContent(HtmlSanitizer.sanitizePlainText(filteredContent));

        try {
            int commentId = Integer.parseInt(commentIdStr);

            Connection conn = DBUtil.getConnection();
            
            // Check ownership
            String checkSql = "SELECT user_id FROM comment WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, commentId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                rs.close();
                checkStmt.close();
                conn.close();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
                return;
            }
            
            int ownerId = rs.getInt("user_id");
            rs.close();
            checkStmt.close();
            
            if (ownerId != userId) {
                conn.close();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "You are not allowed to edit this comment"));
                return;
            }

            String sql = "UPDATE comment SET content = ? WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, comment.getContent());
            stmt.setInt(2, commentId);

            int affectedRows = stmt.executeUpdate();
            stmt.close();
            conn.close();

            if (affectedRows > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("message", "Comment updated successfully");

                JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found or update failed"));
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid comment ID format"));
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to update comment: " + e.getMessage()));
        }
    }

    private void deleteComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        int userId = JwtUtil.getUserIdFromToken(token);

        String pathInfo = req.getPathInfo();
        String commentIdStr = pathInfo.substring(1);

        try {
            int commentId = Integer.parseInt(commentIdStr);

            Connection conn = DBUtil.getConnection();
            
            // Check ownership
            String checkSql = "SELECT user_id, resource_id FROM comment WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, commentId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                rs.close();
                checkStmt.close();
                conn.close();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found"));
                return;
            }
            
            int ownerId = rs.getInt("user_id");
            int resourceId = rs.getInt("resource_id");
            rs.close();
            checkStmt.close();
            
            if (ownerId != userId) {
                conn.close();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "You are not allowed to delete this comment"));
                return;
            }

            String sql = "DELETE FROM comment WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, commentId);

            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                syncCommentCount(conn, resourceId);
            }
            
            stmt.close();
            conn.close();

            if (affectedRows > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("message", "Comment deleted successfully");

                JsonUtil.sendJsonResponse(resp, ApiResponse.success(data));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Comment not found or delete failed"));
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid comment ID format"));
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to delete comment: " + e.getMessage()));
        }
    }

    private Integer getRootIdForParent(Connection conn, int parentId, int resourceId) throws SQLException {
        String sql = "SELECT id, root_id, resource_id FROM comment WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parentId);
        ResultSet rs = ps.executeQuery();
        try {
            if (!rs.next()) return null;
            if (rs.getInt("resource_id") != resourceId) return null;
            Object rootObj = rs.getObject("root_id");
            if (rootObj == null) return rs.getInt("id");
            return rs.getInt("root_id");
        } finally {
            rs.close();
            ps.close();
        }
    }

    private void syncCommentCount(Connection conn, int resourceId) throws SQLException {
        int cnt = 0;
        PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM comment WHERE resource_id = ?");
        ps.setInt(1, resourceId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            cnt = rs.getInt(1);
        }
        rs.close();
        ps.close();
        PreparedStatement up = conn.prepareStatement("UPDATE resource SET comment_count = ? WHERE id = ?");
        up.setInt(1, cnt);
        up.setInt(2, resourceId);
        up.executeUpdate();
        up.close();
    }
}
