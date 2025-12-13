package service;

import javax.net.ssl.SSLSocket;

import model.Report;
import report.ProtectDocumentLibrary;
import sockets.SocketUtils;

public class ReportService {

    private static final String SYMMETRIC_KEY_PATH = "../test_key.key";
    //private static final String CITIZEN_PRIV_KEY_PATH = "../alice.privkey";
    private static final String MUNICIPALITY_PUB_KEY_PATH = "../certificates/municipalities/lisbon/lisbon.pubkey";
    private static final String TEMP_FILE_DIR = "temp_files/";
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();

    public static void sendReport(SSLSocket socket, Report report, String token) {
        try {
            ReportStorage.saveReport(report);

            String fileName = TEMP_FILE_DIR + report.getReportId() + ".json";
            String protectedFileName = TEMP_FILE_DIR + "protected_" + report.getReportId() + ".json";

            protectDocumentLibrary.protect(fileName, protectedFileName, SYMMETRIC_KEY_PATH, MUNICIPALITY_PUB_KEY_PATH, "CREATE", token);

            SocketUtils.writeJsonFileToSocket(socket, protectedFileName);
            String response = SocketUtils.readFromSocket(socket);
            System.out.println("Received response from server: " + response);

            ReportStorage.deleteFiles(fileName, protectedFileName);

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}