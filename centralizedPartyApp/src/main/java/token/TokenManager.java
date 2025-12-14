package token;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class TokenManager {
    private final Map<String, Long> writeTokens; // token -> expiration timestamp

    public TokenManager() {
        this.writeTokens = new ConcurrentHashMap<>();
    }

    public void storeWriteToken(String token) {
        String[] parts = token.split("\\-");
        byte[] partsDeocoded = Base64.getDecoder().decode(parts[0]);
        String partsString = new String(partsDeocoded, StandardCharsets.UTF_8);
        String[] tokenParts = partsString.split("\\:");
        long expirationTimestamp = Long.parseLong(tokenParts[1]);

        writeTokens.put(token, expirationTimestamp);
        System.out.println("Stored WRITE token: " + token + " (expires at: " + Instant.ofEpochMilli(expirationTimestamp) + ")");
    }

    public boolean verifyUsedToken(String token) {
        // Check WRITE tokens
        if (writeTokens.containsKey(token)) {
            return true;
        }
        return false;
    }

    public int cleanupExpiredTokens() {
        long currentTimestamp = System.currentTimeMillis();
        int removedCount = 0;

        // Create a copy of keys to avoid ConcurrentModificationException
        for (Map.Entry<String, Long> entry : writeTokens.entrySet()) {
            if (currentTimestamp > entry.getValue()) {
                writeTokens.remove(entry.getKey());
                removedCount++;
                System.out.println("Cleanup: Removed expired WRITE token: " + entry.getKey());
            }
        }

        return removedCount;
    }

}
