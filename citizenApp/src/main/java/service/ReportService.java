package service;

import javax.net.ssl.SSLSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import model.Report;
import keys.AESKeyGenerator;
import report.ProtectDocumentLibrary;
import sockets.SocketUtils;
import model.TokenManager;
import log.AuditFile;

public class ReportService {

    private static final String SYMMETRIC_KEY_PATH = "temp_keys/symmetric.key";
    private static final String TEMP_FILE_DIR = "temp_files/";
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();
    private static final AESKeyGenerator aesKeyGenerator = new AESKeyGenerator();

    public static void sendReport(SSLSocket socket, Report report, String token) {
        try {
            Path path = Paths.get(SYMMETRIC_KEY_PATH);
            Files.createDirectories(path.getParent());

            // Generate symmetric key for this report request
            aesKeyGenerator.write(SYMMETRIC_KEY_PATH);

            ReportStorage.saveReport(report);
            TokenManager tokenManager = TokenManager.getInstance();
            String location = tokenManager.getWriteTokenLocation(token).toLowerCase();
            
            String fileName = TEMP_FILE_DIR + report.getReportId() + ".json";
            String protectedFileName = TEMP_FILE_DIR + "protected_" + report.getReportId() + ".json";
            
            AuditFile.clientUnprotectedCreate(Files.readString(Path.of(fileName)));

            protectDocumentLibrary.protect(fileName, protectedFileName, SYMMETRIC_KEY_PATH, "../certificates/municipalities/" + location + "/" + location + ".pubkey", "CREATE", token);

            AuditFile.clientProtectedCreate(Files.readString(Path.of(protectedFileName)));

            SocketUtils.writeJsonFileToSocket(socket, protectedFileName);
            String response = SocketUtils.readFromSocket(socket);
            System.out.println("Received response from server: " + response);
            
            AuditFile.clientCreateResponse(response);
            
            ReportStorage.deleteFile(fileName);
            ReportStorage.deleteFile(protectedFileName);
            ReportStorage.deleteFile(SYMMETRIC_KEY_PATH);

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void readReport(SSLSocket socket, String numberOfReports, String token) {
        try {
            Path path = Paths.get(SYMMETRIC_KEY_PATH);
            Files.createDirectories(path.getParent());
            // Generate symmetric key for this report request
            aesKeyGenerator.write(SYMMETRIC_KEY_PATH);

            TokenManager tokenManager = TokenManager.getInstance();
            ReportStorage.saveRead(numberOfReports, token);
            String location = tokenManager.getReadTokenLocation(token).toLowerCase();
            
            String fileName = TEMP_FILE_DIR + "readRequest" + ".json";
            String protectedFileName = TEMP_FILE_DIR + "protected_" + "readRequest" + ".json";
            String outputFileName = TEMP_FILE_DIR + "readRequest" + "_output" + ".json";
            String unprotectedFileName = TEMP_FILE_DIR + "unprotected_" + "readRequest" + "_output" + ".json";

            AuditFile.clientUnprotectedRead(Files.readString(Path.of(fileName)));

            protectDocumentLibrary.protect(fileName, protectedFileName, SYMMETRIC_KEY_PATH, "../certificates/municipalities/" + location + "/" + location + ".pubkey", "READ", token);

            AuditFile.clientProtectedRead(Files.readString(Path.of(protectedFileName)));

            SocketUtils.writeJsonFileToSocket(socket, protectedFileName);

            SocketUtils.readSocketToJsonFile(socket, outputFileName);
            
            AuditFile.clientProtectedReadResponse(Files.readString(Path.of(outputFileName)));

            protectDocumentLibrary.UnprotectSymmetric(outputFileName, unprotectedFileName, SYMMETRIC_KEY_PATH);

            AuditFile.clientUnprotectedReadResponse(Files.readString(Path.of(unprotectedFileName)));

            ReportPrinter.printReportsFromFile(unprotectedFileName);

            ReportStorage.deleteFile(fileName);
            ReportStorage.deleteFile(protectedFileName);
            ReportStorage.deleteFile(outputFileName);
            ReportStorage.deleteFile(unprotectedFileName);
            ReportStorage.deleteFile(SYMMETRIC_KEY_PATH);
            
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}