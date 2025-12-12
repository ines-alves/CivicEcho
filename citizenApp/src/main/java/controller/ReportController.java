package controller;

import model.Report;
import model.TokenManager;
import service.ReportBuilder;
import service.ReportService;

import java.util.Scanner;
import javax.net.ssl.SSLSocket;

public class ReportController {

    private final TokenManager tokenManager = TokenManager.getInstance();

    public void createReport(Scanner scanner, SSLSocket socket) {
        Report report = new Report();
        ReportBuilder builder = new ReportBuilder(scanner, report);

        boolean result = builder.buildReportInteractively();
        if (result) {
            String token = tokenManager.getToken();
            if (token != null) {
                ReportService.sendReport(socket, report, token);
                System.out.println("Report created with ID: " + report.getReportId());
            } else {
                System.out.println("No more tokens available."); //FIXME handle token renewal
                return;
            }
        } else {
            System.out.println("Report creation cancelled.");
        }
    }

    public void readReport(Scanner scanner, SSLSocket socket) {
        // Placeholder for future implementation
    }
}
