import java.io.File;
import java.io.IOException;

import cli.CLIController;

public class Main {
    public static void main(String[] args) throws IOException { //maybe add a flag to substitute "citizen1" with a custom citizen ID
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "../generate-citizen-csr.sh",
                "citizen1",
                "../certificates/authentication-server/ca.crt"
            );
            
            pb.directory(new File(".")); 
            pb.redirectErrorStream(true);
            
            Process process = pb.start();

            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("[Enrollment] Script ended successfully");
            } else {
                System.err.println("[Enrollment] Script failed with code " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Application interrupted: " + e.getMessage());
            return;
        }

        System.setProperty("javax.net.ssl.trustStore", "citizens/citizen1/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        
        CLIController cli = new CLIController();
        
        // Login to auth server
        boolean authenticated = cli.login("192.168.0.20", 8090);
        
        if (authenticated) {

            cli.start("192.168.0.10", 8090);
        }
    }
}