package keys;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class AESKeyGenerator {

    public static void main(String[] args) throws Exception {

        // check args
        if (args.length < 2) {
            System.err.println("Usage: aes-key-gen [r|w] <key-file>");
            return;
        }

        final String mode = args[0];
        final String keyPath = args[1];

        if (mode.toLowerCase().startsWith("w")) {
            write(keyPath);
        } else {
            read(keyPath);
        }

    }

    public static void write(String keyPath) throws GeneralSecurityException, IOException {
        // get an AES private key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();
        byte[] encoded = key.getEncoded();

        FileOutputStream fos = new FileOutputStream(keyPath);
        fos.write(encoded);
        fos.close();
    }

    public static Key read(String keyPath) throws GeneralSecurityException, IOException {
        FileInputStream fis = new FileInputStream(keyPath);
        byte[] encoded = new byte[fis.available()];
        fis.read(encoded);
        fis.close();

        return new SecretKeySpec(encoded, 0, 16, "AES");
    }

}
