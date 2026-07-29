package com.gameon.codesphere.servlet;

import com.gameon.codesphere.util.DatabaseConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test servlet to verify the database connection is working.
 * Access at: http://localhost:8080/codesphere/api/test-connection
 *
 * Returns a JSON response indicating success or failure,
 * along with the number of tables found in GameOnDb.
 */
@WebServlet("/api/test-connection")
public class TestConnectionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Count tables in the database
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) AS table_count FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
            );

            int tableCount = 0;
            if (rs.next()) {
                tableCount = rs.getInt("table_count");
            }

            out.print("{\"success\": true, \"message\": \"Connected to GameOnDb successfully\", \"tableCount\": " + tableCount + "}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Connection failed: " + e.getMessage().replace("\"", "'") + "\"}");
        }

        out.flush();
    }
}
