package pl.rjuszczyk.panorama.viewer;

import android.util.Log;

public class MyLog {

    static boolean showLog = false;

    public static void d(String gyro2, String s) {
        if(showLog) {
            Log.d(gyro2, s);
        }
    }


    public static void e(String gyro2, String s) {
        if(showLog) {
            Log.e(gyro2, s);
        }
    }


    public static void i(String gyro2, String s) {
        if(showLog) {
            Log.i(gyro2, s);
        }
    }
}
