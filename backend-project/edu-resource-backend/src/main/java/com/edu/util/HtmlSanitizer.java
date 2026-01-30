package com.edu.util;

public class HtmlSanitizer {
    public static String sanitizePlainText(String input) {
        if (input == null) return null;
        String s = input;
        s = s.replace("\u0000", "");
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        if (s.length() > 2000) {
            s = s.substring(0, 2000);
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> {
                    if (c >= 0x00 && c < 0x20 && c != '\n' && c != '\t') {
                        continue;
                    }
                    out.append(c);
                }
            }
        }
        return out.toString();
    }
}

