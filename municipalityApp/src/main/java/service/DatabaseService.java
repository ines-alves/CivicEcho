package database;

import model.Report;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sockets.SocketUtils;
import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class DatabaseService {
    private static final String DB_HOST = "192.168.2.4"; // Database VM IP (VMNET4)
    private static final int DB_PORT = 8080;

    public void createReport(Report report, String location) {
        JsonObject request = new JsonObject();
        request.addProperty("operation", "CREATE");
        request.add("report", new Gson().toJsonTree(report));
        request.addProperty("location", location);

        sendRequestToDatabase(request.toString());
    }

    public void readReport(String reportId) {
        System.out.println("=== DatabaseService READ DEBUG ===");
        System.out.println("Reading report with ID: " + reportId);

        JsonObject request = new JsonObject();
        request.addProperty("operation", "READ");
        request.addProperty("reportId", reportId);

        System.out.println("Read request: " + request.toString());
        sendRequestToDatabase(request.toString());
        System.out.println("=== DatabaseService read DEBUG END ===");
    }


    private void sendRequestToDatabase(String request) {
        System.out.println("=== DatabaseService DEBUG ===");
        System.out.println("Connecting to: " + DB_HOST + ":" + DB_PORT);
        System.out.println("Request to send: " + request);

        try {
            System.out.println("Creating socket connection...");
            java.net.Socket socket = new java.net.Socket(DB_HOST, DB_PORT);
            System.out.println("Socket connected successfully!");

            System.out.println("Getting output stream...");
            java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);

            System.out.println("Getting input stream...");
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));

            System.out.println("Sending request...");
            out.println(request);
            System.out.println("Request sent!");

            System.out.println("Waiting for response...");
            String response = in.readLine();
            System.out.println("Response received: " + response);

            System.out.println("Closing connection...");
            socket.close();
            System.out.println("Connection closed successfully!");

        } catch (Exception e) {
            System.err.println("ERROR in sendRequestToDatabase: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== DatabaseService DEBUG END ===");
    }
}