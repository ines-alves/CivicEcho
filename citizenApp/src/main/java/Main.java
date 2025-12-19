import java.io.File;
import java.io.IOException;

import cli.CLIController;

public class Main {
    public static void main(String[] args) throws IOException {
        // Check if municipality argument is provided
        if (args.length < 1) {
            System.err.println("Usage: mvn exec:java -Dexec.args=\"citizenID\"");
            System.exit(1);
        }
        
        String citizenID = args[0].toLowerCase();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "../generate-citizen-csr.sh",
                citizenID,
                "../certificates/authentication-server/ca.crt"
            );
            
            pb.directory(new File(".")); 
            pb.redirectErrorStream(true);
            
            Process process = pb.start();

            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.out.println("generate-citizen-csr.sh script failed with code " + exitCode);
                return;
            }
        } catch (Exception e) {
            System.err.println("Application interrupted: " + e.getMessage());
            return;
        }

        System.setProperty("javax.net.ssl.trustStore", "citizens/" + citizenID + "/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        
        CLIController cli = new CLIController();
        
        // Login to auth server
        boolean authenticated = cli.login("192.168.0.20", 8090);
        
        if (authenticated) {

            cli.start("192.168.0.10", 8090);
        }
    }
}