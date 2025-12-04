package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import service.DatabaseService;

public class DatabaseController {
    private final DatabaseService databaseService;

    public DatabaseController() {
        this.databaseService = new DatabaseService();
    }

    public String processRequest(String request) {
        Gson gson = new Gson();
        try {
            JsonObject requestObj = gson.fromJson(request, JsonObject.class);
            String operation = requestObj.get("operation").getAsString();

            switch (operation.toLowerCase()) {
                case "create":
                    return databaseService.createReport(requestObj);
                case "read":
                    return databaseService.readReport(requestObj);
                default:
                    return createErrorResponse("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            return createErrorResponse("Request processing error: " + e.getMessage());
        }
    }

    private String createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", message);
        return new Gson().toJson(response);
    }
}