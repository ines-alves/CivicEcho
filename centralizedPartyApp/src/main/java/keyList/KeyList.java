package keyList;

import java.util.Dictionary;
import java.util.Hashtable;

public class KeyList {
    private static final Dictionary<String, String> publicKeysList = new Hashtable<>();

    public void addCitizenPublicKey(String citizenID, String publicKey) {
        publicKeysList.put(citizenID, publicKey);
    }

    public String getCitizenPublicKey(String citizenID) {
        return publicKeysList.get(citizenID);
    }
}
