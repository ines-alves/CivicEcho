package cli;

import controller.ReportController;
import controller.LoginController;

import java.util.Scanner;
import java.io.IOException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class CLIController {

    private final Scanner scanner = new Scanner(System.in);
    private final ReportController reportController = new ReportController();
    private final LoginController loginController = new LoginController();

    public void login(String host, int port) {
        SocketFactory factory = SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            
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
                            System.out.println("Terminating application.");
                            return;
                        }
                        try {
                            socket.close();
                        } catch (IOException i) {
                            System.out.println(i);
                            return;
                        }
                        // Once the user is authenticated, start the main loop
                        start("192.168.0.10", 8090);
                        return;
                    case "0":
                        System.out.println("Goodbye!");
                        try {
                            scanner.close();
                            socket.close();
                        } catch (IOException i) {
                            System.out.println(i);
                            return;
                        }
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
    
    public void start(String host, int port) throws IOException {
        SocketFactory factory = SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            
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
                        try {
                            scanner.close();
                            socket.close();
                        } catch (IOException i) {
                            System.out.println(i);
                            return;
                        }
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}