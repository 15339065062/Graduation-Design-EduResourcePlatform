package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.model.PageResult;
import com.edu.model.Resource;
import com.edu.model.User;
import com.edu.util.DBUtil;
import com.edu.util.JsonUtil;
import com.edu.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@WebServlet("/api/resources/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 500,     // 500 MB
    maxRequestSize = 1024 * 1024 * 500   // 500 MB
)
public class ResourceServlet extends HttpServlet {
    private static final String UPLOAD_DIR = "C:/Users/macha/Desktop/Graduation-Design-EduResourcePlatform/storage/uploads";
    
    // Simple in-memory cache for stats
    private static Map<String, Object> statsCache = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION = 60000; // 1 minute

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || "/".equals(pathInfo)) {
            getResources(req, resp);
        } else if (pathInfo.startsWith("/avatar/")) {
             getAvatar(req, resp);
        } else if (pathInfo.matches("/\\d+")) {
            getResourceDetail(req, resp);
        } else if (pathInfo.matches("/\\d+/download")) {
            downloadResource(req, resp);
        } else if (pathInfo.matches("/\\d+/collect")) {
            checkCollection(req, resp);
        } else if ("/my".equals(pathInfo)) {
            getMyResources(req, resp);
        } else if ("/collections".equals(pathInfo)) {
            getCollections(req, resp);
        } else if ("/categories".equals(pathInfo)) {
            getCategories(req, resp);
        } else if ("/stats".equals(pathInfo)) {
            getStats(req, resp);
        } else if (pathInfo.matches("/\\d+/related")) {
            getRelatedResources(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || "/".equals(pathInfo)) {
            uploadResource(req, resp);
        } else if (pathInfo.matches("/\\d+/collect")) {
            collectResource(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if (pathInfo.matches("/\\d+")) {
            updateResource(req, resp);
        } else {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid endpoint"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        String pathInfo = req.getPathInfo();
        
        if (pathInfo.matches("/\\d+")) {
            deleteResource(req, resp);
        } else if (pathInfo.matches("/\\d+/collect")) {
            uncollectResource(req, resp);
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

    private void getAvatar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        String fileName = pathInfo.substring("/avatar/".length());
        
        File file = new File(UPLOAD_DIR + File.separator + fileName);
        if (file.exists()) {
            String mimeType = getServletContext().getMimeType(file.getAbsolutePath());
            if (mimeType == null) {
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (fileName.endsWith(".png")) mimeType = "image/png";
                else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                else mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void getResources(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        String category = req.getParameter("category");
        String fileType = req.getParameter("fileType");
        String sortBy = req.getParameter("sortBy");
        int page = Integer.parseInt(req.getParameter("page") != null ? req.getParameter("page") : "1");
        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        
        int offset = (page - 1) * pageSize;
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM resource r WHERE r.is_public = 1");
            StringBuilder querySql = new StringBuilder(
                "SELECT r.*, u.username, u.nickname, " +
                "(SELECT COUNT(*) FROM comment WHERE resource_id = r.id) as comment_count " +
                "FROM resource r LEFT JOIN user u ON r.uploader_id = u.id WHERE r.is_public = 1");
            List<String> conditions = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            
            if (keyword != null && !keyword.isEmpty()) {
                conditions.add("(r.title LIKE ? OR r.description LIKE ?)");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }
            if (category != null && !category.isEmpty()) {
                conditions.add("r.category = ?");
                params.add(category);
            }
            if (fileType != null && !fileType.isEmpty()) {
                conditions.add("LOWER(r.file_type) = ?");
                params.add(fileType.toLowerCase());
            }
            
            for (String condition : conditions) {
                countSql.append(" AND ").append(condition);
                querySql.append(" AND ").append(condition);
            }
            
            String orderBy = "ORDER BY r.created_at DESC";
            if ("popular".equals(sortBy)) {
                orderBy = "ORDER BY r.collection_count DESC";
            } else if ("downloads".equals(sortBy)) {
                orderBy = "ORDER BY r.download_count DESC";
            } else if ("recommend".equals(sortBy)) {
                // Recommendation logic
                String token = req.getHeader("Authorization");
                Integer userId = null;
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    if (JwtUtil.validateToken(token)) {
                        userId = JwtUtil.getUserIdFromToken(token);
                    }
                }
                
                if (userId != null) {
                    // Fetch user interests (top 3 categories)
                    String interestSql = "SELECT category, COUNT(*) as cnt FROM (" +
                        "SELECT r.category FROM collection c JOIN resource r ON c.resource_id = r.id WHERE c.user_id = ? " +
                        "UNION ALL " +
                        "SELECT r.category FROM comment cm JOIN resource r ON cm.resource_id = r.id WHERE cm.user_id = ? " +
                        "UNION ALL " +
                        "SELECT r.category FROM resource r WHERE r.uploader_id = ? " +
                        ") as interests GROUP BY category ORDER BY cnt DESC LIMIT 3";
                    
                    List<String> topCategories = new ArrayList<>();
                    try (PreparedStatement ips = conn.prepareStatement(interestSql)) {
                        ips.setInt(1, userId);
                        ips.setInt(2, userId);
                        ips.setInt(3, userId);
                        try (ResultSet irs = ips.executeQuery()) {
                            while (irs.next()) {
                                topCategories.add(irs.getString("category"));
                            }
                        }
                    }
                    
                    if (!topCategories.isEmpty()) {
                        StringBuilder caseSql = new StringBuilder("CASE ");
                        for (String cat : topCategories) {
                            caseSql.append("WHEN r.category = '").append(cat).append("' THEN 0 ");
                        }
                        caseSql.append("ELSE 1 END");
                        orderBy = "ORDER BY " + caseSql.toString() + ", r.download_count DESC";
                    } else {
                        // Fallback to popular if no interests found
                        orderBy = "ORDER BY r.download_count DESC";
                    }
                } else {
                    // Fallback to popular for guest
                    orderBy = "ORDER BY r.download_count DESC";
                }
            }
            
            querySql.append(" ").append(orderBy).append(" LIMIT ? OFFSET ?");
            
            // Execute count query
            ps = conn.prepareStatement(countSql.toString());
            int paramIndex = 1;
            for (Object param : params) {
                ps.setObject(paramIndex++, param);
            }
            rs = ps.executeQuery();
            
            int total = 0;
            if (rs.next()) {
                total = rs.getInt(1);
            }
            rs.close();
            ps.close();
            
            // Execute data query
            ps = conn.prepareStatement(querySql.toString());
            paramIndex = 1;
            for (Object param : params) {
                ps.setObject(paramIndex++, param);
            }
            ps.setInt(paramIndex++, pageSize);
            ps.setInt(paramIndex++, offset);
            rs = ps.executeQuery();
            
            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                Resource resource = new Resource();
                resource.setId(rs.getInt("id"));
                resource.setName(rs.getString("title"));
                resource.setDescription(rs.getString("description"));
                resource.setCategory(rs.getString("category"));
                resource.setFileName(rs.getString("file_name"));
                resource.setFilePath(rs.getString("file_path"));
                resource.setFileSize(rs.getLong("file_size"));
                resource.setFileType(rs.getString("file_type"));
                resource.setUploaderId(rs.getInt("uploader_id"));
                resource.setDownloadCount(rs.getInt("download_count"));
                resource.setCollectionCount(rs.getInt("collection_count"));
                resource.setCommentCount(rs.getInt("comment_count"));
                resource.setCreatedAt(rs.getTimestamp("created_at"));
                
                User uploader = new User();
                uploader.setId(rs.getInt("uploader_id"));
                uploader.setUsername(rs.getString("username"));
                uploader.setNickname(rs.getString("nickname"));
                resource.setUploader(uploader);
                
                resources.add(resource);
            }
            
            PageResult<Resource> result = new PageResult<>(resources, total, page, pageSize);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(result));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error: " + e.getMessage()));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void getResourceDetail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String resourceId = req.getPathInfo().substring(1);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT r.*, u.username, u.nickname, u.avatar, (SELECT COUNT(*) FROM comment WHERE resource_id = r.id) as comment_count FROM resource r LEFT JOIN user u ON r.uploader_id = u.id WHERE r.id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(resourceId));
            rs = ps.executeQuery();
            
            if (rs.next()) {
                Resource resource = new Resource();
                resource.setId(rs.getInt("id"));
                resource.setName(rs.getString("title"));
                resource.setDescription(rs.getString("description"));
                resource.setCategory(rs.getString("category"));
                resource.setFileName(rs.getString("file_name"));
                resource.setFilePath(rs.getString("file_path"));
                resource.setFileSize(rs.getLong("file_size"));
                resource.setFileType(rs.getString("file_type"));
                resource.setUploaderId(rs.getInt("uploader_id"));
                resource.setDownloadCount(rs.getInt("download_count"));
                resource.setCollectionCount(rs.getInt("collection_count"));
                resource.setCommentCount(rs.getInt("comment_count"));
                resource.setAllowComments(rs.getBoolean("allow_comments"));
                resource.setIsPublic(rs.getBoolean("is_public"));
                resource.setCreatedAt(rs.getTimestamp("created_at"));
                resource.setUpdatedAt(rs.getTimestamp("updated_at"));
                
                User uploader = new User();
                uploader.setId(rs.getInt("uploader_id"));
                uploader.setUsername(rs.getString("username"));
                uploader.setNickname(rs.getString("nickname"));
                uploader.setAvatar(rs.getString("avatar"));
                resource.setUploader(uploader);
                
                JsonUtil.sendJsonResponse(resp, ApiResponse.success(resource));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource not found"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error: " + e.getMessage()));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void uploadResource(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // Get user ID from token
        String token = req.getHeader("Authorization");
        Integer userId = 1; // Default to user 1 if token is not provided or invalid
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (JwtUtil.validateToken(token)) {
                userId = JwtUtil.getUserIdFromToken(token);
            }
        }
        
        String uploadPath = UPLOAD_DIR;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        try {
            Part filePart = req.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("No file uploaded"));
                return;
            }
            
            String name = req.getParameter("name");
            String description = req.getParameter("description");
            String category = req.getParameter("category");
            boolean allowComments = Boolean.parseBoolean(req.getParameter("allowComments"));
            boolean isPublic = Boolean.parseBoolean(req.getParameter("isPublic"));

            String submittedFileName = filePart.getSubmittedFileName();
            if (submittedFileName == null || submittedFileName.isBlank()) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid file name"));
                return;
            }
            submittedFileName = Paths.get(submittedFileName).getFileName().toString().trim();
            String fileName = System.currentTimeMillis() + "_" + submittedFileName;
            // Use UPLOAD_DIR constant which is an absolute path
            String filePath = UPLOAD_DIR + File.separator + fileName;
            
            filePart.write(filePath);
            
            String fileType = "";
            int dotIndex = submittedFileName.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < submittedFileName.length() - 1) {
                fileType = submittedFileName.substring(dotIndex + 1).toLowerCase();
            }
            long fileSize = filePart.getSize();
            
            Connection conn = null;
            PreparedStatement ps = null;
            
            try {
                conn = DBUtil.getConnection();
                String sql = "INSERT INTO resource (title, description, category, file_name, file_path, file_size, file_type, uploader_id, allow_comments, is_public) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setString(3, category);
                ps.setString(4, fileName);
                // Store only filename in database for portability, or relative path
                ps.setString(5, fileName);
                ps.setLong(6, fileSize);
                ps.setString(7, fileType);
                ps.setInt(8, userId);
                ps.setBoolean(9, allowComments);
                ps.setBoolean(10, isPublic);
                
                int result = ps.executeUpdate();
                
                if (result > 0) {
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        Resource resource = new Resource();
                        resource.setId(rs.getInt(1));
                        resource.setName(name);
                        resource.setDescription(description);
                        resource.setCategory(category);
                        resource.setFileName(fileName);
                        resource.setFilePath(fileName);
                        resource.setFileSize(fileSize);
                        resource.setFileType(fileType);
                        resource.setUploaderId(userId);
                        
                        // 添加上传者信息
                        User uploader = new User();
                        uploader.setId(userId);
                        // 这里可以从数据库查询完整的用户信息，目前使用默认值
                        uploader.setUsername("user" + userId);
                        uploader.setNickname("User " + userId);
                        uploader.setAvatar("https://via.placeholder.com/150");
                        uploader.setRole("teacher");
                        resource.setUploader(uploader);
                        
                        JsonUtil.sendJsonResponse(resp, ApiResponse.success("Resource uploaded successfully", resource));
                    }
                } else {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to upload resource"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
            } finally {
                DBUtil.close(conn, ps);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("File upload error: " + e.getMessage()));
        }
    }

    private void updateResource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        String role = JwtUtil.getRoleFromToken(token);
        
        String resourceId = req.getPathInfo().substring(1);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String checkSql = "SELECT * FROM resource WHERE id = ?";
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, Integer.parseInt(resourceId));
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource not found"));
                return;
            }
            
            Integer uploaderId = rs.getInt("uploader_id");
            
            if (!userId.equals(uploaderId) && !"admin".equals(role)) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "You can only edit your own resources"));
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            
            String requestBody = sb.toString();
            String[] parts = requestBody.split("\"");
            String name = null;
            String description = null;
            String category = null;
            
            for (int i = 0; i < parts.length; i++) {
                if ("name".equals(parts[i])) {
                    name = parts[i + 2];
                } else if ("description".equals(parts[i])) {
                    description = parts[i + 2];
                } else if ("category".equals(parts[i])) {
                    category = parts[i + 2];
                }
            }
            
            String updateSql = "UPDATE resource SET title = ?, description = ?, category = ?, updated_at = NOW() WHERE id = ?";
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, category);
            ps.setInt(4, Integer.parseInt(resourceId));
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.success("Resource updated successfully", null));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to update resource"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void deleteResource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        String role = JwtUtil.getRoleFromToken(token);
        
        String resourceId = req.getPathInfo().substring(1);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String checkSql = "SELECT * FROM resource WHERE id = ?";
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, Integer.parseInt(resourceId));
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource not found"));
                return;
            }
            
            Integer uploaderId = rs.getInt("uploader_id");
            String storedFilePath = rs.getString("file_path");
            String storedFileName = rs.getString("file_name");
            
            if (!userId.equals(uploaderId) && !"admin".equals(role)) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error(403, "You can only delete your own resources"));
                return;
            }
            
            File file = null;
            if (storedFilePath != null && !storedFilePath.trim().isEmpty()) {
                file = resolveStoredFile(storedFilePath);
            }
            if ((file == null || !file.exists()) && storedFileName != null && !storedFileName.trim().isEmpty()
                    && (storedFilePath == null || !storedFileName.equals(storedFilePath))) {
                file = resolveStoredFile(storedFileName);
            }
            
            String deleteSql = "DELETE FROM resource WHERE id = ?";
            ps = conn.prepareStatement(deleteSql);
            ps.setInt(1, Integer.parseInt(resourceId));
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                if (file != null && file.exists()) {
                    file.delete();
                }
                JsonUtil.sendJsonResponse(resp, ApiResponse.success("Resource deleted successfully", null));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to delete resource"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void downloadResource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String resourceId = req.getPathInfo().split("/")[1];
        boolean inline = "true".equals(req.getParameter("inline"));
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM resource WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(resourceId));
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedFilePath = rs.getString("file_path");
                String storedFileName = rs.getString("file_name");
                String title = rs.getString("title");
                String fileType = rs.getString("file_type");
                
                // Only increment download count if not previewing (inline)
                if (!inline) {
                    String updateSql = "UPDATE resource SET download_count = download_count + 1 WHERE id = ?";
                    PreparedStatement updatePs = conn.prepareStatement(updateSql);
                    updatePs.setInt(1, Integer.parseInt(resourceId));
                    updatePs.executeUpdate();
                }
                
                String normalizedFilePath = storedFilePath == null ? null : storedFilePath.trim();
                String normalizedFileName = storedFileName == null ? null : storedFileName.trim();

                File file = null;
                if (normalizedFilePath != null && !normalizedFilePath.isEmpty()) {
                    file = resolveStoredFile(normalizedFilePath);
                }
                if ((file == null || !file.exists()) && normalizedFileName != null && !normalizedFileName.isEmpty()
                        && (normalizedFilePath == null || !normalizedFileName.equals(normalizedFilePath))) {
                    file = resolveStoredFile(normalizedFileName);
                }
                
                if (file.exists()) {
                    // Set Accept-Ranges header
                    resp.setHeader("Accept-Ranges", "bytes");
                    
                    String mimeType = getServletContext().getMimeType(file.getName());
                    if (mimeType == null) {
                        String type = fileType == null ? "" : fileType.toLowerCase();
                        if ("mp4".equals(type)) mimeType = "video/mp4";
                        else if ("webm".equals(type)) mimeType = "video/webm";
                        else if ("ogg".equals(type) || "ogv".equals(type)) mimeType = "video/ogg";
                        else if ("mp3".equals(type)) mimeType = "audio/mpeg";
                        else if ("wav".equals(type)) mimeType = "audio/wav";
                        else if ("pdf".equals(type)) mimeType = "application/pdf";
                        else if ("jpg".equals(type) || "jpeg".equals(type)) mimeType = "image/jpeg";
                        else if ("png".equals(type)) mimeType = "image/png";
                        else if ("gif".equals(type)) mimeType = "image/gif";
                        else mimeType = "application/octet-stream";
                    }
                    
                    resp.setContentType(mimeType);
                    String disposition = inline ? "inline" : "attachment";
                    resp.setHeader("Content-Disposition", disposition + "; filename=\"" + title + "." + fileType + "\"");
                    
                    // Support Range requests for video seeking
                    long length = file.length();
                    long start = 0;
                    long end = length - 1;
                    
                    String range = req.getHeader("Range");
                    boolean hasRange = false;
                    if (range != null && range.startsWith("bytes=")) {
                        String spec = range.substring("bytes=".length()).trim();
                        int dash = spec.indexOf('-');
                        if (dash >= 0) {
                            String startStr = spec.substring(0, dash).trim();
                            String endStr = spec.substring(dash + 1).trim();
                            try {
                                if (startStr.isEmpty()) {
                                    if (!endStr.isEmpty()) {
                                        long suffix = Long.parseLong(endStr);
                                        suffix = Math.max(0, suffix);
                                        start = Math.max(0, length - suffix);
                                    }
                                } else {
                                    start = Long.parseLong(startStr);
                                }

                                if (!endStr.isEmpty()) {
                                    end = Long.parseLong(endStr);
                                }

                                if (end >= length) {
                                    end = length - 1;
                                }

                                if (start < 0 || start >= length || start > end) {
                                    resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                                    resp.setHeader("Content-Range", "bytes */" + length);
                                    return;
                                }

                                hasRange = true;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    if (hasRange) {
                        resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                        resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
                        resp.setContentLengthLong(end - start + 1);
                    } else {
                        resp.setContentLengthLong(length);
                    }
                    
                    try (FileInputStream fis = new FileInputStream(file);
                         OutputStream os = resp.getOutputStream()) {
                        
                        if (hasRange) {
                            fis.skip(start);
                            byte[] buffer = new byte[8192];
                            long bytesToRead = end - start + 1;
                            int bytesRead;
                            while (bytesToRead > 0 && (bytesRead = fis.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
                                os.write(buffer, 0, bytesRead);
                                bytesToRead -= bytesRead;
                            }
                        } else {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                } else {
                    JsonUtil.sendJsonResponse(resp, ApiResponse.error("File not found"));
                }
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource not found"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private File resolveStoredFile(String storedPath) {
        String p = storedPath == null ? "" : storedPath.trim();
        if (p.isEmpty()) {
            return new File(UPLOAD_DIR);
        }

        if (p.startsWith("uploads/")) {
            p = p.substring("uploads/".length());
        } else if (p.startsWith("uploads\\")) {
            p = p.substring("uploads\\".length());
        }

        if (p.contains(":") || p.startsWith("/")) {
            File absolute = new File(p);
            if (absolute.exists()) {
                return absolute;
            }
            p = absolute.getName();
        }

        File file = new File(UPLOAD_DIR + File.separator + p);
        if (!file.exists()) {
            file = new File(getServletContext().getRealPath("") + File.separator + "uploads" + File.separator + p);
        }
        return file;
    }

    private void collectResource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        
        if (token == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Authorization header is missing"));
            return;
        }
        
        if (!token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token format"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid or expired token"));
            return;
        }
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Failed to get user ID from token"));
            return;
        }
        
        String resourceIdStr = req.getPathInfo().split("/")[1];
        int resourceId;
        try {
            resourceId = Integer.parseInt(resourceIdStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resource ID"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String checkSql = "SELECT * FROM collection WHERE user_id = ? AND resource_id = ?";
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource already collected"));
                return;
            }
            
            String insertSql = "INSERT INTO collection (user_id, resource_id) VALUES (?, ?)";
            ps = conn.prepareStatement(insertSql);
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                String updateSql = "UPDATE resource SET collection_count = collection_count + 1 WHERE id = ?";
                ps = conn.prepareStatement(updateSql);
                ps.setInt(1, resourceId);
                ps.executeUpdate();
                
                JsonUtil.sendJsonResponse(resp, ApiResponse.success("Resource collected successfully", null));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to collect resource"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error occurred"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void uncollectResource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        
        if (token == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Authorization header is missing"));
            return;
        }
        
        if (!token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token format"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid or expired token"));
            return;
        }
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Failed to get user ID from token"));
            return;
        }
        
        String resourceIdStr = req.getPathInfo().split("/")[1];
        int resourceId;
        try {
            resourceId = Integer.parseInt(resourceIdStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resource ID"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DBUtil.getConnection();
            String deleteSql = "DELETE FROM collection WHERE user_id = ? AND resource_id = ?";
            ps = conn.prepareStatement(deleteSql);
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                String updateSql = "UPDATE resource SET collection_count = collection_count - 1 WHERE id = ?";
                ps = conn.prepareStatement(updateSql);
                ps.setInt(1, resourceId);
                ps.executeUpdate();
                
                JsonUtil.sendJsonResponse(resp, ApiResponse.success("Resource uncollected successfully", null));
            } else {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Failed to uncollect resource"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error occurred"));
        } finally {
            DBUtil.close(conn, ps);
        }
    }

    private void checkCollection(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getHeader("Authorization");
        
        if (token == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Authorization header is missing"));
            return;
        }
        
        if (!token.startsWith("Bearer ")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid token format"));
            return;
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Invalid or expired token"));
            return;
        }
        
        Integer userId = JwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error(401, "Failed to get user ID from token"));
            return;
        }
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+/collect")) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resource ID format"));
            return;
        }
        
        String resourceIdStr = pathInfo.split("/")[1];
        int resourceId;
        try {
            resourceId = Integer.parseInt(resourceIdStr);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Invalid resource ID"));
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM collection WHERE user_id = ? AND resource_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            rs = ps.executeQuery();
            
            boolean isCollected = rs.next();
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(isCollected));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error occurred"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void getMyResources(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        int page = Integer.parseInt(req.getParameter("page") != null ? req.getParameter("page") : "1");
        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        
        int offset = (page - 1) * pageSize;
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String countSql = "SELECT COUNT(*) FROM resource WHERE uploader_id = ?";
            ps = conn.prepareStatement(countSql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            
            int total = 0;
            if (rs.next()) {
                total = rs.getInt(1);
            }
            rs.close();
            ps.close();
            
            String querySql = "SELECT r.*, " +
                              "(SELECT COUNT(*) FROM comment WHERE resource_id = r.id) as comment_count " +
                              "FROM resource r WHERE uploader_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
            ps = conn.prepareStatement(querySql);
            ps.setInt(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, offset);
            rs = ps.executeQuery();
            
            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                Resource resource = new Resource();
                resource.setId(rs.getInt("id"));
                resource.setName(rs.getString("title"));
                resource.setDescription(rs.getString("description"));
                resource.setCategory(rs.getString("category"));
                resource.setFileName(rs.getString("file_name"));
                resource.setFileSize(rs.getLong("file_size"));
                resource.setFileType(rs.getString("file_type"));
                resource.setDownloadCount(rs.getInt("download_count"));
                resource.setCollectionCount(rs.getInt("collection_count"));
                resource.setCommentCount(rs.getInt("comment_count"));
                resource.setCreatedAt(rs.getTimestamp("created_at"));
                resources.add(resource);
            }
            
            PageResult<Resource> result = new PageResult<>(resources, total, page, pageSize);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(result));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void getCollections(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        int page = Integer.parseInt(req.getParameter("page") != null ? req.getParameter("page") : "1");
        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        
        int offset = (page - 1) * pageSize;
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String countSql = "SELECT COUNT(*) FROM collection WHERE user_id = ?";
            ps = conn.prepareStatement(countSql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            
            int total = 0;
            if (rs.next()) {
                total = rs.getInt(1);
            }
            
            String querySql = "SELECT r.*, u.username, u.nickname, (SELECT COUNT(*) FROM comment WHERE resource_id = r.id) as comment_count FROM collection c " +
                    "JOIN resource r ON c.resource_id = r.id " +
                    "LEFT JOIN user u ON r.uploader_id = u.id " +
                    "WHERE c.user_id = ? ORDER BY c.created_at DESC LIMIT ? OFFSET ?";
            ps = conn.prepareStatement(querySql);
            ps.setInt(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, offset);
            rs = ps.executeQuery();
            
            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                Resource resource = new Resource();
                resource.setId(rs.getInt("id"));
                resource.setName(rs.getString("title"));
                resource.setDescription(rs.getString("description"));
                resource.setCategory(rs.getString("category"));
                resource.setFileName(rs.getString("file_name"));
                resource.setFileSize(rs.getLong("file_size"));
                resource.setFileType(rs.getString("file_type"));
                resource.setDownloadCount(rs.getInt("download_count"));
                resource.setCollectionCount(rs.getInt("collection_count"));
                resource.setCommentCount(rs.getInt("comment_count"));
                resource.setCreatedAt(rs.getTimestamp("created_at"));
                
                User uploader = new User();
                uploader.setId(rs.getInt("uploader_id"));
                uploader.setUsername(rs.getString("username"));
                uploader.setNickname(rs.getString("nickname"));
                resource.setUploader(uploader);
                
                resources.add(resource);
            }
            
            PageResult<Resource> result = new PageResult<>(resources, total, page, pageSize);
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(result));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void getCategories(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT category FROM resource WHERE is_public = 1 ORDER BY category";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            List<String> recommended = Arrays.asList(
                "语文", "数学", "英语", "物理", "化学", "生物", "地理", "历史", "政治",
                "信息技术", "编程入门", "数据结构与算法", "软件工程", "操作系统", "计算机网络", "数据库",
                "前端开发", "后端开发", "移动开发", "人工智能(AI)", "机器学习", "深度学习", "数据科学", "大数据",
                "网络安全", "云计算", "DevOps", "物联网(IoT)", "机器人",
                "产品设计", "UI/UX 设计", "数字媒体", "AIGC", "区块链", "AR/VR/XR", "量子计算",
                "经济学", "管理学", "心理学", "教育学", "医学基础", "法律基础",
                "考研", "公考", "职业技能", "竞赛/科研"
            );

            LinkedHashSet<String> merged = new LinkedHashSet<>();
            for (String c : recommended) {
                if (c != null && !c.trim().isEmpty()) {
                    merged.add(c.trim());
                }
            }

            List<String> extras = new ArrayList<>();
            while (rs.next()) {
                String category = rs.getString("category");
                if (category == null) continue;
                String normalized = category.trim();
                if (normalized.isEmpty()) continue;
                if (!merged.contains(normalized)) {
                    extras.add(normalized);
                }
            }

            Collections.sort(extras);
            merged.addAll(extras);

            JsonUtil.sendJsonResponse(resp, ApiResponse.success(new ArrayList<>(merged)));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void getStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentTime = System.currentTimeMillis();
        
        // Return cached stats if valid
        if (statsCache != null && (currentTime - lastCacheTime) < CACHE_DURATION) {
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(statsCache));
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String userCountSql = "SELECT COUNT(*) FROM user";
            ps = conn.prepareStatement(userCountSql);
            rs = ps.executeQuery();
            int userCount = 0;
            if (rs.next()) {
                userCount = rs.getInt(1);
            }
            
            String resourceCountSql = "SELECT COUNT(*) FROM resource WHERE is_public = 1";
            ps = conn.prepareStatement(resourceCountSql);
            rs = ps.executeQuery();
            int resourceCount = 0;
            if (rs.next()) {
                resourceCount = rs.getInt(1);
            }
            
            String downloadCountSql = "SELECT SUM(download_count) FROM resource";
            ps = conn.prepareStatement(downloadCountSql);
            rs = ps.executeQuery();
            int downloadCount = 0;
            if (rs.next()) {
                downloadCount = rs.getInt(1);
            }
            
            String collectionCountSql = "SELECT SUM(collection_count) FROM resource";
            ps = conn.prepareStatement(collectionCountSql);
            rs = ps.executeQuery();
            int collectionCount = 0;
            if (rs.next()) {
                collectionCount = rs.getInt(1);
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("userCount", userCount);
            stats.put("resourceCount", resourceCount);
            stats.put("downloadCount", downloadCount);
            stats.put("collectionCount", collectionCount);
            
            // Update cache
            statsCache = stats;
            lastCacheTime = currentTime;
            
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(stats));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    private void getRelatedResources(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String resourceId = req.getPathInfo().split("/")[1];
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            
            String categorySql = "SELECT category FROM resource WHERE id = ?";
            ps = conn.prepareStatement(categorySql);
            ps.setInt(1, Integer.parseInt(resourceId));
            rs = ps.executeQuery();
            
            String category = null;
            if (rs.next()) {
                category = rs.getString("category");
            }
            
            if (category == null) {
                JsonUtil.sendJsonResponse(resp, ApiResponse.error("Resource not found"));
                return;
            }
            
            String querySql = "SELECT r.*, u.username, u.nickname, (SELECT COUNT(*) FROM comment WHERE resource_id = r.id) as comment_count FROM resource r " +
                    "LEFT JOIN user u ON r.uploader_id = u.id " +
                    "WHERE r.category = ? AND r.id != ? AND r.is_public = 1 " +
                    "ORDER BY r.download_count DESC LIMIT 5";
            ps = conn.prepareStatement(querySql);
            ps.setString(1, category);
            ps.setInt(2, Integer.parseInt(resourceId));
            rs = ps.executeQuery();
            
            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                Resource resource = new Resource();
                resource.setId(rs.getInt("id"));
                resource.setName(rs.getString("title"));
                resource.setDescription(rs.getString("description"));
                resource.setCategory(rs.getString("category"));
                resource.setFileName(rs.getString("file_name"));
                resource.setFileSize(rs.getLong("file_size"));
                resource.setFileType(rs.getString("file_type"));
                resource.setDownloadCount(rs.getInt("download_count"));
                resource.setCollectionCount(rs.getInt("collection_count"));
                resource.setCommentCount(rs.getInt("comment_count"));
                resource.setCreatedAt(rs.getTimestamp("created_at"));
                
                User uploader = new User();
                uploader.setId(rs.getInt("uploader_id"));
                uploader.setUsername(rs.getString("username"));
                uploader.setNickname(rs.getString("nickname"));
                resource.setUploader(uploader);
                
                resources.add(resource);
            }
            
            JsonUtil.sendJsonResponse(resp, ApiResponse.success(resources));
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(resp, ApiResponse.error("Database error"));
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }
}
