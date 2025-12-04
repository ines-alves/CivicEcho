package database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class DatabaseServer {
    private static final int PORT = 8080;
    private static final String BIND_IP = "192.168.2.4";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/municipality?currentSchema=public";
    private static final String DB_USERNAME = "municipality_user";
    private static final String DB_PASSWORD = "municipality_pass";
    private static final int THREAD_POOL_SIZE = 10;
    private static ExecutorService threadPool;

    public static void startDatabaseServer(int port) throws IOException {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);

        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Database server listening on " + BIND_IP + ":" + port + "...");

            while (true) {
                try {
                    Socket clientSocket = listener.accept();
                    System.out.println("Municipality connected from: " + clientSocket.getInetAddress().getHostAddress());

                    threadPool.submit(() -> {
                        try {
                            handleMunicipalityClient(clientSocket);
                        } catch (Exception ex) {
                            System.err.println("Error handling municipality client: " + ex.getMessage());
                        }
                    });

                } catch (IOException e) {
                    System.err.println("Error accepting municipality connection: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        // Set SSL properties for database server
        System.setProperty("javax.net.ssl.keyStore", "database.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "database-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");

        System.out.println("Database Server starting on port " + PORT);

        try {
            startDatabaseServer(PORT);
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleMunicipalityClient(Socket clientSocket) throws Exception {
        try (Socket socket = clientSocket) {
            String message = "";

            while (!message.equals("Exit")) {
                try {
                    System.out.println("=== DATABASE SERVER DEBUG ===");
                    System.out.println("Reading request from municipality...");

                    // Read request using buffered reader (similar to original approach but SSL-aware)
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String request = in.readLine();
                    System.out.println("Request received: " + request);

                    if (request == null || request.equals("Exit")) {
                        break;
                    }

                    System.out.println("Processing municipality request...");
                    String response = processRequest(request);
                    System.out.println("Response generated: " + response);

                    // Send response back to municipality
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(response);
                    System.out.println("Response sent to municipality");

                    System.out.println("=== DATABASE SERVER DEBUG END ===");

                } catch (IOException e) {
                    System.err.println("Communication error with municipality: " + e.getMessage());
                    break;
                }
            }
            System.out.println("Municipality disconnected");

        } catch (IOException e) {
            System.err.println("Socket error: " + e.getMessage());
        }
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