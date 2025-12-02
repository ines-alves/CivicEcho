package service;

import model.Report;
import model.Report.Coordinates;
import util.IdGenerator;

import java.time.Instant;
import java.util.Scanner;

public class ReportBuilder {

    private final Scanner scanner;

    public ReportBuilder(Scanner scanner) {
        this.scanner = scanner;
    }

    public Report buildReportInteractively() {
        Report report = new Report();

        report.setReportId(IdGenerator.generateId("echo_"));
        report.setTimestamp(Instant.now().toString());
        report.setCoordinates(new Coordinates());

        editCategory(report);
        editLocation(report);
        editCoordinates(report);
        editDescription(report);

        return report;
    }

    // ---------- EDITABLE FIELDS -----------

    public void editCategory(Report r) {
        System.out.print("Category: ");
        r.setCategory(scanner.nextLine().trim());
    }

    public void editLocation(Report r) {
        System.out.print("Location: ");
        r.setLocation(scanner.nextLine().trim());
    }

    public void editCoordinates(Report r) {
        Coordinates c = r.getCoordinates();
        if (c == null) c = new Coordinates();

        System.out.print("Latitude: ");
        c.latitude = readDouble();

        System.out.print("Longitude: ");
        c.longitude = readDouble();

        r.setCoordinates(c);
    }

    public void editDescription(Report r) {
        System.out.print("Description: ");
        r.setDescription(scanner.nextLine().trim());
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
