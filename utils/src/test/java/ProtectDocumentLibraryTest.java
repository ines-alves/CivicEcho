import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.KeyGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import report.ProtectDocumentLibrary;
public class ProtectDocumentLibraryTest {

    @Test 
    public void testGCMEncryptionDecryption() throws Exception {    
        System.out.println("TEST GCM Encryption/Decryption");
        
        // Create test JSON content with timestamp and location fields
        JsonObject testJson = new JsonObject();
        testJson.addProperty("author", "Test Author");
        testJson.addProperty("version", 1);
        testJson.addProperty("title", "Test Document");
        testJson.addProperty("body", "This is a test document body");
        testJson.addProperty("status", "test");
        testJson.addProperty("timestamp", Instant.now().toString());
        testJson.addProperty("location", "Test Location");
        
        // Write test JSON to temporary file as a JSON object
        String inputFile = "test_input.json";
        try (FileWriter writer = new FileWriter(inputFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(testJson, writer);
        }
        
        // Generate AES key for testing
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();
        
        // Write key to temporary file
        String keyFile = "test_key.key";
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(key.getEncoded());
        }

        // Generate citizan RSA key pair
        KeyPairGenerator citizenKeyGen = KeyPairGenerator.getInstance("RSA");
        citizenKeyGen.initialize(2048, SecureRandom.getInstanceStrong());
        KeyPair citizenKeyPair = citizenKeyGen.generateKeyPair();

        // Save RSA private key
        String citizenPrivateKeyFile = "citizen_private_key.key";
        try (FileOutputStream fos = new FileOutputStream(citizenPrivateKeyFile)) {
            fos.write(citizenKeyPair.getPrivate().getEncoded());
        }

        // Save RSA public key
        String citizenPublicKeyFile = "citizen_public_key.key";
        try (FileOutputStream fos = new FileOutputStream(citizenPublicKeyFile)) {
            fos.write(citizenKeyPair.getPublic().getEncoded());
        }

        // Generate centralized RSA key pair
        KeyPairGenerator centralizedKeyGen = KeyPairGenerator.getInstance("RSA");
        centralizedKeyGen.initialize(2048, SecureRandom.getInstanceStrong());
        KeyPair centralizedKeyPair = centralizedKeyGen.generateKeyPair();

        // Save RSA private key
        String centralizedPrivateKeyFile = "centralized_private_key.key";
        try (FileOutputStream fos = new FileOutputStream(centralizedPrivateKeyFile)) {
            fos.write(centralizedKeyPair.getPrivate().getEncoded());
        }

        // Save RSA public key
        String centralizedPublicKeyFile = "centralized_public_key.key";
        try (FileOutputStream fos = new FileOutputStream(centralizedPublicKeyFile)) {
            fos.write(centralizedKeyPair.getPublic().getEncoded());
        }

        // Generate municipality RSA key pair
        KeyPairGenerator municipalityKeyGen = KeyPairGenerator.getInstance("RSA");
        municipalityKeyGen.initialize(2048, SecureRandom.getInstanceStrong());
        KeyPair municipalityKeyPair = municipalityKeyGen.generateKeyPair();

        // Save municipality RSA private key
        String municipalityPrivateKeyFile = "municipality_private_key.key";
        try (FileOutputStream fos = new FileOutputStream(municipalityPrivateKeyFile)) {
            fos.write(municipalityKeyPair.getPrivate().getEncoded());
        }

        // Save municipality RSA public key
        String municipalityPublicKeyFile = "municipality_public_key.key";
        try (FileOutputStream fos = new FileOutputStream(municipalityPublicKeyFile)) {
            fos.write(municipalityKeyPair.getPublic().getEncoded());
        }
        
        // Encrypted output file
        String encryptedFile = "test_encrypted.json";
        // Decrypted output file
        String decryptedFile = "test_decrypted.json";
        
        // Create instance of the library to test
        ProtectDocumentLibrary library = new ProtectDocumentLibrary();
        
        try {
            // Test encryption
            library.protect(inputFile, encryptedFile, keyFile, citizenPrivateKeyFile, municipalityPublicKeyFile, "CREATE");
            System.out.println("Encryption completed successfully");

            // test authenticate and resign
            library.authenticateAndReSign(encryptedFile, encryptedFile, centralizedPrivateKeyFile, citizenPublicKeyFile);

            // Test decryption
            library.unprotect(encryptedFile, decryptedFile, centralizedPublicKeyFile, municipalityPrivateKeyFile);
            System.out.println("Decryption completed successfully");
            
            // Read and verify the decrypted content
            JsonObject decryptedJson;
            try (FileReader reader = new FileReader(decryptedFile)) {
                Gson gson = new Gson();
                decryptedJson = gson.fromJson(reader, JsonObject.class);
            }
            
            // Verify all properties match
            assertEquals(testJson.get("author").getAsString(), decryptedJson.get("author").getAsString());
            assertEquals(testJson.get("version").getAsInt(), decryptedJson.get("version").getAsInt());
            assertEquals(testJson.get("title").getAsString(), decryptedJson.get("title").getAsString());
            assertEquals(testJson.get("body").getAsString(), decryptedJson.get("body").getAsString());
            assertEquals(testJson.get("status").getAsString(), decryptedJson.get("status").getAsString());
            assertEquals(testJson.get("timestamp").getAsString(), decryptedJson.get("timestamp").getAsString());
            assertEquals(testJson.get("location").getAsString(), decryptedJson.get("location").getAsString());
            
            System.out.println("GCM test passed - all content matches!");
            
        } finally {
            // Clean up temporary files
            cleanupFile(inputFile);
            cleanupFile(keyFile);
            cleanupFile(citizenPrivateKeyFile);
            cleanupFile(citizenPublicKeyFile);
            cleanupFile(municipalityPrivateKeyFile);
            cleanupFile(municipalityPublicKeyFile);
            cleanupFile(centralizedPrivateKeyFile);
            cleanupFile(centralizedPublicKeyFile);
            cleanupFile(encryptedFile);
            cleanupFile(decryptedFile);
        }
    }

    

    private void cleanupFile(String filename) {
        try {
            java.io.File file = new java.io.File(filename);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not delete file " + filename);
        }
    }
}