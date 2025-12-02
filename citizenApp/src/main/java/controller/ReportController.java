package controller;

import model.Report;
import service.ReportBuilder;
import service.ReportStorage;

import java.util.Scanner;

public class ReportController {

    public void startCreateFlow(Scanner scanner) {
        ReportBuilder builder = new ReportBuilder(scanner);

        System.out.println("\n--- CREATE REPORT ---");
        Report report = builder.buildReportInteractively();

        while (true) {
            System.out.println("\n--- FINAL PREVIEW ---");
            System.out.println(report.toPrettyJson());

            System.out.println("\noptions:");
            System.out.println("s - Save report");
            System.out.println("e - Edit report");
            System.out.println("d - Delete / Cancel");

            System.out.print("Choose: ");
            String op = scanner.nextLine().trim().toLowerCase();

            switch (op) {
                case "s":
                    ReportStorage.saveReport(report);
                    System.out.println("\nReport saved successfully.");
                    return;

                case "e":
                    editReport(scanner, builder, report);
                    break;

                case "d":
                    System.out.println("\nReport discarded.");
                    return;

                default:
                    System.out.println("\nInvalid option.");
            }
        }
    }

    private void editReport(Scanner scanner, ReportBuilder builder, Report report) {
        while (true) {
            System.out.println("\n--- EDIT REPORT ---");
            System.out.println("1 - Edit category");
            System.out.println("2 - Edit location");
            System.out.println("3 - Edit coordinates");
            System.out.println("4 - Edit description");
            System.out.println("0 - Done editing");

            System.out.print("Option: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "1":
                    builder.editCategory(report);
                    break;
                case "2":
                    builder.editLocation(report);
                    break;
                case "3":
                    builder.editCoordinates(report);
                    break;
                case "4":
                    builder.editDescription(report);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
