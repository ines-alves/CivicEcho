package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Report {

    private String report_id;
    private String timestamp;
    private String category;
    private String location;

    private Coordinates coordinates;
    private String description;

    public static class Coordinates {
        public double latitude;
        public double longitude;
    }

    // Getters & Setters
    public String getReportId() { return report_id; }
    public void setReportId(String id) { this.report_id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String ts) { this.timestamp = ts; }

    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }

    public String getLocation() { return location; }
    public void setLocation(String loc) { this.location = loc; }

    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coords) { this.coordinates = coords; }

    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }

    public String toPrettyJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public static Report fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Report.class);
    }
}
