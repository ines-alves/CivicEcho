package model;

import java.util.LinkedList;
import java.util.Queue;

//Singleton class to manage tokens across the citizen application
public class TokenManager {

    final long TEN_MINUTES_MS = 10 * 60 * 1000; // token validity period

    private static final TokenManager INSTANCE = new TokenManager();

    private final Queue<String> tokenQueue = new LinkedList<>();

    private TokenManager() {} // prevent new instances

    public static TokenManager getInstance() {
        return INSTANCE;
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
        long tokenTime = Long.parseLong(parts[1]);
        if ((timestamp - tokenTime) > TEN_MINUTES_MS) {
            return false; // Tokens expired
        }
        return true;
    }
}
