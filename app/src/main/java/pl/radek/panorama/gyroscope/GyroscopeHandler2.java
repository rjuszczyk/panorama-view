package pl.radek.panorama.gyroscope;

/**
 * Created by Radek on 17.05.2016.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Timer;
import java.util.TimerTask;



import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class GyroscopeHandler2 implements SensorEventListener {
    private SensorManager mSensorManager = null;

    // angular speeds from gyro
    private float[] gyro = new float[3];

    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];

    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];

    // magnetic field vector
    private float[] magnet = null;//new float[3];

    // accelerometer vector
    private float[] accel = null;//new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = null;

    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];


    private Timer fuseTimer = new Timer();

    OnGyroscopeChanged mOnGyroscopeChanged;

    public void start(Context context, OnGyroscopeChanged onGyroscopeChanged) {

        mOnGyroscopeChanged = onGyroscopeChanged;

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initListeners();

//        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
//                1000, TIME_CONSTANT);

    }



    public void initListeners(){
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array
                // then calculate new orientation
                if(accel==null) {
                    accel = new float[3];
                }
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                calculateNewGyroLocation();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                if(magnet==null) {
                    magnet = new float[3];
                }
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void calculateAccMagOrientation() {
        if(magnet==null) return;
        if(accel==null) return;

        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            if (accMagOrientation == null) {
                accMagOrientation = new float[3];
            }

            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
           // float[] test = new float[3];
           // SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    public static final float EPSILON = 0.000000001f;

    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    float[] lastGyroOrientation = null;
    public static final int TIME_CONSTANT = 20;
    final static float FILTER_COEFFICIENT = 0.98f;
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            if(2==2)return;

            if(mOnGyroscopeChanged==null) return;
            if(accMagOrientation==null)return;

            if(lastGyroOrientation==null) {
                lastGyroOrientation = new float[3];
                lastGyroOrientation[0] = gyroOrientation[0];
                lastGyroOrientation[1] = gyroOrientation[1];
                lastGyroOrientation[2] = gyroOrientation[2];
                return;
            }

			float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
			fusedOrientation[0] =
					FILTER_COEFFICIENT * gyroOrientation[0]
							+ oneMinusCoeff * lastGyroOrientation[0];

			fusedOrientation[1] =
					FILTER_COEFFICIENT * gyroOrientation[1]
							+ oneMinusCoeff * lastGyroOrientation[1];

			fusedOrientation[2] =
					FILTER_COEFFICIENT * gyroOrientation[2]
							+ oneMinusCoeff * lastGyroOrientation[2];

			// overwrite gyro matrix and orientation with fused orientation
			// to comensate gyro drift
			gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
			System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

            //gyroMatrix = getRotationMatrixFromOrientation(gyroOrientation);
            mOnGyroscopeChanged.onGyroscopeChanged(gyroMatrix);

            lastGyroOrientation[0] = gyroOrientation[0];
            lastGyroOrientation[1] = gyroOrientation[1];
            lastGyroOrientation[2] = gyroOrientation[2];
        }
    }

    void calculateNewGyroLocation() {
        if(mOnGyroscopeChanged==null) return;
        if(accMagOrientation==null)return;

        if(lastGyroOrientation==null) {
            lastGyroOrientation = new float[3];
            lastGyroOrientation[0] = gyroOrientation[0];
            lastGyroOrientation[1] = gyroOrientation[1];
            lastGyroOrientation[2] = gyroOrientation[2];
            return;
        }

        float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
        fusedOrientation[0] =
                FILTER_COEFFICIENT * gyroOrientation[0]
                        + oneMinusCoeff * lastGyroOrientation[0];

        fusedOrientation[1] =
                FILTER_COEFFICIENT * gyroOrientation[1]
                        + oneMinusCoeff * lastGyroOrientation[1];

        fusedOrientation[2] =
                FILTER_COEFFICIENT * gyroOrientation[2]
                        + oneMinusCoeff * lastGyroOrientation[2];

        // overwrite gyro matrix and orientation with fused orientation
        // to comensate gyro drift
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

        //gyroMatrix = getRotationMatrixFromOrientation(gyroOrientation);
        mOnGyroscopeChanged.onGyroscopeChanged(gyroMatrix);

        lastGyroOrientation[0] = gyroOrientation[0];
        lastGyroOrientation[1] = gyroOrientation[1];
        lastGyroOrientation[2] = gyroOrientation[2];
    }

    public void stop(){
       // fuseTimer.cancel();
        fuseTimer = null;
        mOnGyroscopeChanged = null;
    }

    public interface OnGyroscopeChanged {
        void onGyroscopeChanged(float[] currentRotationMatrix);;
        void onGyroscopeNotAvailable();
    }
}