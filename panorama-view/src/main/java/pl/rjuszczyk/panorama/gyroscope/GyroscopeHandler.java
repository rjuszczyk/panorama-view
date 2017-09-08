package pl.rjuszczyk.panorama.gyroscope;


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

public class GyroscopeHandler implements SensorEventListener,
		FusedGyroscopeSensorListener {

	public static final float EPSILON = 0.000000001f;

	private static final String tag = GyroscopeHandler.class.getSimpleName();
	private static final float NS2S = 1.0f / 1000000000.0f;
	private static final int MEAN_FILTER_WINDOW = 10;
	private static final int MIN_SAMPLE_COUNT = 30;
	public Context mContext;
	OnGyroscopeChanged mOnGyroscopeChanged;
	private boolean hasInitialOrientation = false;
	private boolean stateInitializedCalibrated = false;
	private boolean stateInitializedRaw = false;

	// The gauge views. Note that these are views and UI hogs since they run in
	// the UI thread, not ideal, but easy to use.
	private boolean useFusedEstimation = false;
	private boolean useRadianUnits = false;
	private DecimalFormat df;
	// Calibrated maths.
	private float[] currentRotationMatrixCalibrated;
	private float[] deltaRotationMatrixCalibrated;
	private float[] deltaRotationVectorCalibrated;
	private float[] gyroscopeOrientationCalibrated;
	// Uncalibrated maths
	private float[] currentRotationMatrixRaw;
	private float[] deltaRotationMatrixRaw;
	private float[] deltaRotationVectorRaw;
	private float[] gyroscopeOrientationRaw;
	// accelerometer and magnetometer based rotation matrix
	private float[] initialRotationMatrix;
	// accelerometer vector
	private float[] acceleration;
	// magnetic field vector
	private float[] magnetic;
	private FusedGyroscopeSensor fusedGyroscopeSensor;
	private int accelerationSampleCount = 0;
	private int magneticSampleCount = 0;
	private long timestampOldCalibrated = 0;
	private long timestampOldRaw = 0;
	private MeanFilter accelerationFilter;
	private MeanFilter magneticFilter;
	// We need the SensorManager to register for Sensor Events.
	private SensorManager sensorManager;

	public void start(Context context, OnGyroscopeChanged onGyroscopeChanged) {
		mOnGyroscopeChanged = onGyroscopeChanged;
		mContext = context;
		initUI();
		initMaths();
		initSensors();
		initFilters();
		restart();
	}

	public void stop() {
		reset();
		mContext = null;
	}

	@Override

	public void onSensorChanged(SensorEvent event) {

		FusedGyroscopeSensor.changeArray(mContext, event.values);
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			onAccelerationSensorChanged(event.values, event.timestamp);
		}

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			onMagneticSensorChanged(event.values, event.timestamp);
		}

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			onGyroscopeSensorChanged(event.values, event.timestamp);
		}

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
			onGyroscopeSensorUncalibratedChanged(event.values, event.timestamp);
		}
	}

	@Override
	public void onAngularVelocitySensorChanged(float[] angularVelocity,
											   long timeStamp) {

	}


	public static float[] transposeVector(float[] a) {

		if(a.length==4) {
			return new float[]{a[2], a[0] , a[1], a[3]};
		}
		return new float[]{a[2], a[0] , a[1]};

	}
	public void onAccelerationSensorChanged(float[] acceleration, long timeStamp) {

		acceleration = transposeVector(acceleration);

		// Get a local copy of the raw magnetic values from the device sensor.
		System.arraycopy(acceleration, 0, this.acceleration, 0, acceleration.length);

		// Use a mean filter to smooth the sensor inputs
		this.acceleration = accelerationFilter.filterFloat(this.acceleration);

		// Count the number of samples received.
		accelerationSampleCount++;

		// Only determine the initial orientation after the acceleration sensor
		// and magnetic sensor have had enough time to be smoothed by the mean
		// filters. Also, only do this if the orientation hasn'transposeVector already been
		// determined since we only need it once.
		if (accelerationSampleCount > MIN_SAMPLE_COUNT
				&& magneticSampleCount > MIN_SAMPLE_COUNT
				&& !hasInitialOrientation) {
			calculateOrientation();
		}
	}

	public void onGyroscopeSensorChanged(float[] gyroscope, long timestamp) {

		gyroscope = transposeVector(gyroscope);

		// don'transposeVector start until first accelerometer/magnetometer orientation has
		// been acquired
		if (!hasInitialOrientation) {
			return;
		}

		// Initialization of the gyroscope based rotation matrix
		if (!stateInitializedCalibrated) {
			currentRotationMatrixCalibrated = matrixMultiplication(
					currentRotationMatrixCalibrated, initialRotationMatrix);

			stateInitializedCalibrated = true;
		}

		// This timestep's delta rotation to be multiplied by the current
		// rotation after computing it from the gyro sample data.
		if (timestampOldCalibrated != 0 && stateInitializedCalibrated) {
			final float dT = (timestamp - timestampOldCalibrated) * NS2S;

			// Axis of the rotation sample, not normalized yet.
			float axisX = gyroscope[0];
			float axisY = gyroscope[1];
			float axisZ = gyroscope[2];

			// Calculate the angular speed of the sample
			float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

			// Normalize the rotation vector if it's big enough to get the axis
			if (omegaMagnitude > EPSILON) {
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the
			// timestep. We will convert this axis-angle representation of the
			// delta rotation into a quaternion before turning it into the
			// rotation matrix.
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;

			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

			deltaRotationVectorCalibrated[0] = sinThetaOverTwo * axisX;
			deltaRotationVectorCalibrated[1] = sinThetaOverTwo * axisY;
			deltaRotationVectorCalibrated[2] = sinThetaOverTwo * axisZ;
			deltaRotationVectorCalibrated[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(deltaRotationMatrixCalibrated, deltaRotationVectorCalibrated);

			currentRotationMatrixCalibrated = matrixMultiplication(
					currentRotationMatrixCalibrated,
					deltaRotationMatrixCalibrated);

			SensorManager.getOrientation(currentRotationMatrixCalibrated, gyroscopeOrientationCalibrated);
		}

		timestampOldCalibrated = timestamp;


		if (useRadianUnits) {
			Log.d("sensor", "x= " + (df.format(gyroscopeOrientationCalibrated[0])) + "	y= " + (df.format(gyroscopeOrientationCalibrated[1])) + "	z= " + (df.format(gyroscopeOrientationCalibrated[2])));
		} else {
			// MyLog.d("sensor", "x= " + (df.format(Math.toDegrees(gyroscopeOrientationCalibrated[0])))+"	y= " + (df.format(Math.toDegrees(gyroscopeOrientationCalibrated[1])))+"	z= " + (df.format(Math.toDegrees(gyroscopeOrientationCalibrated[2]))));
			if (mOnGyroscopeChanged != null) {
				mOnGyroscopeChanged.onGyroscopeChanged2(currentRotationMatrixCalibrated);
				mOnGyroscopeChanged.onGyroscopeChange(Math.toDegrees(gyroscopeOrientationCalibrated[0]), Math.toDegrees(gyroscopeOrientationCalibrated[1]), Math.toDegrees(gyroscopeOrientationCalibrated[2]));
			}

		}
	}

	public void onGyroscopeSensorUncalibratedChanged(float[] gyroscope,
													 long timestamp) {
		gyroscope = transposeVector(gyroscope);
		// don'transposeVector start until first accelerometer/magnetometer orientation has
		// been acquired
		if (!hasInitialOrientation) {
			return;
		}

		// Initialization of the gyroscope based rotation matrix
		if (!stateInitializedRaw) {
			currentRotationMatrixRaw = matrixMultiplication(
					currentRotationMatrixRaw, initialRotationMatrix);

			stateInitializedRaw = true;

		}

		// This timestep's delta rotation to be multiplied by the current
		// rotation after computing it from the gyro sample data.
		if (timestampOldRaw != 0 && stateInitializedRaw) {
			final float dT = (timestamp - timestampOldRaw) * NS2S;

			// Axis of the rotation sample, not normalized yet.
			float axisX = gyroscope[0];
			float axisY = gyroscope[1];
			float axisZ = gyroscope[2];

			// Calculate the angular speed of the sample
			float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

			// Normalize the rotation vector if it's big enough to get the axis
			if (omegaMagnitude > EPSILON) {
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the
			// timestep. We will convert this axis-angle representation of the
			// delta rotation into a quaternion before turning it into the
			// rotation matrix.
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;

			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

			deltaRotationVectorRaw[0] = sinThetaOverTwo * axisX;
			deltaRotationVectorRaw[1] = sinThetaOverTwo * axisY;
			deltaRotationVectorRaw[2] = sinThetaOverTwo * axisZ;
			deltaRotationVectorRaw[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(deltaRotationMatrixRaw, deltaRotationVectorRaw);

			currentRotationMatrixRaw = matrixMultiplication(
					currentRotationMatrixRaw, deltaRotationMatrixRaw);

			SensorManager.getOrientation(currentRotationMatrixRaw, gyroscopeOrientationRaw);
		}

		timestampOldRaw = timestamp;


	}

	public void onMagneticSensorChanged(float[] magnetic, long timeStamp) {
		// Get a local copy of the raw magnetic values from the device sensor.

		magnetic=transposeVector(magnetic);

		// Get a local copy of the raw magnetic values from the device sensor.


		System.arraycopy(magnetic, 0, this.magnetic, 0, magnetic.length);

		// Use a mean filter to smooth the sensor inputs
		this.magnetic = magneticFilter.filterFloat(this.magnetic);

		// Count the number of samples received.
		magneticSampleCount++;
	}

	/**
	 * Calculates orientation angles from accelerometer and magnetometer output.
	 * Note that we only use this *once* at the beginning to orient the
	 * gyroscope to earth frame. If you do not call this, the gyroscope will
	 * orient itself to whatever the relative orientation the device is in at
	 * the time of initialization.
	 */
	private void calculateOrientation() {
		hasInitialOrientation = SensorManager.getRotationMatrix(initialRotationMatrix, null, acceleration, magnetic);

		// Remove the sensor observers since they are no longer required.
		if (hasInitialOrientation) {
			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
		}
	}

	/**
	 * Initialize the mean filters.
	 */
	private void initFilters() {
		accelerationFilter = new MeanFilter();
		accelerationFilter.setWindowSize(MEAN_FILTER_WINDOW);

		magneticFilter = new MeanFilter();
		magneticFilter.setWindowSize(MEAN_FILTER_WINDOW);
	}

	/**
	 * Initialize the data structures required for the maths.
	 */
	private void initMaths() {
		acceleration = new float[3];
		magnetic = new float[3];

		initialRotationMatrix = new float[9];

		deltaRotationVectorCalibrated = new float[4];
		deltaRotationMatrixCalibrated = new float[9];
		currentRotationMatrixCalibrated = new float[9];
		gyroscopeOrientationCalibrated = new float[3];

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrixCalibrated[0] = 1.0f;
		currentRotationMatrixCalibrated[4] = 1.0f;
		currentRotationMatrixCalibrated[8] = 1.0f;

		deltaRotationVectorRaw = new float[4];
		deltaRotationMatrixRaw = new float[9];
		currentRotationMatrixRaw = new float[9];
		gyroscopeOrientationRaw = new float[3];

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrixRaw[0] = 1.0f;
		currentRotationMatrixRaw[4] = 1.0f;
		currentRotationMatrixRaw[8] = 1.0f;
	}

	/**
	 * Initialize the sensors.
	 */
	private void initSensors() {
		sensorManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);

		fusedGyroscopeSensor = new FusedGyroscopeSensor(mContext);
	}

	/**
	 * Initialize the UI.
	 */
	private void initUI() {
		// Get a decimal formatter for the text views
		df = new DecimalFormat("#.##");
	}

	/**
	 * Multiply matrix a by b. Android gives us matrices results in
	 * one-dimensional arrays instead of two, so instead of using some (O)2 to
	 * transfer to a two-dimensional array and then an (O)3 algorithm to
	 * multiply, we just use a static linear time method.
	 *
	 * @param a
	 * @param b
	 * @return a*b
	 */
	private float[] matrixMultiplication(float[] a, float[] b) {
		float[] result = new float[9];

		result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
		result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
		result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

		result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
		result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
		result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

		result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
		result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
		result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

		return result;
	}

	/**
	 * Restarts all of the sensor observers and resets the activity to the
	 * initial state. This should only be called *after* a call to reset().
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void restart() {
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);

		// Do not register for gyroscope updates if we are going to use the
		// fused version of the sensor...
		if (!useFusedEstimation) {
			boolean enabled = sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_FASTEST);

			if (!enabled) {
				mOnGyroscopeChanged.onGyroscopeNotAvailable();
				//showGyroscopeNotAvailableAlert();
			}
		}

		if (Utils.hasKitKat()) {
			sensorManager.registerListener(this, sensorManager
							.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED),
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		// If we want to use the fused version of the gyroscope sensor.
		if (useFusedEstimation) {
			boolean hasGravity = sensorManager.registerListener(
					fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
					SensorManager.SENSOR_DELAY_FASTEST);

			// If for some reason the gravity sensor does not exist, fall back
			// onto the acceleration sensor.
			if (!hasGravity) {
				sensorManager.registerListener(fusedGyroscopeSensor,
						sensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			sensorManager.registerListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					SensorManager.SENSOR_DELAY_FASTEST);

			boolean enabled = sensorManager.registerListener(
					fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_FASTEST);

			if (!enabled) {
				mOnGyroscopeChanged.onGyroscopeNotAvailable();
			}


			fusedGyroscopeSensor.registerObserver(this);
		} else {

		}
	}

	/**
	 * Removes all of the sensor observers and resets the activity to the
	 * initial state.
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void reset() {

		hasInitialOrientation = false;

		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

		if (!useFusedEstimation) {
			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		}

		if (Utils.hasKitKat()) {
			sensorManager.unregisterListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED));
		}

		if (useFusedEstimation) {
			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));

			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

			fusedGyroscopeSensor.removeObserver(this);
		}

		initMaths();

		accelerationSampleCount = 0;
		magneticSampleCount = 0;

		hasInitialOrientation = false;
		stateInitializedCalibrated = false;
		stateInitializedRaw = false;
	}

	private void showGyroscopeNotAvailableAlert() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

		// set title
		alertDialogBuilder.setTitle("Gyroscope Not Available");

		// set dialog message
		alertDialogBuilder
				.setMessage(
						"Your device is not equipped with a gyroscope or it is not responding...")
				.setCancelable(false)
				.setNegativeButton("I'll look around...",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// if this button is clicked, just close
								// the dialog box and do nothing
								dialog.cancel();
							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public interface OnGyroscopeChanged {
		void onGyroscopeChange(double x, double y, double z);
		void onGyroscopeChanged2(float[] currentRotationMatrix);
		void onGyroscopeNotAvailable();
	}
}