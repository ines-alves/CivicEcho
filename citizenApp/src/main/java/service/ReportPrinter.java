package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.FileReader;
import java.io.IOException;

public class ReportPrinter {

    public static void printReportsFromFile(String fileName) {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(fileName)) {

            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray reports = root.getAsJsonArray("reports");

            if (reports == null || reports.size() == 0) {
                System.out.println("No reports found.");
                return;
            }

            System.out.println();
            System.out.println("┌──────────────────────────────────────────────┐");
            System.out.println("│                 READ RESULT                  │");
            System.out.println("└──────────────────────────────────────────────┘");
            System.out.println();

            int number = 1;
            for (JsonElement element : reports) {
                JsonObject report = element.getAsJsonObject();
                printSingleReport(report, number);
                number++;
            }

        } catch (IOException e) {
            System.err.println("Failed to read report file: " + e.getMessage());
        }
    }

    private static void printSingleReport(JsonObject report, int number) {

        JsonObject coords = report.getAsJsonObject("coordinates");

        String reportId   = getString(report, "report_id");
        String timestamp  = getString(report, "created_at");
        String category   = getString(report, "category");
        String location   = getString(report, "location");
        String latitude   = coords != null ? getString(coords, "latitude") : "N/A";
        String longitude  = coords != null ? getString(coords, "longitude") : "N/A";
        String description = getString(report, "description");

        System.out.println("┌──────────────────────────────────────────────┐");
        System.out.printf("│                  REPORT: %-4d                │%n", number);
        System.out.println("├──────────────────────────────────────────────┤");
        System.out.printf("│ Report ID   ▸ %-30s │%n", reportId);
        System.out.printf("│ Timestamp   ▸ %-30s │%n", timestamp);
        System.out.printf("│ Category    ▸ %-30s │%n", category);
        System.out.printf("│ Location    ▸ %-30s │%n", location);
        System.out.printf("│ Latitude    ▸ %-30s │%n", latitude);
        System.out.printf("│ Longitude   ▸ %-30s │%n", longitude);

        printWrappedDescription(description);

        System.out.println("└──────────────────────────────────────────────┘");
        System.out.println();
    }

    private static void printWrappedDescription(String description) {
        int lineLength = 30;
        int start = 0;

        if (description == null) {
            System.out.printf("│ %-11s ▸ %-30s │%n", "Description", "N/A");
            return;
        }

        int end = Math.min(description.length(), lineLength);
        System.out.printf("│ %-11s ▸ %-30s │%n", "Description", description.substring(start, end));
        start = end;

        while (start < description.length()) {
            end = Math.min(start + lineLength, description.length());
            System.out.printf("│ %-11s   %-30s │%n", "", description.substring(start, end));
            start = end;
        }
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : "N/A";
    }
}
