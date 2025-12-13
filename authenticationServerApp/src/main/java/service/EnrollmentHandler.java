package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import tokens.TokenManager;

public class EnrollmentHandler {
    
    private ProtectDocumentLibrary protectDocLib = new ProtectDocumentLibrary();
    private final TokenManager tokenManager = TokenManager.getInstance();
    
    public EnrollmentHandler() { // Initialize the authorized citizens
        tokenManager.addCitizen("citizen1", "../pubkeys/citizen1.pubkey", "Lisbon");
    }

    public void handleEnrollmentRequest(Socket clientSocket, String request) {
        try {
            
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
                return "ERROR|Invalid request format. Expected: citizenID|csr|signature";
            }
            
            String citizenID = parts[1];
            String csr = parts[2];
            String signatureBase64 = parts[3];

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] csrBytes = Base64.getDecoder().decode(csr);
            
            System.out.println("[Enrollment] Processing request for: " + citizenID);
            
            // 1. Check if client is authorized
            if (tokenManager.getCitizenPublicKey(citizenID) == null) {
                return "ERROR|Unauthorized client: " + citizenID;
            }
            
            if (!protectDocLib.verifySignature(csrBytes, signatureBytes, tokenManager.getCitizenPublicKey(citizenID))) {
                return "ERROR|Invalid signature";
            }
            
            System.out.println("[Enrollment] Client " + citizenID + " authenticated successfully");
            
            String tempCsrFile = saveCsrToTempFile(citizenID, csr);
            
            String signedCertFile = signCsrWithScript(citizenID, tempCsrFile);
            
            if (signedCertFile == null) {
                return "ERROR|Failed to sign certificate";
            }
            
            String certificateBase64 = readAndEncodeCertificate(signedCertFile);
        
            //cleanupTempFiles(tempCsrFile, signedCertFile);
            
            System.out.println("[Enrollment] Successfully issued certificate to: " + citizenID);
            
            String tokens = tokenManager.requestTokens(citizenID);

            if (tokens == null) {
                return "ERROR|Something that is wrong is not wright"; // This should never happen, future diogo: this kinda can happend if a cient disconects and login again, we prevent that kidna of abuse, nice.
            }

            System.out.println("THESE ARE THE TOKENS " + tokens);

            return "SUCCESS|" + certificateBase64 + tokens;
            
        } catch (Exception e) {
            System.err.println("[Enrollment] Processing error: " + e.getMessage());
            return "ERROR|" + e.getMessage();
        }
    }

    private String saveCsrToTempFile(String citizenID, String csr) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filename = tempDir + "/csr-" + citizenID + "-" + System.currentTimeMillis() + ".pem";
        
        byte[] csrBytes = Base64.getDecoder().decode(csr);
        Files.write(Paths.get(filename), csrBytes);
        
        System.out.println("[Enrollment] CSR saved to: " + filename);
        return filename;
    }

   private String signCsrWithScript(String citizenID, String csrFile) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String outputFile = tempDir + "/cert-" + citizenID + "-" + 
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