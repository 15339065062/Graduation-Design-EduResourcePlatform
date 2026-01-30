package com.edu.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {
    // Use a fixed secret key instead of generating it dynamically
    // This ensures tokens remain valid across server restarts
    private static final String FIXED_SECRET = "your-secret-key-for-jwt-signing-change-this-in-production";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(FIXED_SECRET.getBytes());
    private static final long EXPIRATION_TIME = 604800000; // 7 days in milliseconds

    public static String generateToken(Integer userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return Integer.parseInt(claims.getSubject());
        }
        return null;
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("username", String.class);
        }
        return null;
    }

    public static String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("role", String.class);
        }
        return null;
    }

    public static boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            System.out.println("Token is null or empty");
            return false;
        }
        try {
            System.out.println("Validating token: " + token);
            Claims claims = parseToken(token);
            if (claims == null) {
                System.out.println("Failed to parse token");
                return false;
            }
            System.out.println("Token parsed successfully, subject: " + claims.getSubject());
            boolean isExpired = claims.getExpiration().before(new Date());
            System.out.println("Token expiration: " + claims.getExpiration());
            System.out.println("Current time: " + new Date());
            System.out.println("Token is expired: " + isExpired);
            return !isExpired;
        } catch (Exception e) {
            System.out.println("Token validation exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
