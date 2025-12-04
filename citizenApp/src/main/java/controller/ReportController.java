package controller;

import model.Report;
import service.ReportBuilder;
import service.ReportService;

import java.util.Scanner;
import javax.net.ssl.SSLSocket;

public class ReportController {

    public void createReport(Scanner scanner, SSLSocket socket) {
        Report report = new Report();
        ReportBuilder builder = new ReportBuilder(scanner, report);

        boolean result = builder.buildReportInteractively();
        if (result) {
            ReportService.sendReport(socket, report);
            System.out.println("Report created with ID: " + report.getReportId());
        } else {
            System.out.println("Report creation cancelled.");
        }
    }

    public void readReport(Scanner scanner, SSLSocket socket) {
        // Placeholder for future implementation
    }
}
