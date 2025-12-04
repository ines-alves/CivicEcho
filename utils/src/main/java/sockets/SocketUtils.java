package sockets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class SocketUtils {
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    public static String readFromSocket(Socket socket) throws IOException {
        InputStream is = new BufferedInputStream(socket.getInputStream());
        byte[] data = new byte[DEFAULT_BUFFER_SIZE];
        int len = is.read(data);
        return new String(data, 0, len);
    }
    

    public static void writeToSocket(Socket socket, String message) throws IOException {
        OutputStream os = new BufferedOutputStream(socket.getOutputStream());
        os.write(message.getBytes(), 0, message.getBytes().length);
        os.flush();
    }
    
    public static void closeSocketStreams(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.getInputStream().close();
                socket.getOutputStream().close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket streams: " + e.getMessage());
        }
    }
    

    public static void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                closeSocketStreams(socket);
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    public static String getLocationFromJsonFile(String fileName) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(fileName)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            JsonObject aad = jsonObject.getAsJsonObject("aad");
            return aad.get("location").getAsString();
        }
    }

    public static String getOperationFromJsonFile(String fileName) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(fileName)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            JsonObject aad = jsonObject.getAsJsonObject("aad");
            return aad.get("operation").getAsString();
        }
    }

    public static void readSocketToJsonFile(Socket socket, String outputFileName) throws IOException {

        String jsonString = readFromSocket(socket);
        Gson gson = new Gson();
  
        byte[] decoded = Base64.getDecoder().decode(jsonString);
        String jsonStringFromByteArray = new String(decoded, StandardCharsets.UTF_8);

        JsonObject jsonObject = gson.fromJson(jsonStringFromByteArray, JsonObject.class);
        
        try (FileWriter writer = new FileWriter(outputFileName)) {
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            prettyGson.toJson(jsonObject, writer);
        }
    }

    public static void writeJsonFileToSocket(Socket socket, String jsonFileName) throws IOException {
        Gson gson = new Gson();
        String jsonMessage;
        
        try (FileReader reader = new FileReader(jsonFileName)) {
            jsonMessage = gson.fromJson(reader, String.class);
        }
                
        writeToSocket(socket, jsonMessage);
    }

}