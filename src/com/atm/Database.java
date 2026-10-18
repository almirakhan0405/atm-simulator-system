package com.atm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static final Properties PROPERTIES = new Properties();

    static {
        Path[] candidates = new Path[] {
            Paths.get("config", "atm.properties"),
            Paths.get("..", "config", "atm.properties"),
            Paths.get(System.getProperty("user.dir", "."), "config", "atm.properties")
        };
        boolean loaded = false;
        for (Path p : candidates) {
            if (Files.exists(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    PROPERTIES.load(in);
                    loaded = true;
                    break;
                } catch (IOException ignored) {
                }
            }
        }
        if (!loaded) {
            try (InputStream in = Database.class.getResourceAsStream("/atm.properties")) {
                if (in != null) {
                    PROPERTIES.load(in);
                }
            } catch (Exception ignored) {
            }
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Database() { }

    private static String value(String key, String env, String fallback) {
        String v = System.getenv(env);
        if (v != null && !v.trim().isEmpty()) return v.trim();
        v = PROPERTIES.getProperty(key);
        return v == null || v.trim().isEmpty() ? fallback : v.trim();
    }

    public static Connection getConnection() throws SQLException {
        String host = value("db.host", "ATM_DB_HOST", "localhost");
        String port = value("db.port", "ATM_DB_PORT", "3306");
        String name = value("db.name", "ATM_DB_NAME", "atm_simulator");
        String user = value("db.user", "ATM_DB_USER", "root");
        String password = value("db.password", "ATM_DB_PASSWORD", "2805");
        String sslMode = value("db.sslMode", "ATM_DB_SSLMODE", "PREFERRED");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?sslMode=" + sslMode + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, user, password);
    }
}
