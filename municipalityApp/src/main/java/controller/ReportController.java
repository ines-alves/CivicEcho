package controller;

import model.Report;

public class ReportController {

    public void createReport(Report report, String location) {
        System.out.println("Report created: " + report.getReportId() + " for " + location);
    }

    public void readReport(String reportId) {
        System.out.println("Report read: " + reportId);
    }

    public void updateReport(Report report, String location) {
        System.out.println("Report updated: " + report.getReportId());
    }

    public void deleteReport(String reportId) {
        System.out.println("Report deleted: " + reportId);
    }
}