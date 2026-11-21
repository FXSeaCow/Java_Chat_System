package com.javachat.server.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Checks that the database is reachable and the chat schema has been installed. */
public final class DatabaseCheck {
    private DatabaseCheck() {
    }

    public static void main(String[] args) throws Exception {
        DatabaseConfig config = DatabaseConfig.load();
        try (Connection connection = config.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM public.users")) {
            result.next();
            System.out.println("Database connection OK; users table rows: " + result.getLong(1));
        }
    }
}
