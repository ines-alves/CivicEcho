package token;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TokenManager {
    private final Map<String, Long> writeTokens; // token -> expiration timestamp
    private final Map<String, Boolean> readTokens; // token -> always valid (never expires)

    public TokenManager() {
        this.writeTokens = new ConcurrentHashMap<>();
        this.readTokens = new ConcurrentHashMap<>();
    }

    public void storeWriteToken(String token, long expirationTimestamp) {
        writeTokens.put(token, expirationTimestamp);
        System.out.println("Stored WRITE token: " + token + " (expires at: " + Instant.ofEpochMilli(expirationTimestamp) + ")");
    }

    public void storeReadToken(String token) {
        readTokens.put(token, true);
        System.out.println("Stored READ token: " + token + " (never expires)");
    }

    public boolean verifyToken(String token) {
        // Check READ tokens first (they never expire)
        if (readTokens.containsKey(token)) {
            System.out.println("Token verified: " + token + " (READ token)");
            return true;
        }

        // Check WRITE tokens
        if (writeTokens.containsKey(token)) {
            long expirationTimestamp = writeTokens.get(token);
            long currentTimestamp = System.currentTimeMillis();

            if (currentTimestamp > expirationTimestamp) {
                // Token expired, remove it
                writeTokens.remove(token);
                System.out.println("Token EXPIRED and removed: " + token + " (expired at: " + Instant.ofEpochMilli(expirationTimestamp) + ")");
                return false;
            } else {
                System.out.println("Token verified: " + token + " (WRITE token, expires at: " + Instant.ofEpochMilli(expirationTimestamp) + ")");
                return true;
            }
        }

        // Token not found in either store
        System.out.println("Token NOT FOUND: " + token);
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
