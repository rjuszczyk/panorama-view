package pl.rjuszczyk.panorama;

import android.content.Context;
import android.opengl.Matrix;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        float[] re = to3dVector(0, 0);
        assertTrue(""+re[0]+" , " + re[1] + " , " + re[2], false);
    }
    public void print(float[] re) {
        System.out.println(""+re[0]+" , " + re[1] + " , " + re[2]);
        Log.d("test", ""+re[0]+" , " + re[1] + " , " + re[2]);
    }

    public float[] to3dVector(float lat, float lon) {
        lon-=90;
        lat-=90;
        lat = (float)(lat*Math.PI/180f);
        lon = (float)(lon*Math.PI/180f);
        float R = 10;
//        float temp = lat;
//        float latit = lon;
//        float longit = temp;
        float x = (float)(Math.sin(lon) * Math.cos(lat));
        float z = (float)(Math.sin(lon) * Math.sin(lat));
        float y = (float)(Math.cos(lon));
        x = x * R;
        z = z * R;
        y = y * R;
        return new float[]{x,y,z};
    }
}
