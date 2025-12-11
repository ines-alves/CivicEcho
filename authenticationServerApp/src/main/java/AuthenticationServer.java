import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import service.EnrollmentHandler;

public class AuthenticationServer {
    private static final String BIND_IP = "0.0.0.0";
    private static ExecutorService threadPool;
    private static final int THREAD_POOL_SIZE = 10;
    
    public static void startAuthenticationServer(int port) throws IOException {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        EnrollmentHandler enrollmentHandler = new EnrollmentHandler();
        
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        InetAddress bindAddress = InetAddress.getByName(BIND_IP);
        
        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port, 50, bindAddress)) {
            listener.setNeedClientAuth(false);
            
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            
            System.out.println("Authentication Server listening on port " + port + "...");
            
            while (true) {
                try {
                    Socket clientSocket = listener.accept();
                    System.out.println("New enrollment request from: " +  clientSocket.getInetAddress().getHostAddress());
       
                    threadPool.submit(() -> {
                        try {
                            enrollmentHandler.handleEnrollmentRequest(clientSocket);
                        } catch (Exception ex) {
                            System.err.println("Error in enrollment handler: " + ex.getMessage());
                        }
                    });
                    
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    
    public static void main(String args[]) throws IOException {

        System.setProperty("javax.net.ssl.keyStore", "../certificates/authentication-server/server.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        //System.setProperty("javax.net.ssl.trustStore", "../certificates/ca/servertruststore.jks");
        //System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        
        startAuthenticationServer(8090);
    }
}