import java.io.IOException;
import java.net.Socket;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import processor.MessageProcessor;

public class MunicipalityApp {

    public static void startMunicipalityServer(int port) throws IOException {
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();

        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("Municipality server listening on port " + port + "...");

            try (Socket clientSocket = listener.accept()) {
                System.out.println("Centralized party connected from: " + clientSocket.getInetAddress().getHostAddress());
                handleCentralizedPartyRequest(clientSocket);
                System.out.println("Centralized party disconnected");
            }
        }
    }

    private static void handleCentralizedPartyRequest(Socket clientSocket) throws Exception {
        MessageProcessor processor = new MessageProcessor();
        String message = "";

        while (!message.equals("Exit")) {
            try {
                // Read secure document from centralized party
                String secureDocument = sockets.SocketUtils.readFromSocket(clientSocket);
                System.out.println("Received secure document from centralized party");

                if (secureDocument.equals("Exit")) {
                    break;
                }

                processor.processSecureDocument(secureDocument);

                // Send response back to centralized party
                String response = "Document processed successfully by municipality";
                sockets.SocketUtils.writeToSocket(clientSocket, response);
                System.out.println("Sent response to centralized party");

            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
                break;
            }
        }
    }
}
