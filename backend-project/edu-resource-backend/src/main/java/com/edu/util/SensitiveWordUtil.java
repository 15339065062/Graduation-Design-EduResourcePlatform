package com.edu.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SensitiveWordUtil {
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "暴力", "色情", "赌博", "笨蛋", "傻瓜", "垃圾", "死", "杀"
    ));

    public static String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (String word : SENSITIVE_WORDS) {
            if (result.contains(word)) {
                String replacement = "";
                for (int i = 0; i < word.length(); i++) {
                    replacement += "*";
                }
                result = result.replace(word, replacement);
            }
        }
        return result;
    }
    
    public static boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
