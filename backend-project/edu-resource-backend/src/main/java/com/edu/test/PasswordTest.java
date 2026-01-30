package com.edu.test;

import com.edu.util.PasswordUtil;

public class PasswordTest {
    public static void main(String[] args) {
        String password = "admin123";
        String hashedPassword = PasswordUtil.hashPassword(password);
        
        System.out.println("Original password: " + password);
        System.out.println("Hashed password: " + hashedPassword);
        System.out.println("Hash length: " + hashedPassword.length());
        
        boolean isValid = PasswordUtil.verifyPassword(password, hashedPassword);
        System.out.println("Verification result: " + isValid);
    }
}
