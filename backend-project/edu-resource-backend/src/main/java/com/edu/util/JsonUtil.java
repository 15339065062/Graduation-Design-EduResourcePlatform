package com.edu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static <T> T parseJsonRequest(HttpServletRequest request, Class<T> classOfT) throws IOException {
        StringBuilder json = new StringBuilder();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        return fromJson(json.toString(), classOfT);
    }
    
    public static java.util.Map<String, Object> parseJsonMap(HttpServletRequest request) throws IOException {
        StringBuilder json = new StringBuilder();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        return gson.fromJson(json.toString(), new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType());
    }

    public static void sendJsonResponse(HttpServletResponse response, Object obj) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(toJson(obj));
        writer.flush();
    }
}
