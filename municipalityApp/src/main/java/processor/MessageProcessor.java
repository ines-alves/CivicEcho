package processor;

import controller.ReportController;
import report.ProtectDocumentLibrary;
import model.Report;
import sockets.SocketUtils;

public class MessageProcessor {
    private final ReportController reportController;
    private final ProtectDocumentLibrary documentLibrary;
    private final SocketUtils socketUtils;

    private static final String MUNICIPALITY_PRIVATE_KEY_PATH = "municipality_private.key";
    private static final String CENTRALIZED_PARTY_PUBLIC_KEY_PATH = "centralized_party_public.key";

    public MessageProcessor() {
        this.reportController = new ReportController();
        this.documentLibrary = new ProtectDocumentLibrary();
        this.socketUtils = new SocketUtils();
    }

    public void testCreateReport() {
        System.out.println("=== MUNICIPALITY TEST DEBUG ===");

        System.out.println("Step 1: Creating test report object...");
        model.Report testReport = new model.Report();
        testReport.setReportId("test_report_001");
        testReport.setCategory("Infrastructure");
        testReport.setDescription("Test pothole on main street");
        testReport.setTimestamp("2024-12-03T15:30:00Z");

        model.Report.Coordinates coords = new model.Report.Coordinates();
        coords.latitude = 38.7169;
        coords.longitude = -9.1395;
        testReport.setCoordinates(coords);

        String location = "Lisbon";

        System.out.println("Report created with ID: " + testReport.getReportId());
        System.out.println("Report JSON: " + testReport.toPrettyJson());

        System.out.println("Step 2: Storing report in database...");
        reportController.createReport(testReport, location);

        System.out.println("Step 3: Reading report back from database...");
        reportController.readReport(testReport.getReportId());

        System.out.println("Step 4: Test completed successfully!");
        System.out.println("=== MUNICIPALITY TEST DEBUG END ===");
    }

    public void processSecureDocument(String base64SecureDocument) {
        try {
            String location = socketUtils.getLocationFromJsonFile(base64SecureDocument);
            String operation = socketUtils.getOperationFromJsonFile(base64SecureDocument);
            Report decryptedReport = decryptSecureDocument(base64SecureDocument);

            if (decryptedReport != null) {
                performCrudOperation(operation, decryptedReport, location);
            }
        } catch (Exception e) {
            System.err.println("Processing failed: " + e.getMessage());
        }
    }

    private void performCrudOperation(String operation, Report report, String location) {
        switch (operation.toLowerCase()) {
            case "create":
                reportController.createReport(report, location);
                System.out.println("Report created: " + report.getReportId());
                break;
            case "read":
                reportController.readReport(report.getReportId());
                break;
            default:
                System.err.println("Unknown operation: " + operation);
        }
    }

    private Report decryptSecureDocument(String base64SecureDocument) {
        try {
            String tempSecureDocFile = "temp_secure_document.json";
            String tempDecryptedFile = "temp_decrypted_report.json";

            writeSecureDocumentToFile(base64SecureDocument, tempSecureDocFile);
            documentLibrary.unprotect(tempSecureDocFile, tempDecryptedFile,
                                    CENTRALIZED_PARTY_PUBLIC_KEY_PATH, MUNICIPALITY_PRIVATE_KEY_PATH);
            Report report = readReportFromFile(tempDecryptedFile);
            cleanupTempFiles(tempSecureDocFile, tempDecryptedFile);

            return report;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeSecureDocumentToFile(String base64SecureDocument, String filename) throws Exception {
        try (java.io.FileWriter writer = new java.io.FileWriter(filename)) {
            writer.write("\"" + base64SecureDocument + "\"");
        }
    }

    private Report readReportFromFile(String filename) throws Exception {
        try (java.io.FileReader reader = new java.io.FileReader(filename)) {
            StringBuilder content = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                content.append((char) character);
            }
            return Report.fromJson(content.toString());
        }
    }

    private void cleanupTempFiles(String... filenames) {
        for (String filename : filenames) {
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
            } catch (Exception ignored) {}
        }
    }

}