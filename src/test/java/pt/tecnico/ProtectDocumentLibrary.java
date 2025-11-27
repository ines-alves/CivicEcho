package pt.tecnico;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;


public class ProtectDocumentLibrary {
    final String CIPHER_ALGO = "AES/GCM/NoPadding";
    final String DIGEST_ALGO = "SHA-256";

    /* ASYMMETRIC KEY OPERATIONS */
   
    private byte[] encryptRSA(byte[] plainBytes, String pubKeyPath, Cipher cipher) throws Exception {
        RSAKeyGenerator rsakg = new RSAKeyGenerator();
        PublicKey pubKey = rsakg.readPublicKey(pubKeyPath);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(plainBytes);
    }
    private byte[] decryptRSA(byte[] cipherBytes, String privKeyPath, Cipher cipher) throws Exception {
        PrivateKey privKey = RSAKeyGenerator.readPrivateKey(privKeyPath);
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(cipherBytes);
    }

    private byte[] digest(byte[] data) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGO);
        messageDigest.update(data);
        return messageDigest.digest();
    }


    public void protect(String inputDocumentName, String outputDocumentName, String KeyPath) throws Exception {
        AESKeyGenerator aeskg = new AESKeyGenerator();
        Key secretKey = aeskg.read(KeyPath);

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
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, params);
        
        cipher.updateAAD(AAD);

        byte[] cipherText = cipher.doFinal(plainBytes);

        JsonObject encryptedObject = new JsonObject();
        encryptedObject.addProperty("ciphered", Base64.getEncoder().encodeToString(cipherText));
        encryptedObject.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        encryptedObject.add("aad", aadObject); // Store AAD as JSON object, not string

        // Encode the entire JSON as base64 string
        String finalJsonString = new Gson().toJson(encryptedObject);
        String base64Encoded = Base64.getEncoder().encodeToString(finalJsonString.getBytes(StandardCharsets.UTF_8));
    
        // Write result to file
        try (FileWriter fileWriter = new FileWriter(outputDocumentName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(base64Encoded, fileWriter);
        }
    }

    public void unprotect(String encryptedDocumentName, String decryptedDocumentName, String KeyPath) throws Exception {
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
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
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
    


}
