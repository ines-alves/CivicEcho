package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.sql.*;

public class DatabaseService {
    private static final String DB_URL = "jdbc:postgresql://192.168.2.100:5432/municipality" +
        "?sslmode=require" +
        "&sslcert=../certificates/municipalities/lisbon/lisbon.crt" +
        "&sslkey=../certificates/municipalities/lisbon/lisbon.key" +
        "&sslrootcert=../certificates/database/db.crt" +
        "&ssl=true" +
        "&sslProtocol=TLSv1.3" +
        "&sslCipherSuites=TLS_AES_128_GCM_SHA256";
    private static final String DB_USERNAME = "municipality_user";
    private static final String DB_PASSWORD = "municipality_pass";

    public String createReport(JsonObject report) {
        System.out.println("=== CREATE REPORT DEBUG ===");
        String sql = "INSERT INTO reports (report_id, location, report_data) VALUES (?, ?, ?::jsonb)";

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            String location = report.get("location").getAsString();
            String reportId = report.get("report_id").getAsString();

            System.out.println("Report ID: " + reportId);
            System.out.println("Location: " + location);
            System.out.println("Connecting to PostgreSQL database...");

            stmt.setString(1, reportId);
            stmt.setString(2, location);
            stmt.setString(3, report.toString());

            System.out.println("Executing SQL INSERT...");
            stmt.executeUpdate();
            System.out.println("Report stored in PostgreSQL successfully!");

            return createSuccessResponse("Report created in database: " + reportId, null);

        } catch (SQLException e) {
            System.err.println("PostgreSQL Error: " + e.getMessage());
            return createErrorResponse("Database error creating report: " + e.getMessage());
        }
    }

    public String readReport(String reportId) {
        System.out.println("=== READ REPORT DEBUG ===");
        String sql = "SELECT report_id, location, report_data, created_at FROM reports WHERE report_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            System.out.println("Searching for report ID: " + reportId);

            stmt.setString(1, reportId);

            System.out.println("Executing SQL SELECT...");
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String reportJson = rs.getString("report_data");
                String location = rs.getString("location");
                String createdAt = rs.getString("created_at");

                System.out.println("Report found!");
                System.out.println("Location: " + location);
                System.out.println("Created at: " + createdAt);
                System.out.println("Report data: " + reportJson);

                return createSuccessResponse("Report found: " + reportId, reportJson);
            } else {
                System.out.println("Report not found in database");
                return createErrorResponse("Report not found: " + reportId);
            }

        } catch (SQLException e) {
            System.err.println("PostgreSQL Error during read: " + e.getMessage());
            return createErrorResponse("Database error reading report: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    private String createSuccessResponse(String message, Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");
        response.addProperty("message", message);
        if (data != null) {
            response.add("data", new Gson().toJsonTree(data));
        }
        return new Gson().toJson(response);
    }

    private String createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", message);
        return new Gson().toJson(response);
    }
}
