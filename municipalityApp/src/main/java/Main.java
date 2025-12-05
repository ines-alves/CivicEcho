import processor.MessageProcessor;
import app.MunicipalityApp;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // Always use Lisbon municipality
        String municipality = "lisbon";

        // Set SSL properties for Lisbon based on certificates.sh structure
        System.setProperty("javax.net.ssl.keyStore", "../certificates/municipalities/lisbon/lisbon.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "../certificates/municipalities/lisbon/lisbon-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");

        System.out.println("Municipality App Started - " + municipality);

        int port = 6001; // Same port for all municipalities since they run on different VMs
        System.out.println("Starting " + municipality + " municipality server on port " + port + "...");
        MunicipalityApp.startMunicipalityServer(port);
    }
}