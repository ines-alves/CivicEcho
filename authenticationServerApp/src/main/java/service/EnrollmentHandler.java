package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import token.TokenGenerator;

public class EnrollmentHandler {
    
    private Map<String, String> authorizedClients;

    private ProtectDocumentLibrary protectDocLib = new ProtectDocumentLibrary();
    
    public EnrollmentHandler() {
        this.authorizedClients = new HashMap<>();
        loadAuthorizedClients();
    }
    
    private void loadAuthorizedClients() {
       
        authorizedClients.put("citizen1", "../pubkeys/citizen1.pubkey");

    }

    public void handleEnrollmentRequest(Socket clientSocket) {
        try {
            String request = SocketUtils.readFromSocket(clientSocket);
            
            String response = processEnrollment(request);
            
            SocketUtils.writeToSocket(clientSocket, response);
            
        } catch (Exception e) {
            System.err.println("[Enrollment] Error handling request: " + e.getMessage()); 
        } finally {
            SocketUtils.closeSocket(clientSocket);
        }
    }
    

    private String processEnrollment(String request) {
        try {
            String[] parts = request.split("\\|");
            if (parts.length != 3) {
                return "ERROR|Invalid request format. Expected: clientId|csr|signature";
            }
            
            String clientId = parts[0];
            String csr = parts[1];
            String signatureBase64 = parts[2];

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] csrBytes = Base64.getDecoder().decode(csr);
            
            System.out.println("[Enrollment] Processing request for: " + clientId);
            
            // 1. Check if client is authorized
            if (!isClientAuthorized(clientId)) {
                return "ERROR|Unauthorized client: " + clientId;
            }
            
            if (!protectDocLib.verifySignature(csrBytes, signatureBytes, authorizedClients.get(clientId))) {
                return "ERROR|Invalid signature";
            }
            
            System.out.println("[Enrollment] Client " + clientId + " authenticated successfully");
            
            String tempCsrFile = saveCsrToTempFile(clientId, csr);
            
            String signedCertFile = signCsrWithScript(clientId, tempCsrFile);
            
            if (signedCertFile == null) {
                return "ERROR|Failed to sign certificate";
            }
            
            String certificateBase64 = readAndEncodeCertificate(signedCertFile);

        
            //cleanupTempFiles(tempCsrFile, signedCertFile);
            
            System.out.println("[Enrollment] Successfully issued certificate to: " + clientId);
            
            String tokens = "";
            for (int i = 0; i < 10; i++) { // Generate 10 tokens
                tokens += "|";
                tokens += TokenGenerator.generateToken();
            }

            System.out.println("THESE ARE THE TOKENS " + tokens);

            return "SUCCESS|" + certificateBase64 + tokens;
            
        } catch (Exception e) {
            System.err.println("[Enrollment] Processing error: " + e.getMessage());
            return "ERROR|" + e.getMessage();
        }
    }
    
    private boolean isClientAuthorized(String clientId) {
        return authorizedClients.containsKey(clientId);
    }
    

    private String saveCsrToTempFile(String clientId, String csr) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filename = tempDir + "/csr-" + clientId + "-" + System.currentTimeMillis() + ".pem";
        
        byte[] csrBytes = Base64.getDecoder().decode(csr);
        Files.write(Paths.get(filename), csrBytes);
        
        System.out.println("[Enrollment] CSR saved to: " + filename);
        return filename;
    }

   private String signCsrWithScript(String clientId, String csrFile) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String outputFile = tempDir + "/cert-" + clientId + "-" + 
                System.currentTimeMillis() + ".crt";
            
            // Build simple command (2 parameters only)
            ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "../sign-certificate.sh",
                csrFile,
                outputFile
            );
            
            pb.directory(new File(".")); 
            
            // Run script
            Process process = pb.start();
            
            // Capture output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("[Enrollment] Script output: " + output.toString());
                return outputFile;
            } else {
                System.err.println("[Enrollment] Script failed with code " + exitCode);
                System.err.println("Output: " + output.toString());
                System.err.println("Error: " + errorOutput.toString());
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("[Enrollment] Error running signing script: " + e.getMessage());
            return null;
        }
    }

    private String readAndEncodeCertificate(String certFile) throws IOException {
        byte[] certBytes = Files.readAllBytes(Paths.get(certFile));
        return Base64.getEncoder().encodeToString(certBytes);
    }
    
    private void cleanupTempFiles(String... files) {
        for (String file : files) {
            try {
                if (file != null) {
                    Files.deleteIfExists(Paths.get(file));
                }
            } catch (IOException e) {
                System.err.println("[Enrollment] Failed to delete temp file " + file + ": " + e.getMessage());
            }
        }
    }
   
}