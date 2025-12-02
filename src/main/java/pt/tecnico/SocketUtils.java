package pt.tecnico;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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
}