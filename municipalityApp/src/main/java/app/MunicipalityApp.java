package app;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import processor.MessageProcessor;
import sockets.SocketUtils;

public class MunicipalityApp {
    private static final String BIND_IP = "192.168.1.254"; // IP da centralized Party no VMNET3

    public static void startMunicipalityServer(int port) throws IOException {
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);


        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Municipality server listening on port " + port + "...");

            while (true) {  // Add loop to accept multiple connections
            try (Socket clientSocket = listener.accept()) {
                System.out.println("New CentralizedParty connected from: " + clientSocket.getInetAddress().getHostAddress());
                handleCentralizedPartyRequest(clientSocket);
                System.out.println("CentralizedParty disconnected from: " + clientSocket.getInetAddress().getHostAddress());
            } catch (Exception e) {
                System.err.println("Error handling centralized party request: " + e.getMessage());
                e.printStackTrace();
                // Don't break - continue accepting new connections
            }
        }
        }
    }

    private static void handleCentralizedPartyRequest(Socket clientSocket) throws Exception {
        MessageProcessor processor = new MessageProcessor();

        try {
            // Read secure document from centralized party using utils
            String secureDocument = SocketUtils.readSocketToJsonFile(clientSocket);
            System.out.println("Received secure document from centralized party");
            String processingResult = processor.processSecureDocument(secureDocument);
            
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
            throw e;
        }
    }

    public static String connectToDatabase(String host, int port, String request) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });

            System.out.println("Connected to database at " + host + ":" + port);

            // Send request to database
            sockets.SocketUtils.writeToSocket(socket, request);
            System.out.println("Sent request to database: " + request);

            // Read response from database
            String response = sockets.SocketUtils.readFromSocket(socket);
            System.out.println("Received response from database: " + response);

            return response;

        } catch (IOException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            throw e;
        }
    }

}
