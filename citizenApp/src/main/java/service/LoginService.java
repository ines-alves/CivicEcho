package service;

import javax.net.ssl.SSLSocket;

import model.TokenManager;
import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.io.File;

public class LoginService {
    
    private static final String CITIZEN_PRIV_KEY_PATH = "citizens/citizen1/citizen1.privatekey";
    private static final String CITIZEN_CERTIFICATE_PATH = "citizens/citizen1/citizen1.csr";
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();
        
    public static boolean sendLogin(SSLSocket socket, String citizenID) {
        try {

            Path path = Paths.get(CITIZEN_CERTIFICATE_PATH);
            byte[] certificateBytes = Files.readAllBytes(path);

            byte[] signature = protectDocumentLibrary.sign(certificateBytes, CITIZEN_PRIV_KEY_PATH);

            String signatureBase64 = Base64.getEncoder().encodeToString(signature);
            String certificateBase64 = Base64.getEncoder().encodeToString(certificateBytes);
            
            String loginMessage = "LOGIN|" + citizenID + "|" + certificateBase64 + "|" + signatureBase64;

            SocketUtils.writeToSocket(socket, loginMessage);
            
            String response = SocketUtils.readFromSocket(socket);

            
            String[] parts = response.split("\\|");
            
            String autentication = parts[0];

            System.out.println("Received response from server: " + autentication);//DEBUG

            if (autentication.equals("ERROR")) {
                System.out.println("authentication failed: ") ;
                return false;
            }

            String crt = parts[1];
            byte[] crtBytes = Base64.getDecoder().decode(crt);
            Path crtPath = Paths.get("citizens/citizen1/citizen1.crt");
            Files.write(crtPath, crtBytes);

            ProcessBuilder pb = new ProcessBuilder(
                "openssl", "pkcs12", "-export",
                "-in", "citizen1.crt",
                "-inkey", "citizen1.key",
                "-out", "citizen1.p12",
                "-passout", "pass:changeme"
            );


            pb.directory(new File("citizens/citizen1"));

            // Inherit IO to see output in console
            pb.inheritIO(); //debug

            // Start the process
            Process process = pb.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Error creating PKCS#12 file, exit code: " + exitCode);
                return false;
            }

            TokenManager tokenManager = TokenManager.getInstance();
            for (int i = 2; i < parts.length; i++) {
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
