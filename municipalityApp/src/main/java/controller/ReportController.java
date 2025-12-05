package controller;

import model.Report;
import service.DatabaseService;

public class ReportController {
    private final DatabaseService databaseService;

    public ReportController() {
        this.databaseService = new DatabaseService();
    }

    public void createReport(Report report, String location) {
        databaseService.createReport(report, location);
    }

    public void readReport(String reportId) {
        databaseService.readReport(reportId);
    }

}