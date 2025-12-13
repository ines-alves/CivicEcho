package model;

import java.util.LinkedList;
import java.util.Queue;

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
        String[] parts = token.split("\\:");
        long timestamp = System.currentTimeMillis();
        long expirationTimestamp = Long.parseLong(parts[1]);
        if (timestamp > expirationTimestamp) {
            return false; // Tokens expired
        }
        return true;
    }
}
