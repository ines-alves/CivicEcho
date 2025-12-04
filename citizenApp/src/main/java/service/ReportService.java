package service;

import model.Report;
import sockets.SocketUtils;
import report.ProtectDocumentLibrary;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import javax.net.ssl.SSLSocket;

public class ReportService {

    private static final String SYMMETRIC_KEY_PATH = "symmetricKey.key";
    private static final String CITIZEN_PRIV_KEY_PATH = "citizen_priv.key";
    private static final String MUNICIPALITY_PUB_KEY_PATH = "municipality_pub.key";
    private static final String TEMP_FILE_DIR = "temp_files/";
    private static final ProtectDocumentLibrary protectDocumentLibrary = new ProtectDocumentLibrary();

    public static void sendReport(SSLSocket socket, Report report) {
        try {
            ReportStorage.saveReport(report);

            String fileName = TEMP_FILE_DIR + report.getReportId() + ".json";
            String protectedFileName = TEMP_FILE_DIR + "protected_" + report.getReportId() + ".json";

            protectDocumentLibrary.protect(fileName, protectedFileName, SYMMETRIC_KEY_PATH, CITIZEN_PRIV_KEY_PATH, MUNICIPALITY_PUB_KEY_PATH, "CREATE");

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