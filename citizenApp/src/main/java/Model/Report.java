package model;

import com.google.gson.JsonObject;

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

    public JsonObject toPrettyJson() {
        JsonObject reportJson = new JsonObject();
        reportJson.addProperty("report_id", this.report_id);
        reportJson.addProperty("timestamp", this.timestamp);
        reportJson.addProperty("category", this.category);
        reportJson.addProperty("location", this.location);
        JsonObject coordinatesJson = new JsonObject();
        coordinatesJson.addProperty("latitude", this.coordinates.latitude);
        coordinatesJson.addProperty("longitude", this.coordinates.longitude);
        reportJson.add("coordinates", coordinatesJson);
        reportJson.addProperty("description", this.description);
        return reportJson;
    }
}
