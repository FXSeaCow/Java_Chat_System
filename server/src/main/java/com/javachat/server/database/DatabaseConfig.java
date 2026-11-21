package com.javachat.server.database;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Loads server-side PostgreSQL settings from config.properties or environment variables. */
public record DatabaseConfig(String url, String username, String password) {
    public static DatabaseConfig load() throws IOException {
        String configPath = System.getenv().getOrDefault("JAVA_CHAT_CONFIG", "config.properties");
        Properties properties = new Properties();
        Path path = Path.of(configPath);
        if (Files.exists(path)) {
            try (Reader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(input);
            }
        }

        String url = setting("JAVA_CHAT_DATABASE_URL", "database.url", properties).trim();
        String username = setting("JAVA_CHAT_DATABASE_USERNAME", "database.username", properties).trim();
        String password = setting("JAVA_CHAT_DATABASE_PASSWORD", "database.password", properties);
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("database.url must start with jdbc:postgresql://");
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Database username and password are required");
        }
        return new DatabaseConfig(url, username, password);
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String environmentKey, String propertyKey, Properties properties) {
        String fromEnvironment = System.getenv(environmentKey);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        return properties.getProperty(propertyKey, "");
    }
}
