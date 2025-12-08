package database;

import controller.DatabaseController;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class DatabaseServer {
    private static final int PORT = 8080;
    private static final String BIND_IP = "0.0.0.0";
    private static final int THREAD_POOL_SIZE = 10;
    private static ExecutorService threadPool;

    public static void startDatabaseServer(int port) throws IOException {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);

        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Database server listening on " + BIND_IP + ":" + port + "...");

            while (true) {
                try {
                    Socket clientSocket = listener.accept();
                    System.out.println("Municipality connected from: " + clientSocket.getInetAddress().getHostAddress());

                    threadPool.submit(() -> {
                        try {
                            handleMunicipalityClient(clientSocket);
                        } catch (Exception ex) {
                            System.err.println("Error handling municipality client: " + ex.getMessage());
                        }
                    });

                } catch (IOException e) {
                    System.err.println("Error accepting municipality connection: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        // Set SSL properties for database server
        System.setProperty("javax.net.ssl.keyStore", "../certificates/database/db.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "../certificates/database/db-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");

        System.out.println("Database Server starting on port " + PORT);

        try {
            startDatabaseServer(PORT);
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleMunicipalityClient(Socket clientSocket) throws Exception {
        DatabaseController controller = new DatabaseController();

        try (Socket socket = clientSocket) {
            String message = "";

            while (!message.equals("Exit")) {
                try {
                    System.out.println("=== DATABASE SERVER DEBUG ===");
                    System.out.println("Reading request from municipality...");

                    // Read request using buffered reader (similar to original approach but SSL-aware)
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String request = in.readLine();
                    System.out.println("Request received: " + request);

                    if (request == null || request.equals("Exit")) {
                        break;
                    }

                    System.out.println("Processing municipality request...");
                    String response = controller.processRequest(request);
                    System.out.println("Response generated: " + response);

                    // Send response back to municipality
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(response);
                    System.out.println("Response sent to municipality");

                    System.out.println("=== DATABASE SERVER DEBUG END ===");

                } catch (IOException e) {
                    System.err.println("Communication error with municipality: " + e.getMessage());
                    break;
                }
            }
            System.out.println("Municipality disconnected");

        } catch (IOException e) {
            System.err.println("Socket error: " + e.getMessage());
        }
    }

}