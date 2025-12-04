package service;

import model.Report;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDate;

public class ReportStorage {

    private static final String DIRECTORY = "temp_files/";

    public static void saveReport(Report report) {
        try {
            Path dir = Paths.get(DIRECTORY);
            if (!Files.exists(dir)) Files.createDirectory(dir);

            String filename = DIRECTORY + report.getReportId() + ".json";

            FileWriter writer = new FileWriter(filename);
            writer.write(report.toPrettyJson());
            writer.close();

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
