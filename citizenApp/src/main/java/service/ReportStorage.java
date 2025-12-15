package service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import model.Report;

public class ReportStorage {

    private static final String DIRECTORY = "temp_files/";

    public static void saveReport(Report report) {
        try {
            Path dir = Paths.get(DIRECTORY);
            if (!Files.exists(dir)) Files.createDirectory(dir);

            String filename = DIRECTORY + report.getReportId() + ".json";

            try (FileWriter fileWriter = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(report.toPrettyJson(), fileWriter);
            }

        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    public static void deleteFile(String fileName) {
        try {
            Files.delete(Paths.get(fileName));
        } catch (IOException e) {
            System.out.println("Error deleting file: " + e.getMessage());
        }
    }

        public static void saveRead(String readRequest) {
        try {
            Path dir = Paths.get(DIRECTORY);
            if (!Files.exists(dir)) Files.createDirectory(dir);

            String filename = DIRECTORY + "readRequest" + ".json";

            // Token format contains location, but for simplicity, parse from token
            String[] parts = readRequest.split("\\|");
            String token = parts.length > 2 ? parts[2] : "";

            // Extract location from token (READ token format: base64(randomNumber:location)-signature)
            String location = ""; // Default location
            if (!token.isEmpty()) {
                try {
                    String[] tokenParts = token.split("-");
                    if (tokenParts.length > 0) {
                        String decoded = new String(java.util.Base64.getDecoder().decode(tokenParts[0]));
                        String[] decodedParts = decoded.split(":");
                        // READ token has 2 parts: randomNumber:location
                        if (decodedParts.length >= 2) {
                            location = decodedParts[1];
                        }
                    }
                } catch (Exception e) {
                }
            }

            JsonObject readJson = new JsonObject();
            readJson.addProperty("content", readRequest);
            readJson.addProperty("timestamp", java.time.Instant.now().toString());
            readJson.addProperty("location", location);

            try (FileWriter fileWriter = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(readJson, fileWriter);
            }

        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }
}
