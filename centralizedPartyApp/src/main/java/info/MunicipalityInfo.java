package info;
public class MunicipalityInfo {
    String ipAddress;
    int port;
    
    public MunicipalityInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }
    
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }
        
    }