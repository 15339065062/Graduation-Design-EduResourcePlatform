package com.edu.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static String url;
    private static String username;
    private static String password;
    private static String driver;

    static {
        try {
            Properties props = new Properties();
            InputStream is = DBUtil.class.getClassLoader().getResourceAsStream("database.properties");
            if (is != null) {
                props.load(is);
                driver = props.getProperty("jdbc.driver");
                url = props.getProperty("jdbc.url");
                username = props.getProperty("jdbc.username");
                password = props.getProperty("jdbc.password");
                Class.forName(driver);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close(Connection conn, PreparedStatement ps) {
        close(conn, ps, null);
    }
}
