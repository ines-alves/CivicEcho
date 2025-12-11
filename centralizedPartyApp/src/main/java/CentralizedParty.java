import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import info.MunicipalityInfo;
import sockets.SocketUtils;


public class CentralizedParty {
    private static final String BIND_IP = "0.0.0.0"; // Talvez trocar para subnet
    private static final Map<String, MunicipalityInfo> municipalityMap = new HashMap<>();
    private static final String TEMP_FILE_DIR = "temp_files/";
    private static final int THREAD_POOL_SIZE = 10;
    private static ExecutorService threadPool;


    static {
        new File(TEMP_FILE_DIR).mkdirs();
    }

    public static void startCentralizedParty(int port) throws IOException {

        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);
        
        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Centralized Party listening on port " + port + "...");
            
            initMunicipalityMap();

            while (true) {
                try {
                    Socket clientSocket = listener.accept();
                    System.out.println("New client connected from: " + clientSocket.getInetAddress().getHostAddress());
       
                    threadPool.submit(() -> {
                        try {
                            handleClient(clientSocket);
                        } catch (Exception ex) {
                            System.err.println("Error in client handler: " + ex.getMessage());
                        }
                    });
                    
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
                    String tempFileName = TEMP_FILE_DIR + "temp_" + System.currentTimeMillis() + "_" + 
                                        socket.getInetAddress().getHostAddress() + ".json";
                    String tempFileNameResign = TEMP_FILE_DIR + "tempResign_" + System.currentTimeMillis() + "_" + 
                                        socket.getInetAddress().getHostAddress() + ".json";
                    // Read from socket and save to JSON file
                    SocketUtils.readSocketToJsonFile(socket, tempFileName);
                                        
                    // Forward to municipality
                    String serverResponse = connectToMunicipalityAndForward(tempFileName);

                    // Delete the temporary file after forwarding
                    deleteFile(tempFileName, tempFileNameResign);

                    //String response = "JSON message processed by centralized party | Municipality response: " + serverResponse;
                    
                    SocketUtils.writeToSocket(socket, serverResponse);
                    System.out.printf("Sent response to client%n");
                    
                } catch (IOException i) {
                    System.out.printf("Communication error: %s%n",  i.getMessage());
                    break;
                }
            }
            System.out.printf("Client disconnected%n");
            shutdownThreadPool();
            
        } catch (IOException e) {
            System.err.printf("Socket error: %s%n", e.getMessage());
        }
    }
    
    private static String connectToMunicipalityAndForward(String fileName) throws Exception {

        String location = SocketUtils.getLocationFromJsonFile(fileName);
        MunicipalityInfo municipality = municipalityMap.get(location);
        
        if (municipality == null) {
            return "Unknown municipality for location: " + location;
        }
        
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(municipality.getIpAddress(), municipality.getPort())) {
            System.out.println("----CONNECTED TO MUNICIPALITY---");
            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });
            
            SocketUtils.prepareAndWriteJsonFileToSocket(socket, fileName);
            System.out.println("Sent JSON file name to municipality: " + fileName);
            String serverResponse = SocketUtils.readFromSocket(socket);
            System.out.println("Received response from municipality: " + serverResponse);
            
            return serverResponse;
            
        } catch (IOException e) {
            System.err.println("Error connecting to municipality: " + e.getMessage());
            return "Municipality connection failed";
        }
    }

    private static void initMunicipalityMap() {
        municipalityMap.put("Lisbon", new MunicipalityInfo("192.168.1.1", 8091));
        municipalityMap.put("Porto", new MunicipalityInfo("127.0.0.1", 8091)); // Porto still localhost for now
    }

    private static void deleteFile(String fileName, String fileNameResign) {
        try {
            Files.delete(Paths.get(fileName));
            Files.delete(Paths.get(fileNameResign));
        } catch (IOException ex) {
        }
        System.out.println("Deleted temporary files: " + fileName + ", " + fileNameResign);
    }

    private static void shutdownThreadPool() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
    public static void main(String args[]) throws IOException {
        // Set SSL properties
        System.setProperty("javax.net.ssl.keyStore", "../certificates/centralized-party/centralized.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "../certificates/centralized-party/centralized-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        // Start the centralized party
        startCentralizedParty(8090);
    }
}