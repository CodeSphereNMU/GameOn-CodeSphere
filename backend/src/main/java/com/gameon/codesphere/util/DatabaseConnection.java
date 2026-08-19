package com.gameon.codesphere.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for obtaining JDBC connections to the GameOnDb database.
 * Uses Windows Authentication (integratedSecurity=true) so no username/password is needed.
 *
 * Prerequisites:
 * - SQL Server running locally (localhost or localhost\SQLEXPRESS)
 * - GameOnDb database created and schema applied
 * - mssql-jdbc JAR on the classpath (handled by Maven)
 * - sqljdbc_auth.dll on the system PATH (for Windows Authentication)
 */
public class DatabaseConnection {

    // Connection string for Windows Authentication
    private static final String URL =
        "jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=GameOnDb;"
        + "integratedSecurity=true;"
        + "encrypt=true;"
        + "trustServerCertificate=true;";

    // Private constructor — utility class, do not instantiate
    private DatabaseConnection() {
    }

    /**
     * Returns a new connection to the GameOnDb database.
     * Caller is responsible for closing the connection (use try-with-resources).
     *
     * Example usage:
     * <pre>
     *     try (Connection conn = DatabaseConnection.getConnection()) {
     *         // use conn
     *     }
     * </pre>
     *
     * @return a JDBC Connection to GameOnDb
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
