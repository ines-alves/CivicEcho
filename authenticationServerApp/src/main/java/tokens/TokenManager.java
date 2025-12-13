package tokens;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import report.ProtectDocumentLibrary;

// Singleton class to manage tokens across the auth server
public class TokenManager {

    private static final String AUTHSERVER_PRIV_KEY_PATH = "../certificates/authentication-server/server.privatekey";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();

    final long TEN_MINUTES_MS = 10 * 60 * 1000; // Cooldown period between token requests
    final int TOKENS_PER_REQUEST = 10; // Number of tokens to generate per request

    private static final TokenManager INSTANCE = new TokenManager();

    // citizenId -> citizenInfo(publicKey, location, requestTime)
    private final Map<String, citizenInfo> tokenMap = new HashMap<>();

    private TokenManager() {} // prevent new instances

    public static TokenManager getInstance() {
        return INSTANCE;
    }

    public void addCitizen(String citizenID, String publicKey, String location) {
        if (citizenID == null || publicKey == null) {
            return;
        }
        tokenMap.put(citizenID, new citizenInfo(publicKey, location, null));
    }

    public String getCitizenPublicKey(String citizenID) {
        citizenInfo info = tokenMap.get(citizenID);
        return (info != null) ? info.getPublicKey() : null;
    }

    public String requestTokens(String citizenID, boolean enrollment) throws Exception{

        citizenInfo info = tokenMap.get(citizenID);

        long timestamp = System.currentTimeMillis();
        if (info.getRequestTime() != null) {
            if ((timestamp - info.getRequestTime()) < TEN_MINUTES_MS) {
                return null; // Too soon since last request
            }
        }
        info.setRequestTime(timestamp);
        String location = info.getLocation();
        String tokens = "";

        if (enrollment) { // if we are doing the citizen enrollment
            tokens += ("|" + generateReadToken(location));
        }

        long expirationTimestamp = timestamp + TEN_MINUTES_MS;
        for (int i=0 ; i<TOKENS_PER_REQUEST; i++) {
            tokens += "|" + generateToken(location, expirationTimestamp);
        }
        return tokens;
    }

    private String generateToken(String location, long expirationTimestamp) throws Exception {
        // Generate a 256-bit random number (virtually zero collisions)
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        String randomNumberBase64 = Base64.getEncoder().encodeToString(randomBytes);

        // token = "randomNumber:expirationTimestamp:location"
        String token = randomNumberBase64 + ":" + expirationTimestamp + ":" + location;
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        
        // Sign token
        byte[] signatureBytes = protectDocumentLibrary.sign(tokenBytes, AUTHSERVER_PRIV_KEY_PATH);

        // Encode output
        String tokenBase64 = Base64.getEncoder().encodeToString(tokenBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        // "token-signature"
        return tokenBase64 + "-" + signatureBase64;
    }

    private String generateReadToken(String location) throws Exception {
        // Generate a 256-bit random number (virtually zero collisions)
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        String randomNumberBase64 = Base64.getEncoder().encodeToString(randomBytes);

        // token = "randomNumber:expirationTimestamp:location"
        String token = randomNumberBase64 + ":" + location;
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

