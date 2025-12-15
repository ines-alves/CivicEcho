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

            // Extract location from token (token format: base64(data:timestamp:location)-signature)
            String location = ""; 
            if (!token.isEmpty()) {
                try {
                    String[] tokenParts = token.split("-");
                    if (tokenParts.length > 0) {
                        String decoded = new String(java.util.Base64.getDecoder().decode(tokenParts[0]));
                        String[] decodedParts = decoded.split(":");
                        if (decodedParts.length > 2) {
                            location = decodedParts[2];
                        }
                    }
                } catch (Exception e) {
                }
            }

            JsonObject readJson = new JsonObject();
            readJson.addProperty("content", readRequest);

            try (FileWriter fileWriter = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(readJson, fileWriter);
            }

        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }
}
