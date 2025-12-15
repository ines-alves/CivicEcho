package service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

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

        public static void saveRead(String numberOfReports, String token) {
        try {
            Path dir = Paths.get(DIRECTORY);
            if (!Files.exists(dir)) Files.createDirectory(dir);

            String filename = DIRECTORY + "readRequest" + ".json";

            String readRequest = "READ|" + numberOfReports + "|" + token;

            String[] parts = token.split("\\-");
            byte[] partsDeocoded = Base64.getDecoder().decode(parts[0]);
            String partsString = new String(partsDeocoded, StandardCharsets.UTF_8);
            String[] tokenParts = partsString.split("\\:");
            String location = tokenParts[1];

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
