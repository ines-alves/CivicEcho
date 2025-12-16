import processor.MessageProcessor;
import app.MunicipalityApp;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // Check if municipality argument is provided
        if (args.length < 1) {
            System.err.println("Usage: java Main <municipality>");
            System.err.println("Available municipalities: Lisbon, Porto");
            System.exit(1);
        }

        String municipality = args[0].toLowerCase();

        // Validate municipality argument
        if (!municipality.equals("lisbon") && !municipality.equals("porto")) {
            System.err.println("Error: Invalid municipality '" + args[0] + "'");
            System.err.println("Available municipalities: lisbon, porto");
            System.exit(1);
        }

        // Set SSL properties based on municipality
        String keystorePath = "../certificates/municipalities/" + municipality + "/" + municipality + ".p12";
        String truststorePath = "../certificates/municipalities/" + municipality + "/" + municipality + "-truststore.jks";

        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", truststorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");

        System.out.println("Municipality App Started - " + municipality);

        int port = 8091; // Same port for all municipalities since they run on different VMs
        System.out.println("Starting " + municipality + " municipality server on port " + port + "...");
        MunicipalityApp.startMunicipalityServer(port, municipality);
    }
}