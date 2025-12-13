package tokens;

public class citizenInfo {
    private final String publicKey;
    private final String location;
    private Long requestTime;

    public citizenInfo(String publicKey, String location, Long requestTime) {
        this.publicKey = publicKey;
        this.location = location;
        this.requestTime = requestTime;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getLocation() {
        return location;
    }

    public Long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Long requestTime) {
        this.requestTime = requestTime;
    }
}
