package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.sql.*;
import log.AuditFile;

public class DatabaseService {
    private final String dbUrl;
    private static final String DB_USERNAME = "municipality_user";
    private static final String DB_PASSWORD = "municipality_pass";

    public DatabaseService(String municipality) {
        this.dbUrl = "jdbc:postgresql://192.168.2.100:5432/municipality" +
            "?sslmode=require" +
            "&sslcert=../certificates/municipalities/" + municipality + "/" + municipality + ".crt" +
            "&sslkey=../certificates/municipalities/" + municipality + "/" + municipality + ".key" +
            "&sslrootcert=../certificates/database/db.crt" +
            "&ssl=true" +
            "&sslProtocol=TLSv1.3" +
            "&sslCipherSuites=TLS_AES_128_GCM_SHA256";
    }

    public String createReport(JsonObject report) {
        System.out.println("=== CREATE REPORT ===");
        String sql = "INSERT INTO reports (report_id, location, report_data) VALUES (?, ?, ?::jsonb)";

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            String location = report.get("location").getAsString();
            String reportId = report.get("report_id").getAsString();

            stmt.setString(1, reportId);
            stmt.setString(2, location);
            stmt.setString(3, report.toString());

            System.out.println("Sending SQL INSERT...");
            stmt.executeUpdate();
            System.out.println("Report stored in PostgreSQL successfully!");

            return createSuccessResponse("Report created in database: " + reportId, null);

        } catch (SQLException e) {
            System.err.println("PostgreSQL Error: " + e.getMessage());
            return createErrorResponse("Database error creating report: " + e.getMessage());
        }
    }


    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, DB_USERNAME, DB_PASSWORD);
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

    public JsonObject getReports(int numberOfReports, String location) {
        String sql;
        boolean isAllReports = (numberOfReports == -1);

        if (isAllReports) {
            sql = "SELECT report_id, location, report_data, created_at FROM reports WHERE location = ? ORDER BY created_at DESC";
            System.out.println("Fetching ALL reports for location: " + location);
            AuditFile.municipalitySqlQuery("SELECT report_id, location, report_data, created_at FROM reports WHERE location = '" + location + "' ORDER BY created_at DESC");
        } else {
            sql = "SELECT report_id, location, report_data, created_at FROM reports WHERE location = ? ORDER BY created_at DESC LIMIT ?";
            System.out.println("Fetching " + numberOfReports + " most recent reports for location: " + location);
            AuditFile.municipalitySqlQuery("SELECT report_id, location, report_data, created_at FROM reports WHERE location = '" + location + "' ORDER BY created_at DESC LIMIT " + numberOfReports);
        }

        JsonObject response = new JsonObject();
        Gson gson = new Gson();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set location parameter (always first parameter)
            stmt.setString(1, location);

            // Set limit parameter if not fetching all reports
            if (!isAllReports) {
                stmt.setInt(2, numberOfReports);
            }

            ResultSet rs = stmt.executeQuery();

            com.google.gson.JsonArray reportsArray = new com.google.gson.JsonArray();
            int count = 0;

            while (rs.next()) {
                String reportJson = rs.getString("report_data");
                String createdAt = rs.getString("created_at");

                JsonObject report = gson.fromJson(reportJson, JsonObject.class);
                report.addProperty("created_at", createdAt);

                reportsArray.add(report);
                count++;

            }

            System.out.println("Sending " + count + " reports");

            response.addProperty("status", "SUCCESS");
            response.addProperty("count", count);
            response.add("reports", reportsArray);

        } catch (SQLException e) {
            System.err.println("PostgreSQL Error during getReports: " + e.getMessage());
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Database error: " + e.getMessage());
        }

        return response;
    }
}
