package cli;

import controller.ReportController;

import java.util.Scanner;

public class CLIController {

    private final Scanner scanner = new Scanner(System.in);
    private final ReportController reportController = new ReportController();

    public void start() {
        while (true) {
            System.out.println("\n=== REPORT CITIZEN APP ===");
            System.out.println("Choose an operation:");
            System.out.println("1 - Create Report");
            System.out.println("2 - Read Report (empty)");
            System.out.println("3 - Update Report (empty)");
            System.out.println("4 - Delete Report (empty)");
            System.out.println("0 - Exit");

            System.out.print("Option: ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    reportController.createReport(scanner);
                    break;
                case "2":
                case "3":
                case "4":
                    System.out.println("Operation not implemented yet.");
                    break;
                case "0":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
