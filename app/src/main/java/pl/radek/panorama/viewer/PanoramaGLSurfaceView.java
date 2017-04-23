package pl.radek.panorama.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import pl.radek.panorama.R;
import pl.radek.panorama.gyroscope.GyroscopeHandler;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */
public class PanoramaGLSurfaceView extends GLSurfaceView
{
	public static float MAX_X_ROT = 88f;
	public static float MIN_X_ROT = -88f;
	private final float TOUCH_SCALE = 0.2f;
	GestureDetector mGestureDetector;
	RawImageDrawer mImageDrawer;

	GyroscopeHandler gyroscopeHandler;
	GyroscopeHandler gyroscopeHandler2;
	private float mDefaultModelScale = 1f;
	private PanoramaRenderer mPanoramaRenderer;
	private float mScaleFactor = 1;
	private float xrot;                    //X Rotation
	private float yrot;                    //Y Rotation


	private ScaleGestureDetector mScaleGestureDetector;
	private boolean isGyroAvailable;
	private int currentGyroHandler = 1;
	private float[] currentRotationMatrix;
	private float[] currentRotationMatrix2;
	private float currentProgress = 0;
	private int targetProgress;
	private long lastTime = System.currentTimeMillis();

	public PanoramaGLSurfaceView(Context context) {
		super(context);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new RawImageDrawer(getResources());


		mPanoramaRenderer = new PanoramaRenderer(context, mImageDrawer, R.raw.sphere2, R.raw.pano1024);
		mPanoramaRenderer.setModelScale(mDefaultModelScale);

		setEGLContextClientVersion(2);
		setRenderer(mPanoramaRenderer);
	}

	public PanoramaGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new RawImageDrawer(getResources());


		mPanoramaRenderer = new PanoramaRenderer(context, mImageDrawer, R.raw.sphere2, R.raw.pano1024);
		mPanoramaRenderer.setModelScale(mDefaultModelScale);


