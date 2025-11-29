package pt.tecnico;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;


public class ProtectDocumentLibrary {
    final String SYMMETRIC_CIPHER_ALGO = "AES/GCM/NoPadding";
    final String SIGNATURE_ALGO = "SHA256withRSA";
    final String ASYMMETRIC_CIPHER_ALGO = "RSA/ECB/PKCS1Padding";

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        ProtectDocumentLibrary library = new ProtectDocumentLibrary();
        
        try {
            System.out.println("arg 0!"+ args[0]);
            switch (args[0]) {

                case "protect":
                    if (args.length != 6) {
                        System.err.println("Usage: protect <inputDocument> <outputDocument> <symmetricKey> <citizenPrivateKey> <municipalityPublicKey>");
                        return;
                    }
                    library.protect(args[1], args[2], args[3], args[4], args[5]);
                    System.out.println("Document protected successfully!");
                    break;
                    
                case "authenticate-resign":
                    if (args.length != 5) {
                        System.err.println("Usage: authenticate-resign <encryptedDocument> <reSignedDocument> <centralPrivateKey> <citizenPublicKey>");
                        return;
                    }
                    library.authenticateAndReSign(args[1], args[2], args[3], args[4]);
                    System.out.println("Document authenticated and re-signed successfully!");
                    break;
                    
                case "unprotect":
                    if (args.length != 5) {
                        System.err.println("Usage: unprotect <encryptedDocument> <decryptedDocument> <centralPublicKey> <municipalityPrivateKey>");
                        return;
                    }
                    library.unprotect(args[1], args[2], args[3], args[4]);
                    System.out.println("Document unprotected successfully!");
                    break;
                    
                case "check":
                    if (args.length != 4) {
                        System.err.println("Usage: check <encryptedDocument> <decryptedDocument> <symmetricKey>");
                        return;
                    }
                    library.check(args[1], args[2], args[3]);
                    break;
                    
                default:
                    System.err.println("Unknown command: " + args[0]);
                    printUsage();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: <command> [arguments]");
        System.out.println("Commands:");
        System.out.println("  protect <inputDocument> <outputDocument> <symmetricKey> <citizenPrivateKey> <municipalityPublicKey>");
        System.out.println("  authenticate-resign <encryptedDocument> <reSignedDocument> <centralPrivateKey> <citizenPublicKey>");
        System.out.println("  unprotect <encryptedDocument> <decryptedDocument> <centralPublicKey> <municipalityPrivateKey>");
        System.out.println("  check <encryptedDocument> <decryptedDocument> <symmetricKey>");
    }

    /* ASYMMETRIC KEY OPERATIONS */
   
    private byte[] encryptRSA(byte[] plainBytes, String pubKeyPath) throws Exception {
        RSAKeyGenerator rsakg = new RSAKeyGenerator();
        PublicKey pubKey = rsakg.readPublicKey(pubKeyPath);
        Cipher cipher = Cipher.getInstance(ASYMMETRIC_CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(plainBytes);
    }
    private byte[] decryptRSA(byte[] cipherBytes, String privKeyPath) throws Exception {
        PrivateKey privKey = RSAKeyGenerator.readPrivateKey(privKeyPath);
        Cipher cipher = Cipher.getInstance(ASYMMETRIC_CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(cipherBytes);
    }

    private byte[] sign(byte[] data, String privateKeyPath) throws Exception {
        PrivateKey privKey = RSAKeyGenerator.readPrivateKey(privateKeyPath);
        Signature sig = Signature.getInstance(SIGNATURE_ALGO);
        sig.initSign(privKey);
        sig.update(data);
        return sig.sign();
    }

    private boolean verifySignature(byte[] data, byte[] signature, String publicKeyPath) throws Exception {
        PublicKey pubKey = RSAKeyGenerator.readPublicKey(publicKeyPath);
        Signature sig = Signature.getInstance(SIGNATURE_ALGO);
        sig.initVerify(pubKey);
        sig.update(data);
        boolean verified = sig.verify(signature);
        return verified;
    }

    private JsonObject encryptGCM(String inputDocumentName, String outputDocumentName, String symmetricKeyPath, String municipalityKeyPath) throws Exception {
        AESKeyGenerator aeskg = new AESKeyGenerator();
        Key secretKey = aeskg.read(symmetricKeyPath);

        // Read the JSON file as an object
        JsonObject jsonObject;
        try (FileReader fileReader = new FileReader(inputDocumentName)) {
            Gson gson = new Gson();
            jsonObject = gson.fromJson(fileReader, JsonObject.class);
        }

        // Convert JSON object to string bytes
        byte[] plainBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);

        final int TAG_LENGTH = 128; 
        
        // Create AAD as JSON object with metadata
        JsonObject aadObject = new JsonObject();
        aadObject.addProperty("timestamp", jsonObject.get("timestamp").getAsString());
        aadObject.addProperty("location", jsonObject.get("location").getAsString());
        
        // Convert AAD JSON object to bytes
        byte[] AAD = aadObject.toString().getBytes(StandardCharsets.UTF_8);

        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);

        // Initialize cipher with GCM
        GCMParameterSpec params = new GCMParameterSpec(TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, params);
        
        cipher.updateAAD(AAD);

        byte[] cipherText = cipher.doFinal(plainBytes);

        byte[] cipherKey = encryptRSA(secretKey.getEncoded(), municipalityKeyPath);

        JsonObject encryptedObject = new JsonObject();
        encryptedObject.addProperty("ciphered", Base64.getEncoder().encodeToString(cipherText));
        encryptedObject.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        encryptedObject.add("aad", aadObject); // Store AAD as JSON object, not string
        encryptedObject.addProperty("key", Base64.getEncoder().encodeToString(cipherKey));

        return encryptedObject;
    }

    public void protect(String inputDocumentName, String outputDocumentName, String symmetricKeyPath, String citizenPrivKeyPath, String municipalityKeyPath) throws Exception {
        JsonObject encryptedObject = encryptGCM(inputDocumentName, outputDocumentName,symmetricKeyPath, municipalityKeyPath);
        
        byte[] ciphertextBytes = Base64.getDecoder().decode(encryptedObject.get("ciphered").getAsString());

        byte[] signature = sign(ciphertextBytes, citizenPrivKeyPath);

        encryptedObject.addProperty("signature", Base64.getEncoder().encodeToString(signature));

        System.out.println(encryptedObject.toString());

        // Encode the entire JSON as base64 string
        String finalJsonString = new Gson().toJson(encryptedObject);
        String base64Encoded = Base64.getEncoder().encodeToString(finalJsonString.getBytes(StandardCharsets.UTF_8));
    
        // Write result to file
        try (FileWriter fileWriter = new FileWriter(outputDocumentName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(base64Encoded, fileWriter);
        }
    }

    public void authenticateAndReSign(String encryptedDocumentName, String reSignedDocumentName, String centralPrivateKeyPath, String citizenPublicKeyPath) throws Exception {
        try (FileReader fileReader = new FileReader(encryptedDocumentName)) {
            Gson gson = new Gson();
            
            // Read base64 encoded string from file
            String jsonStringFromFile = gson.fromJson(fileReader, String.class);
            byte[] decoded = Base64.getDecoder().decode(jsonStringFromFile);
            String jsonStringFromByteArray = new String(decoded, StandardCharsets.UTF_8);
            
            // Parse the encrypted JSON structure
            JsonObject cipheredJson = gson.fromJson(jsonStringFromByteArray, JsonObject.class);
            String cipheredTextB64 = cipheredJson.get("ciphered").getAsString();
            byte[] cipheredText = Base64.getDecoder().decode(cipheredTextB64);
            
            String signatureB64 = cipheredJson.get("signature").getAsString();
            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);

            // Verify signature
            boolean isVerified = verifySignature(cipheredText, signatureBytes, citizenPublicKeyPath);
            if (!isVerified) {
                throw new SecurityException("Signature verification failed!");
            }

            // Re-sign the ciphertext
            byte[] newSignature = sign(cipheredText, centralPrivateKeyPath);
            cipheredJson.addProperty("signature", Base64.getEncoder().encodeToString(newSignature));

            // Write re-signed JSON back to file
            String finalJsonString = new Gson().toJson(cipheredJson);
            String base64Encoded = Base64.getEncoder().encodeToString(finalJsonString.getBytes(StandardCharsets.UTF_8));
        
            try (FileWriter fileWriter = new FileWriter(reSignedDocumentName)) {
                Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
                prettyGson.toJson(base64Encoded, fileWriter);
            }
        }
    }

    public void unprotect(String encryptedDocumentName, String decryptedDocumentName, String centralizedPublicKeyPath, String privKeyPath) throws Exception {
        try (FileReader fileReader = new FileReader(encryptedDocumentName)) {
            Gson gson = new Gson();
            
            // Read base64 encoded string from file
            String jsonStringFromFile = gson.fromJson(fileReader, String.class);
            byte[] decoded = Base64.getDecoder().decode(jsonStringFromFile);
            String jsonStringFromByteArray = new String(decoded, StandardCharsets.UTF_8);
            
            // Parse the encrypted JSON structure
            JsonObject cipheredJson = gson.fromJson(jsonStringFromByteArray, JsonObject.class);
            
            String cipheredKeyB64 = cipheredJson.get("key").getAsString();
            byte[] cipheredKey = Base64.getDecoder().decode(cipheredKeyB64);
            byte[] decryptedKeyBytes = decryptRSA(cipheredKey, privKeyPath);
            Key key = new SecretKeySpec(decryptedKeyBytes, 0, 16, "AES");

            String cipheredTextB64 = cipheredJson.get("ciphered").getAsString();
            String ivB64 = cipheredJson.get("iv").getAsString();
            
            // Get AAD as JSON object and convert to bytes
            JsonObject aadObject = cipheredJson.get("aad").getAsJsonObject();
            byte[] AAD = aadObject.toString().getBytes(StandardCharsets.UTF_8);

            // Decode components
            byte[] cipheredText = Base64.getDecoder().decode(cipheredTextB64);
            byte[] iv = Base64.getDecoder().decode(ivB64);

            String signatureB64 = cipheredJson.get("signature").getAsString();
            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
            
            boolean isVerified = verifySignature(cipheredText, signatureBytes, centralizedPublicKeyPath);
                if (!isVerified) {
                    throw new SecurityException("Signature verification failed!");
                }

            // Initialize GCM cipher for decryption
            final int TAG_LENGTH = 128;
            GCMParameterSpec params = new GCMParameterSpec(TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            
            cipher.updateAAD(AAD);

            // Perform decryption
            byte[] decipheredBytes = cipher.doFinal(cipheredText);
            
            // Convert decrypted bytes back to JSON string
            String decryptedJsonString = new String(decipheredBytes, StandardCharsets.UTF_8);
            
            // Write decrypted JSON to output file
            try (FileWriter fileWriter = new FileWriter(decryptedDocumentName)) {
                JsonObject decryptedJson = gson.fromJson(decryptedJsonString, JsonObject.class);
                Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
                prettyGson.toJson(decryptedJson, fileWriter);
            }
        }
    }
    
    public void check(String encryptedDocumentName, String decryptedDocumentName, String KeyPath) throws Exception {
        try (FileReader fileReader = new FileReader(encryptedDocumentName)) {
            AESKeyGenerator aeskg = new AESKeyGenerator();
            Key key = aeskg.read(KeyPath);
            
            Gson gson = new Gson();
            
            // Read base64 encoded string from file
            String jsonStringFromFile = gson.fromJson(fileReader, String.class);
            byte[] decoded = Base64.getDecoder().decode(jsonStringFromFile);
            String jsonStringFromByteArray = new String(decoded, StandardCharsets.UTF_8);
            
            // Parse the encrypted JSON structure
            JsonObject cipheredJson = gson.fromJson(jsonStringFromByteArray, JsonObject.class);
            String cipheredTextB64 = cipheredJson.get("ciphered").getAsString();
            String ivB64 = cipheredJson.get("iv").getAsString();
            
            // Get AAD as JSON object and convert to bytes
            JsonObject aadObject = cipheredJson.get("aad").getAsJsonObject();
            byte[] AAD = aadObject.toString().getBytes(StandardCharsets.UTF_8);

            // Decode components
            byte[] cipheredText = Base64.getDecoder().decode(cipheredTextB64);
            byte[] iv = Base64.getDecoder().decode(ivB64);

            // Initialize GCM cipher for decryption
            final int TAG_LENGTH = 128;
            GCMParameterSpec params = new GCMParameterSpec(TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            
            cipher.updateAAD(AAD);

            try {
                byte[] plaintext = cipher.doFinal(cipheredText);
                System.out.println("Message is authentic");
            } catch (AEADBadTagException ex) {
                System.out.println("Integrity verification FAILED!");
            }
        }
    }

}
