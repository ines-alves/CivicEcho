package service;

import model.Report;
import model.Report.Coordinates;
import util.IdGenerator;

import java.time.Instant;
import java.util.Scanner;

public class ReportBuilder {

    private final Scanner scanner;
    private final Report report;

    public ReportBuilder(Scanner scanner, Report report) {
        this.scanner = scanner;
        this.report = report;
    }

    public boolean buildReportInteractively() { // returns true if the report was created successfully
        System.out.println("\n--- CREATE REPORT ---");

        report.setReportId(IdGenerator.generateId("echo_"));
        report.setTimestamp(Instant.now().toString());
        report.setCoordinates(new Coordinates());

        editCategory();
        editLocation();
        editCoordinates();
        editDescription();

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
                    System.out.println("\nReport saved successfully.");
                    return true;

                case "e":
                    editReport();
                    break;

                case "d":
                    System.out.println("\nReport discarded.");
                    return false;

                default:
                    System.out.println("\nInvalid option.");
            }
        }
    }

    private void editReport() {
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
                    editCategory();
                    break;
                case "2":
                    editLocation();
                    break;
                case "3":
                    editCoordinates();
                    break;
                case "4":
                    editDescription();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    // ---------- edit AUX -----------

    public void editCategory() {
        System.out.print("Category: ");
        report.setCategory(scanner.nextLine().trim());
    }

    public void editLocation() {
        System.out.print("Location: ");
        report.setLocation(scanner.nextLine().trim());
    }

    public void editCoordinates() {
        Coordinates c = report.getCoordinates();
        if (c == null) c = new Coordinates();

        System.out.print("Latitude: ");
        c.latitude = readDouble();

        System.out.print("Longitude: ");
        c.longitude = readDouble();

        report.setCoordinates(c);
    }

    public void editDescription() {
        System.out.print("Description: ");
        report.setDescription(scanner.nextLine().trim());
    }

    private double readDouble() {
        while (true) {
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.print("Invalid number, try again: ");
            }
        }
    }
}
