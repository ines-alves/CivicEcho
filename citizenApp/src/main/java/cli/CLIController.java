package cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import controller.LoginController;
import controller.ReportController;

public class CLIController {
    private final Scanner scanner = new Scanner(System.in);
    private final ReportController reportController = new ReportController();
    private final LoginController loginController = new LoginController();
    
    // Store the SSL factory (will be updated after login)
    private SSLSocketFactory sslSocketFactory;
    
    public CLIController() {
        // Start with default factory (no certificate yet)
        this.sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public boolean login(String host, int port) {
        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
            
            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });
            
            while (true) {
                System.out.println();
                System.out.println("┌─────────────────────────────────┐");
                System.out.println("│       CvicEcho CITIZEN APP      │");
                System.out.println("├─────────────────────────────────┤");
                System.out.println("│ 1 ▸ Login                       │");
                System.out.println("│ 0 ▸ Exit                        │");
                System.out.println("└─────────────────────────────────┘");
                System.out.print("Option: ");
                String option = scanner.nextLine().trim();

                switch (option) {
                    case "1":
                        boolean result = loginController.login(scanner, socket);
                        if (!result) {
                            System.out.println("Goodbye!");
                            return false;
                        }
                        
                        // AFTER SUCCESSFUL LOGIN: Update SSL factory with certificate
                        updateSSLSocketFactoryWithCertificate();
                        
                        return true;
                    case "0":
                        System.out.println("Goodbye!");
                        return false;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            return false;
        }
    }
    
    public void start(String host, int port) throws IOException {
        // Now use the UPDATED factory (with certificate)
        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
            
            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });
            
            while (true) {
                System.out.println();
                System.out.println("┌─────────────────────────────────┐");
                System.out.println("│       CvicEcho CITIZEN APP      │");
                System.out.println("│             REPORT              │");
                System.out.println("├─────────────────────────────────┤");
                System.out.println("│ 1 ▸ Create Report               │");
                System.out.println("│ 2 ▸ Read Report                 │");
                System.out.println("│ 0 ▸ Exit                        │");
                System.out.println("└─────────────────────────────────┘");
                System.out.print("Option: ");
                String option = scanner.nextLine().trim();

                switch (option) {
                    case "1":
                        reportController.createReport(scanner, socket);
                        break;
                    case "2":
                        System.out.println("Operation not implemented yet.");
                        break;
                    case "0":
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateSSLSocketFactoryWithCertificate() {
        try {
            System.out.println("\n=== Updating SSL Factory with citizen certificate ===");
            
            String keyStorePath = "citizens/citizen1/citizen1.p12";
            String keyStorePassword = "changeme";
            String trustStorePath = "citizens/citizen1/truststore.jks";
            String trustStorePassword = "changeme";
            
            // Verify files exist
            File keyStoreFile = new File(keyStorePath);
            File trustStoreFile = new File(trustStorePath);
            
            if (!keyStoreFile.exists()) {
                System.err.println("ERROR: KeyStore not found: " + keyStorePath);
                return;
            }
            if (!trustStoreFile.exists()) {
                System.err.println("ERROR: TrustStore not found: " + trustStorePath);
                return;
            }
            
            System.out.println("KeyStore exists: " + keyStoreFile.length() + " bytes");
            System.out.println("TrustStore exists: " + trustStoreFile.length() + " bytes");
            
            // Load keystore (citizen's certificate)
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            
            // Load truststore (CA certificate)
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(trustStorePath), trustStorePassword.toCharArray());
            
            // Create key manager
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, keyStorePassword.toCharArray());
            
            // Create trust manager
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore);
            
            // Create SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            
            // Update the factory
            this.sslSocketFactory = sslContext.getSocketFactory();
            
            System.out.println("SSL Factory updated with citizen certificate");
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to update SSL Factory: " + e.getMessage());
            e.printStackTrace();
        }
    }
}