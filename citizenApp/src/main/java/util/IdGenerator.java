package util;

import java.util.Random;

public class IdGenerator {

    public static String generateId(String prefix) {
        int number = new Random().nextInt(99999);
        return prefix + String.format("%05d", number);
    }
}
