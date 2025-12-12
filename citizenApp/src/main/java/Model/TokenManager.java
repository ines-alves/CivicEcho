package model;

import java.util.LinkedList;
import java.util.Queue;

//Singleton class to manage tokens across the citizen application
public class TokenManager {

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
}
