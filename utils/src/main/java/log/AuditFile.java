package log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditFile {

    private static final String AUDIT_FILE_PATH = "audit.log";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Object LOCK = new Object();

    /* ===========================
       Public Audit Methods
       =========================== */

    public static void clientUnprotectedCreate(String json) {
        log("CLIENT", "UNPROTECTED REPORT JSON", json);
    }

    public static void clientProtectedCreate(String protectedJson) {
        log("CLIENT", "PROTECTED REPORT JSON", protectedJson);
    }

    public static void clientUnprotectedRead(String json) {
        log("CLIENT", "UNPROTECTED READ REQUEST", json);
    }

    public static void clientProtectedRead(String protectedJson) {
        log("CLIENT", "PROTECTED READ REQUEST", protectedJson);
    }

    public static void centralizedReceivesProtected(String protectedJson) {
        log("CENTRALIZED", "RECEIVED PROTECTED JSON", protectedJson);
    }

    public static void centralizedValidatesToken(String tokenResult) {
        log("CENTRALIZED", "TOKEN VALIDATION RESULT", tokenResult);
    }

    public static void centralizedFowarding(String response, String toWho) {
        log("CENTRALIZED", "FORWARDING TO " + toWho, response);
    }

    public static void municipalityReceivesProtected(String protectedJson) {
        log("MUNICIPALITY", "RECEIVED PROTECTED JSON", protectedJson);
    }

    public static void municipalityDeciphersJson(String json) {
        log("MUNICIPALITY", "DECIPHERED JSON", json);
    }

    public static void municipalitySqlQuery(String query) {
        log("MUNICIPALITY", "SQL QUERY", query);
    }

    public static void municipalityCreateResponse(String response) {
        log("MUNICIPALITY", "CREATE RESPONSE", response);
    }

    public static void municipalityReadResponse(String response) {
        log("MUNICIPALITY", "READ RESPONSE", response);
    }

    public static void municipalityProtectedResponse(String response) {
        log("MUNICIPALITY", "PROTECTED READ RESPONSE", response);
    }

    public static void clientCreateResponse(String response) {
        log("CLIENT", "CREATE REQUEST RESPONSE", response);
    }

    public static void clientProtectedReadResponse(String response) {
        log("CLIENT", "PROTECTED READ REQUEST RESPONSE", response);
    }

    public static void clientUnprotectedReadResponse(String response) {
        log("CLIENT", "UNPROTECTED READ REQUEST RESPONSE", response);
    }



    /* ===========================
       Core Logging Logic
       =========================== */

    private static void log(String actor, String action, String data) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);

        StringBuilder entry = new StringBuilder();
        entry.append("--------------------------------------------------\n");
        entry.append("Time   : ").append(timestamp).append("\n");
        entry.append("Actor  : ").append(actor).append("\n");
        entry.append("Action : ").append(action).append("\n");
        entry.append("Data   : ").append(data).append("\n");

        writeToFile(entry.toString());
    }

    private static void writeToFile(String content) {
        synchronized (LOCK) {
            try (BufferedWriter writer =
                         new BufferedWriter(new FileWriter(AUDIT_FILE_PATH, true))) {
                writer.write(content);
            } catch (IOException e) {
                System.err.println("Failed to write audit log: " + e.getMessage());
            }
        }
    }
}

