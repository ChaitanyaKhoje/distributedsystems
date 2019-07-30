import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utility {

    private static String hashingAlgorithm = "SHA-256";

    public static String calculateHashValue(String input) {

        StringBuffer stringBuffer = new StringBuffer();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(getHashingAlgorithm());
            byte [] hash = messageDigest.digest(input.getBytes());

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) stringBuffer.append('0');
                stringBuffer.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return stringBuffer.toString();
    }

    public boolean isElementOf(String key, String node1, String node2, boolean flag) {

        if(node1.compareTo(node2) < 0) {
            if(flag) {
                //return left < key and key <= right
                  return (key.compareTo(node1) > 0 && key.compareTo(node2) <= 0);
            }
            //return left < key and key < right
            return (key.compareTo(node1) > 0 && key.compareTo(node2) < 0);
        } else {
            if (flag) {
                return (key.compareTo(node1) > 0 || key.compareTo(node2) <= 0);
            }
            return (key.compareTo(node1) > 0 || key.compareTo(node2) < 0);
        }
    }

    public static String getHashingAlgorithm() {
        return hashingAlgorithm;
    }
}
