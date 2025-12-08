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
     */
    public String processSecureDocument(String secureDocumentJson) {
        try {
            // Use SocketUtils to get location and operation from the file
            String location = SocketUtils.getLocationFromJsonFile(secureDocumentJson);
            String operation = SocketUtils.getOperationFromJsonFile(secureDocumentJson);
            String DecrytedFile = "decrypted_" + System.currentTimeMillis() + ".json";
            System.out.println("Location extracted: " + location);
            System.out.println("Operation extracted: " + operation);

            // Use ProtectDocumentLibrary.unprotect() to decrypt the document
            documentLibrary.unprotect(secureDocumentJson, DecrytedFile,
                                    CENTRALIZED_PARTY_PUBLIC_KEY_PATH, MUNICIPALITY_PRIVATE_KEY_PATH);

            System.out.println("Document successfully decrypted using utils methods");
            System.out.println("Decrypted content: " + DecrytedFile);

            // Forward to database
            String result = connectToDatabase(DATABASE_ADRRESS, 8080, DecrytedFile);

            return "SUCCESS: Document processed for " + location + " with operation " + operation + " - " + result;

        } catch (Exception e) {
            System.err.println("Error processing secure document: " + e.getMessage());
            return "FAILURE: Error processing document - " + e.getMessage();
        } 
    }
}