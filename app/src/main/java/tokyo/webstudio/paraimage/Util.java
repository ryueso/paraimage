package tokyo.webstudio.paraimage;

import android.util.Log;

public class Util {

    static void log(Object msg) {
        Log.d("MyDebug", msg.toString());
    }

    public static String bin2hex(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();

        for (byte aByte : bytes) {
            buffer.append(String.format("%02x", aByte));
        }

        return buffer.toString();
    }
}
