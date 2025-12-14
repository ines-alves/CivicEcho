package processor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import report.ProtectDocumentLibrary;
import service.DatabaseService;
import sockets.SocketUtils;
import java.io.FileReader;

public class MessageProcessor {
    private final ProtectDocumentLibrary documentLibrary;
    private final DatabaseService databaseService;

    // Certificate paths for municipality (Lisbon)
    private static final String MUNICIPALITY_PRIVATE_KEY_PATH = "../certificates/municipalities/lisbon/lisbon.privatekey";

    public MessageProcessor() {
        this.documentLibrary = new ProtectDocumentLibrary();
        this.databaseService = new DatabaseService();
    }

    public String processSecureDocument(String secureDocumentJson) throws Exception {
        // Create temporary files for processing
        String tempDecryptedFile = "temp_decrypted_" + System.currentTimeMillis() + ".json";

        try {
            // Use SocketUtils to get location and operation from the file
            String location = SocketUtils.getLocationFromJsonFile(secureDocumentJson);
            String operation = SocketUtils.getOperationFromJsonFile(secureDocumentJson);

            System.out.println("Location extracted: " + location);
            System.out.println("Operation extracted: " + operation);

            // For READ operations, extract the symmetric key first for later reuse
            String extractedSymmetricKeyPath = null;
            if ("READ".equalsIgnoreCase(operation)) {
                extractedSymmetricKeyPath = "temp_read_key_" + System.currentTimeMillis() + ".key";
                documentLibrary.extractSymmetricKeyToPath(secureDocumentJson, MUNICIPALITY_PRIVATE_KEY_PATH, extractedSymmetricKeyPath);
                System.out.println("Extracted symmetric key to: " + extractedSymmetricKeyPath);
            }

            documentLibrary.unprotect(secureDocumentJson, tempDecryptedFile, MUNICIPALITY_PRIVATE_KEY_PATH);

            System.out.println("Document successfully decrypted using utils methods");
            System.out.println("Decrypted content: " + tempDecryptedFile);

            // Read the decrypted JSON file
            Gson gson = new Gson();
            JsonObject decryptedJson;
            try (FileReader reader = new FileReader(tempDecryptedFile)) {
                decryptedJson = gson.fromJson(reader, JsonObject.class);
            }

            // Process based on operation type
            if ("CREATE".equalsIgnoreCase(operation)) {
                String result = databaseService.createReport(decryptedJson);
                return "SUCCESS: Document processed for " + location + " with operation " + operation + " - " + result;
            } else if ("READ".equalsIgnoreCase(operation)) {
                System.out.println("=== PROCESSING READ REQUEST ===");

                // Extract the raw request from decrypted content (should be "READ|numberOfReports|token")
                String rawRequest = decryptedJson.get("content").getAsString();
                System.out.println("Raw request: " + rawRequest);

                // Parse the request: "READ|numberOfReports|token"
                String[] parts = rawRequest.split("\\|");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid read request format. Expected: READ|numberOfReports|token");
                }

                String numberOfReportsStr = parts[1];
                String token = parts[2];

                System.out.println("Number of reports requested: " + numberOfReportsStr);
                System.out.println("Token: " + token);

                // Parse numberOfReports - support "ALL" or numeric value
                int numberOfReports;
                if ("ALL".equalsIgnoreCase(numberOfReportsStr)) {
                    numberOfReports = -1; // Use -1 to indicate "all reports"
                } else {
                    try {
                        numberOfReports = Integer.parseInt(numberOfReportsStr);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid numberOfReports: " + numberOfReportsStr + ". Must be a number or 'ALL'");
                    }
                }

                // Query database for reports
                JsonObject reportsJson = databaseService.getReports(numberOfReports);

                // Create temporary files for encryption
                String tempReportsFile = "temp_reports_" + System.currentTimeMillis() + ".json";
                String tempEncryptedFile = "temp_encrypted_" + System.currentTimeMillis() + ".json";

                try {
                    // Write reports to temporary file
                    try (java.io.FileWriter writer = new java.io.FileWriter(tempReportsFile)) {
                        gson.toJson(reportsJson, writer);
                    }

                    // Encrypt reports using the extracted symmetric key
                    documentLibrary.ProtectSymmetric(tempReportsFile, tempEncryptedFile, extractedSymmetricKeyPath);

                    System.out.println("Reports encrypted successfully");

                    // Read encrypted file content to return
                    String encryptedContent;
                    try (FileReader reader = new FileReader(tempEncryptedFile)) {
                        encryptedContent = gson.fromJson(reader, String.class);
                    }

                    return encryptedContent;

                } finally {
                    // Clean up temporary files
                    cleanupTempFiles(tempReportsFile, tempEncryptedFile, extractedSymmetricKeyPath);
                }
            } else {
                return "ERROR: Unknown operation: " + operation;
            }

        } finally {
            // Clean up temporary files
            cleanupTempFiles(tempDecryptedFile);
            cleanupTempFiles(secureDocumentJson);
        }
    }

    /**
     * Clean up temporary files
     */
    private void cleanupTempFiles(String... filenames) {
        for (String filename : filenames) {
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
                System.out.println("Cleaned up temporary file: " + filename);
            } catch (Exception e) {
                System.err.println("Failed to cleanup file " + filename + ": " + e.getMessage());
            }
        }
    }
}