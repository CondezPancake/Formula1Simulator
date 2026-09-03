package com.formula1.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Configura las conexiones JDBC de la aplicación.
 *
 * <p>Las credenciales pueden cambiarse sin recompilar mediante
 * {@code DB_URL}, {@code DB_USER} y {@code DB_PASSWORD}.</p>
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3307/formula1_simulator"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            + "&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "root123";

    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(
                setting("DB_URL", DEFAULT_URL),
                setting("DB_USER", DEFAULT_USER),
                setting("DB_PASSWORD", DEFAULT_PASSWORD));
    }

    public static boolean isAvailable() {
        try (Connection connection = open()) {
            return connection.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
