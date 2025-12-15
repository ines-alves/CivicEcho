package service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import model.TokenManager;
import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import java.util.Base64;

public class TokenService {

    private static final String CITIZEN_PRIV_KEY_PATH = "citizens/citizen1/citizen1.privatekey";
    private static final String CITIZEN_CERTIFICATE_PATH = "citizens/citizen1/citizen1.csr";
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();
    
    public static boolean sendTokenRequest(String citizenID) {
        try (SSLSocket authSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket("192.168.0.20", 8090)) {
            authSocket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            authSocket.setEnabledProtocols(new String[] { "TLSv1.3" });

            byte[] citizenIDBytes = citizenID.getBytes();
            byte[] signature = protectDocumentLibrary.sign(citizenIDBytes, CITIZEN_PRIV_KEY_PATH);

            String signatureBase64 = Base64.getEncoder().encodeToString(signature);

            String tokenRequestMessage = "TOKEN|" + citizenID + "|" + signatureBase64;

            SocketUtils.writeToSocket(authSocket, tokenRequestMessage);

            String response = SocketUtils.readFromSocket(authSocket);
            
            String[] parts = response.split("\\|");
            
            String status = parts[0];

            System.out.println("Received response from server: " + status);//DEBUG

            if (status.equals("ERROR")) {
                System.out.println("Token request failed: " + parts[1]);
                return false;
            }

            TokenManager tokenManager = TokenManager.getInstance();
            for (int i = 1; i < parts.length; i++) {
                System.out.println("---\n" + "Adding token [" + (i-2) + "] " + parts[i] + "\n---"); //DEBUG
                tokenManager.addToken(parts[i]);
            }
            
            return true;

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
