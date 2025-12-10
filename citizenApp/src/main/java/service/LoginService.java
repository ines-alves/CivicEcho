package service;

import javax.net.ssl.SSLSocket;

import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class LoginService {
    
    private static final String CITIZEN_PRIV_KEY_PATH = "../alice.privkey"; //FIXME: change to the correct path
    private static final String CITIZEN_CERTIFICATE_PATH = "../alice.csr";  //FIXME: change to the correct path
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();
        
    public static boolean sendLogin(SSLSocket socket, String citizenID) {
        try {

            Path path = Paths.get(CITIZEN_CERTIFICATE_PATH);
            byte[] certificateBytes = Files.readAllBytes(path);

            byte[] signature = protectDocumentLibrary.sign(certificateBytes, CITIZEN_PRIV_KEY_PATH);

            String signatureBase64 = Base64.getEncoder().encodeToString(signature);
            String certificateBase64 = Base64.getEncoder().encodeToString(certificateBytes);
            
            String loginMessage = citizenID + "|" + certificateBase64 + "|" + signatureBase64;

            System.out.println("\n---- loginMessage: " + loginMessage + "\n-----"); //DEBUG

            SocketUtils.writeToSocket(socket, loginMessage);
            
            String response = SocketUtils.readFromSocket(socket);
            System.out.println("Received response from server: " + response);

            return true; //FIXME: check response for success/failure

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
