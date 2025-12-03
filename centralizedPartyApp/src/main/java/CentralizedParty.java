import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import sockets.SocketUtils;


public class CentralizedParty {
    private static final String BIND_IP = "0.0.0.0"; // Talvez trocar para subnet
    private static final Dictionary<String, String> municipalityList = new Hashtable<>();
    private static final int MUNICIPALITY_PORT = 5000; 

    public static void startCentralizedParty(int port) throws IOException {
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);
        
        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Centralized Party listening on port " + port + "...");
            
            initializeMunicipalities();

            while (true) {
                try {
                    Socket clientSocket = listener.accept();
                    System.out.println("New client connected from: " + clientSocket.getInetAddress().getHostAddress());
       
                    new Thread(() -> {
                        try {
                            handleClient(clientSocket);
                        } catch (Exception ex) {
                        }
                    }).start();
                    
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    private static void handleClient(Socket clientSocket) throws Exception {
        
        try (Socket socket = clientSocket) {
            String message = "";
            
            while (!message.equals("Exit")) {
                try {
                    message = SocketUtils.readFromSocket(socket);
                    System.out.printf("Received %d bytes: %s%n", message.length(), message);

                    // AUTHENTICATE AND RESIGN MESSAGE HERE
                    
                    String serverResponse = connectToMunicipalityAndForward(message);
                    
                    String response = message + " processed by centralized party | Municipality: " + serverResponse;
                    
                    SocketUtils.writeToSocket(socket, response);
                    System.out.printf("Sent response to client%n");
                    
                } catch (IOException i) {
                    System.out.printf("Communication error: %s%n",  i.getMessage());
                    break;
                }
            }
            System.out.printf("Client disconnected%n");
            
        } catch (IOException e) {
            System.err.printf("Socket error: %s%n", e.getMessage());
        }
    }
    
    private static String connectToMunicipalityAndForward(String message) throws Exception {
        String location = SocketUtils.getLocationFromSocketMessage(message);
        String SERVER_IP = getMunicipalityIP(location);
        if (SERVER_IP == null) {
            return "Unknown municipality for location: " + location;
        }
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(SERVER_IP, MUNICIPALITY_PORT)) {
            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });
            
            SocketUtils.writeToSocket(socket, message);
            
            String serverResponse = SocketUtils.readFromSocket(socket);
            System.out.println("Received response from municipality: " + serverResponse);
            
            return serverResponse;
        } catch (IOException e) {
            System.err.println("Error connecting to municipality: " + e.getMessage());
            return "Municipality connection failed";
        }
    }
    
    private static String getMunicipalityIP(String location) {
        String IP = municipalityList.get(location);
        if (IP == null) {
            System.err.println("No IP found for location: " + location);
        }
        return IP;
    }

    private static void initializeMunicipalities() {
        municipalityList.put("Lisbon", "");
        municipalityList.put("Porto", "");
    }
    public static void main(String args[]) throws IOException {
        // Set SSL properties
        System.setProperty("javax.net.ssl.keyStore", "server.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "servertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        
        // Start the centralized party
        startCentralizedParty(5000);
    }
}