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

    /**
     * Process a secure document received from CentralizedParty
     * Uses actual utils methods: SocketUtils and ProtectDocumentLibrary
     */
    public String processSecureDocument(String secureDocumentJson) throws Exception {
        // Create temporary files for processing
        String tempDecryptedFile = "temp_decrypted_" + System.currentTimeMillis() + ".json";

        try {
            // Use SocketUtils to get location and operation from the file
            String location = SocketUtils.getLocationFromJsonFile(secureDocumentJson);
            String operation = SocketUtils.getOperationFromJsonFile(secureDocumentJson);

            System.out.println("Location extracted: " + location);
            System.out.println("Operation extracted: " + operation);

            documentLibrary.unprotect(secureDocumentJson, tempDecryptedFile, MUNICIPALITY_PRIVATE_KEY_PATH);

            System.out.println("Document successfully decrypted using utils methods");
            System.out.println("Decrypted content: " + tempDecryptedFile);

            // Read the decrypted JSON file
            Gson gson = new Gson();
            JsonObject decryptedJson;
            try (FileReader reader = new FileReader(tempDecryptedFile)) {
                decryptedJson = gson.fromJson(reader, JsonObject.class);
            }

            // Forward to database
            String result;
            if ("CREATE".equalsIgnoreCase(operation)) {
                result = databaseService.createReport(decryptedJson);
            } else if ("READ".equalsIgnoreCase(operation)) {
                String reportId = decryptedJson.get("report_id").getAsString();
                result = databaseService.readReport(reportId);
            } else {
                result = "Unknown operation: " + operation;
            }

            return "SUCCESS: Document processed for " + location + " with operation " + operation + " - " + result;

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