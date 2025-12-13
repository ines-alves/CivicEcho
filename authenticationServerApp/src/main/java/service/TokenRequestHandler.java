package service;

import java.util.Base64;
import java.net.Socket;

import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import tokens.TokenManager;

public class TokenRequestHandler {
    private ProtectDocumentLibrary protectDocLib = new ProtectDocumentLibrary();
    private final TokenManager tokenManager = TokenManager.getInstance();

    public void handleTokenRequest(Socket clientSocket, String request) {
        try {
            String response = processTokenRequest(request);
            
            SocketUtils.writeToSocket(clientSocket, response);
            
        } catch (Exception e) {
            System.err.println("[TokenRequest] Error handling request: " + e.getMessage()); 
        } finally {
            SocketUtils.closeSocket(clientSocket);
        }
    }

    private  String processTokenRequest(String request) {
        try {
            String[] parts = request.split("\\|");
            if (parts.length != 2) {
                return "ERROR|Invalid request format. Expected: citizenID|signature";
            }
            
            String citizenID = parts[1];
            byte[] citizenIDBytes = citizenID.getBytes();
            String signatureBase64 = parts[2];
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            
            System.out.println("[TokenRequest] Processing request for: " + citizenID);
            
            if (tokenManager.getCitizenPublicKey(citizenID) == null) {
                return "ERROR|Unauthorized client: " + citizenID;
            }
            
            if (!protectDocLib.verifySignature(citizenIDBytes, signatureBytes, tokenManager.getCitizenPublicKey(citizenID))) {
                return "ERROR|Invalid signature";
            }
            
            System.out.println("[TokenRequest] Client " + citizenID + " authenticated successfully");
            
            String tokens = tokenManager.requestTokens(citizenID, false);
 
            if (tokens == null) {
                System.out.println("TOO SOON SINCE LAST REQUEST");//debug
                return "ERROR|Too soon since last request"; 
            }
 
            System.out.println("THESE ARE THE TOKENS " + tokens);
            
            return "SUCCESS|" + tokens;
            
        } catch (Exception e) {
            System.err.println("[TokenRequest] Error processing request: " + e.getMessage());
            return "ERROR|Internal server error";
        }
    }

}
