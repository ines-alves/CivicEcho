import cli.CLIController;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // Set SSL properties
        System.setProperty("javax.net.ssl.keyStore", "../certificates/clients/citizen1/citizen1.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "../certificates/clients/citizen1/usertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");

        CLIController cli = new CLIController();
        cli.start("192.168.0.10", 8090);
    }
}
