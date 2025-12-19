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
import java.security.cert.Certificate;
import java.security.PublicKey;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class MunicipalityApp {
    private static final String CENTRALZED_PUB_KEY = "../certificates/centralized-party/centralized.pubkey";
    private static final String BIND_IP = "0.0.0.0"; // IP da centralized Party no VMNET3
    private static String municipality; // Store municipality name for use in handlers

    public static void startMunicipalityServer(int port, String municipalityName) throws IOException {
        municipality = municipalityName;
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);


        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Municipality " + municipality + " server listening on port " + port + "...");

            while (true) {  // Add loop to accept multiple connections
                try (Socket clientSocket = listener.accept()) {
                    boolean result = extractCentralizedPartyCertificateInfo(clientSocket);
                    if (!result) {
                        System.out.println("Client certificate verification failed. Closing connection.");
                        continue;
                    }
                    System.out.println("CentralizedParty connected from: " + clientSocket.getInetAddress().getHostAddress());
                    handleCentralizedPartyRequest(clientSocket);
                    System.out.println("Response sent to CentralizedParty.");
                } catch (Exception e) {
                    System.err.println("Error handling centralized party request: " + e.getMessage());
                    e.printStackTrace();
                    // Don't break - continue accepting new connections
                }
            }
        }
    }

    private static void handleCentralizedPartyRequest(Socket clientSocket) throws Exception {
        MessageProcessor processor = new MessageProcessor(municipality);
        String tempProcessedFile = "temp_processed_" + System.currentTimeMillis() + ".json";

        try {

            // Read secure document from centralized party using utils
            SocketUtils.readSocketToJsonFile(clientSocket,tempProcessedFile);
            String processingResult = processor.processSecureDocument(tempProcessedFile);
            SocketUtils.writeToSocket(clientSocket, processingResult);

        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
            throw e;
        }
    }

    private static boolean extractCentralizedPartyCertificateInfo(Socket clientSocket) {
        try {
            if (clientSocket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) clientSocket;
                Certificate[] certs = sslSocket.getSession().getPeerCertificates();
                if (certs.length > 0) {
                    PublicKey receivedPubKey = certs[0].getPublicKey();
                    byte[] receivedPubKeyBytes = receivedPubKey.getEncoded();

                    byte[] expectedPubKeyBytes = Files.readAllBytes(Paths.get(CENTRALZED_PUB_KEY));

                    boolean match = Arrays.equals(receivedPubKeyBytes, expectedPubKeyBytes);
                    return match;
                }
            }
        } catch (Exception e) {
            System.err.println("Error verifying centralized party certificate: " + e.getMessage());
            return false;
        }
        return false;
    }

}
