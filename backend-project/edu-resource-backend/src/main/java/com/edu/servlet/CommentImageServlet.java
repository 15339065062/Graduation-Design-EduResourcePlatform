package com.edu.servlet;

import com.edu.model.ApiResponse;
import com.edu.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet(urlPatterns = {"/api/comment/image/*"})
public class CommentImageServlet extends HttpServlet {
    private static final String UPLOAD_DIR = "C:/Users/macha/Desktop/Graduation-Design-EduResourcePlatform/storage/uploads";
    private static final String COMMENT_DIR = "comments";
    private static final String COMMENT_THUMB_DIR = "comments_thumbs";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        File file = new File(UPLOAD_DIR + File.separator + (thumb ? COMMENT_THUMB_DIR : COMMENT_DIR) + File.separator + p);
        if (!file.exists()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mime = getServletContext().getMimeType(file.getName());
        if (mime == null) mime = "image/jpeg";

        resp.setHeader("Cache-Control", "public, max-age=31536000");
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
}

