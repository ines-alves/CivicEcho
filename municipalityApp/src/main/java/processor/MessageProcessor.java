package processor;

import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import java.io.IOException;

public class MessageProcessor {
    private final ProtectDocumentLibrary documentLibrary;

    // Certificate paths for municipality (Lisbon)
    private static final String DATABASE_ADRRESS = "192.168.2.254";
    private static final String MUNICIPALITY_PRIVATE_KEY_PATH = "../certificates/municipalities/lisbon/lisbon.key";
    private static final String CENTRALIZED_PARTY_PUBLIC_KEY_PATH = "../certificates/centralized-party/cp.crt";

    public MessageProcessor() {
        this.documentLibrary = new ProtectDocumentLibrary();
    }

    /**
     * Process a secure document received from CentralizedParty
     * Uses actual utils methods: SocketUtils and ProtectDocumentLibrary
     */
    public String processSecureDocument(String secureDocumentJson) {
        // Create temporary files for processing
        String tempDecryptedFile = "temp_decrypted_" + System.currentTimeMillis() + ".json";

        try {
            // Use SocketUtils to get location and operation from the file
            String location = SocketUtils.getLocationFromJsonFile(secureDocumentJson);
            String operation = SocketUtils.getOperationFromJsonFile(secureDocumentJson);

            System.out.println("Location extracted: " + location);
            System.out.println("Operation extracted: " + operation);

            // Use ProtectDocumentLibrary.unprotect() to decrypt the document
            documentLibrary.unprotect(secureDocumentJson, tempDecryptedFile,
                                    CENTRALIZED_PARTY_PUBLIC_KEY_PATH, MUNICIPALITY_PRIVATE_KEY_PATH);

            System.out.println("Document successfully decrypted using utils methods");
            System.out.println("Decrypted content: " + tempDecryptedFile);

            // Forward to database
            String result = connectToDatabase(DATABASE_ADRRESS, 8090, tempDecryptedFile);

            return "SUCCESS: Document processed for " + location + " with operation " + operation + " - " + result;

        } finally {
            // Clean up temporary files
            cleanupTempFiles(tempDecryptedFile);
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