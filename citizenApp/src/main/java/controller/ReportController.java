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
                System.out.println("No more tokens available."); //sanity check this should not happen
                return;
            }
        } else {
            System.out.println("Report creation cancelled.");
        }
    }

    public void readReport(Scanner scanner, SSLSocket socket) {
        
        String readToken = tokenManager.getReadToken();
        
        while (true) {
            System.out.println();
            System.out.println("┌─────────────────────────────────┐");
            System.out.println("│  How many reports do you want?  │");
            System.out.println("├─────────────────────────────────┤");
            System.out.println("│ 1 ▸ Custom                      │");
            System.out.println("│ 2 ▸ All                         │");
            System.out.println("│ 0 ▸ Go Back                     │");
            System.out.println("└─────────────────────────────────┘");
            System.out.print("Option: ");
            String option = scanner.nextLine().trim();
            
            switch (option) {
                case "1":
                    String number = requestCustomNumber(scanner);
                    ReportService.readReport(socket, number, readToken);
                    return;
                case "2":
                    String reports = "ALL";
                    ReportService.readReport(socket, reports, readToken);
                    return;
                case "0":
                    System.out.println("\nGoing back to main menu.");
                    return;
                default:
                    System.out.println("\nInvalid option.");
            }
        }
    }

    public String requestCustomNumber(Scanner scanner){
        while (true) {
            System.out.println();
            System.out.println("┌─────────────────────────────────┐");
            System.out.println("│   Enter the number of reports   │");
            System.out.println("└─────────────────────────────────┘");
            System.out.print("▸ ");
            String number = scanner.nextLine().trim();

            try {
                int numReports = Integer.parseInt(number);
                if (numReports <= 0) {
                    System.out.println("\nPlease enter a positive integer.");
                } else {
                    return number;
                }
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid input. Please enter a valid integer.");
            }
            
        }

    }
}
