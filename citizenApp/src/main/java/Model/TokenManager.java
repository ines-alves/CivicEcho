package model;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Base64;

//Singleton class to manage tokens across the citizen application
public class TokenManager {
    // token structure = "token-signature"
    // token = "randomNumber:expirationTimestamp:location"

    private static final TokenManager INSTANCE = new TokenManager();

    private final Queue<String> tokenQueue = new LinkedList<>();
    private String readToken;

    private TokenManager() {} // prevent new instances

    public static TokenManager getInstance() {
        return INSTANCE;
    }

    public void setReadToken(String token){
        this.readToken = token;
    }

    public String getReadToken(){
        return this.readToken;
    }

    public void addToken(String token) {
        if (token != null) {
            tokenQueue.add(token);
        }
    }

    public String getToken() {
        return tokenQueue.poll();
    }

    public int getTokenCount() {
        if (areTokensValid()) {
            return tokenQueue.size();
        } else {
            tokenQueue.clear();
            return 0;
        }
    }

    private boolean areTokensValid(){
        String token = tokenQueue.peek();
        if (token == null) {
            return false;
        }
        String[] parts = token.split("\\-");
        byte[] partsDeocoded = Base64.getDecoder().decode(parts[0]);
        String partsString = new String(partsDeocoded, StandardCharsets.UTF_8);
        String[] tokenParts = partsString.split("\\:");
        long expirationTimestamp = Long.parseLong(tokenParts[1]);
        long timestamp = System.currentTimeMillis();
        if (timestamp > expirationTimestamp) {
            return false; // Tokens expired
        }
        return true;
    }
}
