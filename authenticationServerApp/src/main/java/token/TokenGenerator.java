package token;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import report.ProtectDocumentLibrary;

public class TokenGenerator {

    private static final String AUTHSERVER_PRIV_KEY_PATH = "../certificates/authentication-server/server.privatekey";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();
    
    public static String generateToken() throws Exception {
        long timestamp = System.currentTimeMillis();

        // Generate a 256-bit random number (virtually zero collisions)
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        String randomNumberBase64 = Base64.getEncoder().encodeToString(randomBytes);

        // token = "randomNumber:timestamp"
        String token = randomNumberBase64 + ":" + timestamp;
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        
        // Sign token
        byte[] signatureBytes = protectDocumentLibrary.sign(tokenBytes, AUTHSERVER_PRIV_KEY_PATH);

        // Encode output
        String tokenBase64 = Base64.getEncoder().encodeToString(tokenBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        // "token-signature"
        return tokenBase64 + "-" + signatureBase64;
    }
}
