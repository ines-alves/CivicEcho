import cli.CLIController;
import java.io.IOException;
import java.io.File;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "../generate-citizen-csr.sh",
                "citizen1",
                "../certificates/authentication-server/server.crt"
            );
            
            pb.directory(new File(".")); 
            
            // Run script
            Process process = pb.start();
    
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("[Enrollment] Script ended sucessfully" );
            } else {
                System.err.println("[Enrollment] Script failed with code " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Application interrupted: " + e.getMessage());
            return;
        }

        // Set SSL properties
        System.setProperty("javax.net.ssl.trustStore", "citizens/citizen1/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        
        
        CLIController cli = new CLIController();
        boolean authenticated = cli.login("192.168.0.20", 8090); //fix the ip and port
        // Once the user is authenticated, start the main loop
        if (authenticated) {
            System.setProperty("javax.net.ssl.keyStore", "citizens/citizen1/citizen1.p12");
            System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
            cli.start("192.168.0.10", 8090);
        }
    }
}