		setEGLContextClientVersion(2);
		setRenderer(mPanoramaRenderer);
	}

	public boolean onTouchEvent(MotionEvent event) {
		boolean retVal = mScaleGestureDetector.onTouchEvent(event);
		retVal = mGestureDetector.onTouchEvent(event) || retVal;
		return retVal || super.onTouchEvent(event);
	}


	@Override
	protected void onDetachedFromWindow() {
		gyroscopeHandler.stop();
		gyroscopeHandler2.stop();
		super.onDetachedFromWindow();
	}
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(isInEditMode())
			return;

		gyroscopeHandler = new GyroscopeHandler();
		gyroscopeHandler2 = new GyroscopeHandler();
		currentGyroHandler = 1;

		isGyroAvailable = true;

		gyroscopeHandler.start(getContext(), new GyroscopeHandler.OnGyroscopeChanged() {


			@Override
			public void onGyroscopeChange(double x, double y, double z) {

			}


			@Override
			public void onGyroscopeChanged2(float[] currentRotationMatrix) {
				if(isGyroAvailable) {
					PanoramaGLSurfaceView.this.currentRotationMatrix = currentRotationMatrix;
					if(currentGyroHandler == 2) {
						float deltaTime = System.currentTimeMillis() - lastTime;
						lastTime = System.currentTimeMillis();
						currentProgress = lerp(0.01f, currentProgress, targetProgress);
						if(currentProgress<0.01 && targetProgress==0)currentProgress = 0;
						if(currentProgress>0.99 && targetProgress==1)currentProgress = 1;
						Log.d("gyro1", "onGyroscopeChanged2: currentProgress = " +currentProgress);
					} else {
						if(PanoramaGLSurfaceView.this.currentRotationMatrix == null ||
								PanoramaGLSurfaceView.this.currentRotationMatrix2 == null) {
							return;
						}

						float[] rotationMatrix = getCurrentRotationMatrix(
								PanoramaGLSurfaceView.this.currentRotationMatrix,
								PanoramaGLSurfaceView.this.currentRotationMatrix2,
								currentProgress
						);
						mPanoramaRenderer.setModelRotationMatrix(rotationMatrix);
					}
				}
			}

			@Override
			public void onGyroscopeNotAvailable() {
				isGyroAvailable = false;
			}
		});

		gyroscopeHandler2.start(getContext(), new GyroscopeHandler.OnGyroscopeChanged() {


			@Override
			public void onGyroscopeChange(double x, double y, double z) {

			}

			@Override
			public void onGyroscopeChanged2(float[] currentRotationMatrix) {
				if(isGyroAvailable) {
					PanoramaGLSurfaceView.this.currentRotationMatrix2 = currentRotationMatrix;
					if(currentGyroHandler == 1) {
						float deltaTime = System.currentTimeMillis() - lastTime;
						lastTime = System.currentTimeMillis();
						currentProgress = lerp(0.01f, currentProgress, targetProgress);
						Log.d("gyro2", "onGyroscopeChanged2: currentProgress = " +currentProgress);
						if(currentProgress<0.01 && targetProgress==0)currentProgress = 0;
						if(currentProgress>0.99 && targetProgress==1)currentProgress = 1;

					} else {
						if(PanoramaGLSurfaceView.this.currentRotationMatrix == null ||
								PanoramaGLSurfaceView.this.currentRotationMatrix2 == null) {
							return;
						}

						float[] rotationMatrix = getCurrentRotationMatrix(
								PanoramaGLSurfaceView.this.currentRotationMatrix,
								PanoramaGLSurfaceView.this.currentRotationMatrix2,
								currentProgress
						);
						mPanoramaRenderer.setModelRotationMatrix(rotationMatrix);
					}
				}
			}

			@Override
			public void onGyroscopeNotAvailable() {

			}
		});
	}
	float[] getCurrentRotationMatrix(
		float[] matrix1,
		float[] matrix2,
		float progress
	) {
		float[] matrixResult = new float[matrix1.length];
		for (int i = 0; i < matrix1.length; i++) {
			matrixResult[i] = lerp(progress, matrix1[i], matrix2[i]);
		}

		return matrixResult;
	}

	float lerp(float t, float a, float b) {
		return (1 - t) * a + t * b;
	}

	public void reset() {
		if(currentGyroHandler==1) {
			gyroscopeHandler.reset();
			gyroscopeHandler.restart();
			currentGyroHandler = 2;
			targetProgress = 0;
//			currentProgress = 1;
		} else {
			gyroscopeHandler2.reset();
			gyroscopeHandler2.restart();
			currentGyroHandler = 1;
			targetProgress = 1;
//			currentProgress = 0;
		}

		mPanoramaRenderer.resetCamera();
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor = mPanoramaRenderer.getModelScale() / detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.30f, Math.min(mScaleFactor, 1.5f));
			//     mPanoramaRenderer.mCameraZ *= mScaleFactor;
			mPanoramaRenderer.setModelScale(mScaleFactor);
			invalidate();
			return true;
		}
	}

	private class MyOnGestureListener implements GestureDetector.OnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			if (mPanoramaRenderer.useGyro) {
//				return true;
//			}

			xrot = mPanoramaRenderer.getModelRotationX() - distanceY * TOUCH_SCALE;
			xrot = Math.max(Math.min(xrot, MAX_X_ROT), MIN_X_ROT);
			yrot = mPanoramaRenderer.getModelRotationY() - distanceX * TOUCH_SCALE;

			mPanoramaRenderer.setModelRotationX(xrot);
			mPanoramaRenderer.setModelRotationY(yrot);

			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {

			mPanoramaRenderer.resetModelScale(mDefaultModelScale);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (isGyroAvailable) {
				return true;
			}

			mPanoramaRenderer.setVelocities(velocityX, velocityY);

			return false;
		}
	}

	public void setTex_resourceID(int tex_resourceID) {
		mPanoramaRenderer.setTex_resourceID(tex_resourceID);
	}

	public void setTex_bitmap(Bitmap bitmap) {
		mPanoramaRenderer.setTextureBitmap(bitmap);
	}
}