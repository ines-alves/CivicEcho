package database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;
import java.sql.*;

public class DatabaseServer {
    private static final int PORT = 8080;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/municipality?currentSchema=public";
    private static final String DB_USERNAME = "municipality_user";
    private static final String DB_PASSWORD = "municipality_pass";

    public static void main(String[] args) {
        System.out.println("Database Server starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("192.168.2.4"))) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        System.out.println("=== DATABASE SERVER DEBUG ===");
        System.out.println("Client connected from: " + clientSocket.getInetAddress().getHostAddress());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            System.out.println("Reading request from client...");
            String request = in.readLine();
            System.out.println("Request received: " + request);

            System.out.println("Processing request...");
            String response = processRequest(request);
            System.out.println("Response generated: " + response);

            System.out.println("Sending response to client...");
            out.println(response);
            System.out.println("Response sent successfully!");

        } catch (IOException e) {
            System.err.println("ERROR in handleClient: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Closing client socket...");
                clientSocket.close();
                System.out.println("Client socket closed!");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }

        System.out.println("=== DATABASE SERVER DEBUG END ===");
    }

    private static String processRequest(String request) {
        Gson gson = new Gson();
        try {
            JsonObject requestObj = gson.fromJson(request, JsonObject.class);
            String operation = requestObj.get("operation").getAsString();

            switch (operation.toLowerCase()) {
                case "create":
                    return createReport(requestObj);
                case "read":
                    return readReport(requestObj);
                default:
                    return createErrorResponse("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            return createErrorResponse("Request processing error: " + e.getMessage());
        }
    }

    private static String createReport(JsonObject request) {
        System.out.println("=== CREATE REPORT DEBUG ===");
        String sql = "INSERT INTO reports (report_id, location, report_data) VALUES (?, ?, ?::jsonb)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            JsonObject report = request.getAsJsonObject("report");
            String location = request.get("location").getAsString();
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

    private static String readReport(JsonObject request) {
        System.out.println("=== READ REPORT DEBUG ===");
        String sql = "SELECT report_id, location, report_data, created_at FROM reports WHERE report_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String reportId = request.get("reportId").getAsString();
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


    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    private static String createSuccessResponse(String message, Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");
        response.addProperty("message", message);
        if (data != null) {
            response.add("data", new Gson().toJsonTree(data));
        }
        return new Gson().toJson(response);
    }

    private static String createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", message);
        return new Gson().toJson(response);
    }
}