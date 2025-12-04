package controller;

import model.Report;
import service.ReportBuilder;
import service.ReportStorage;

import java.util.Scanner;

public class ReportController {

    public void createReport(Scanner scanner) {
        Report report = new Report();
        ReportBuilder builder = new ReportBuilder(scanner, report);

        boolean result = builder.buildReportInteractively();
        if (result) {
            ReportStorage.saveReport(report);
            System.out.println("Report created with ID: " + report.getReportId());
        } else {
            System.out.println("Report creation cancelled.");
        }
    }
}
