package pl.rjuszczyk.panorama.viewer;

import android.util.Log;

public class MatrixCalculator {

    public static float[] to3dVector(float lat, float lon) {
        float temp = lat;
        lat = lon;
        lon = temp;

        lon-=90;
        lat+=90;
        lat = (float)(lat*Math.PI/180f);
        lon = (float)(lon*Math.PI/180f);
        float R = 20;
        float x = (float)(Math.sin(lon) * Math.cos(lat));
        float z = (float)(Math.sin(lon) * Math.sin(lat));
        float y = (float)(Math.cos(lon));
        x = x * R;
        z = z * R;
        y = y * R;
        return new float[]{x,y,z, 0};
    }

    public static float[] getPosOnScreen(float[] mvMatrix, float[] vector4) {
        //gl_Position = u_MVPMatrix * a_Position;
//		float[] vector4result = new float[4];
//		float[] m = new float[mvMatrix.length];
//		//Matrix.transposeM(m,0,mvMatrix,0);
//		m = mvMatrix;
//		Matrix.multiplyMV(vector4result, 0, m, 0, vector4, 0);

        float[] vector4result = aaa(mvMatrix, vector4);




        return vector4result;
    }


    public static float[] aaa(float[] matrix, float[] vec) {
        final float l_mat[] = matrix;
        float x = vec[0];
        float y = vec[1];
        float z = vec[2];
        final float l_w = 1f / (x * l_mat[M30] + y * l_mat[M31] + z * l_mat[M32] + l_mat[M33]);
        return new float[]{(x * l_mat[M00] + y * l_mat[M01] + z * l_mat[M02] + l_mat[M03]) * l_w, (x
                * l_mat[M10] + y * l_mat[M11] + z * l_mat[M12] + l_mat[M13])
                * l_w, (x * l_mat[M20] + y * l_mat[M21] + z * l_mat[M22] + l_mat[M23]) * l_w};
    }

    public static final int M00 = 0;
    /** XY: Typically the negative sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied
     * with the source Y component and added to the target X component. */
    public static final int M01 = 4;
    /** XZ: Typically the sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied with the
     * source Z component and added to the target X component. */
    public static final int M02 = 8;
    /** XW: Typically the translation of the X component. On Vector3 multiplication this value is added to the target X component. */
    public static final int M03 = 12;
    /** YX: Typically the sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied with the
     * source X component and added to the target Y component. */
    public static final int M10 = 1;
    /** YY: Typically the unrotated Y component for scaling, also the cosine of the angle when rotated on the X and/or Z axis. On
     * Vector3 multiplication this value is multiplied with the source Y component and added to the target Y component. */
    public static final int M11 = 5;
    /** YZ: Typically the negative sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied
     * with the source Z component and added to the target Y component. */
    public static final int M12 = 9;
    /** YW: Typically the translation of the Y component. On Vector3 multiplication this value is added to the target Y component. */
    public static final int M13 = 13;
    /** ZX: Typically the negative sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied
     * with the source X component and added to the target Z component. */
    public static final int M20 = 2;
    /** ZY: Typical the sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied with the
     * source Y component and added to the target Z component. */
    public static final int M21 = 6;
    /** ZZ: Typically the unrotated Z component for scaling, also the cosine of the angle when rotated on the X and/or Y axis. On
     * Vector3 multiplication this value is multiplied with the source Z component and added to the target Z component. */
    public static final int M22 = 10;
    /** ZW: Typically the translation of the Z component. On Vector3 multiplication this value is added to the target Z component. */
    public static final int M23 = 14;
    /** WX: Typically the value zero. On Vector3 multiplication this value is ignored. */
    public static final int M30 = 3;
    /** WY: Typically the value zero. On Vector3 multiplication this value is ignored. */
    public static final int M31 = 7;
    /** WZ: Typically the value zero. On Vector3 multiplication this value is ignored. */
    public static final int M32 = 11;
    /** WW: Typically the value one. On Vector3 multiplication this value is ignored. */
    public static final int M33 = 15;

    public static float[] calculateOnScreenPosNormalized(float[] mvpMatrix, double latitude, double longitude) {
        float[] vec = MatrixCalculator.to3dVector((float)latitude, (float)longitude);
        float[] mMVMatrix = mvpMatrix.clone();
        float[] pos = MatrixCalculator.getPosOnScreen(mvpMatrix, vec);
        final float[] p = MatrixCalculator.aaa(mMVMatrix, vec);
        if (p[2]-1 < 0) {
            return pos;
        } else {
            p[0] = 10;
            p[1] = 10;
            return p;
        }
    }
}
