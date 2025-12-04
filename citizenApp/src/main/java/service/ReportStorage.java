package service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

    public static void deleteFiles(String fileName, String protectedFileName) {
        try {
            Files.delete(Paths.get(fileName));
            Files.delete(Paths.get(protectedFileName));
        } catch (IOException e) {
            System.out.println("Error deleting file: " + e.getMessage());
        }
        System.out.println("Deleted temporary files: " + fileName + ", " + protectedFileName);
    }
}
