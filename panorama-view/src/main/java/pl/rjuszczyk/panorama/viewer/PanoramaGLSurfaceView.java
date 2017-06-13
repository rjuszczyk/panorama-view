package pl.rjuszczyk.panorama.viewer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.Timer;
import java.util.TimerTask;

import pl.rjuszczyk.panorama.R;
import pl.rjuszczyk.panorama.multitouch.MoveGestureDetector;
import pl.rjuszczyk.panorama.multitouch.RotateGestureDetector;

import pl.rjuszczyk.panorama.gyroscope.GyroscopeHandler;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */
public class PanoramaGLSurfaceView extends GLSurfaceView
{
	public static float MAX_X_ROT = 88f;
	public static float MIN_X_ROT = -88f;
	private float DEFAULT_TOUCH_SCALE = 0.2f;
	private float TOUCH_SCALE = 0.2f;
	//GestureDetector mGestureDetector;
	MoveGestureDetector mMoveDetector;
	RawImageDrawer mImageDrawer;
	RotateGestureDetector mRotateGestureDetector;
	GestureDetector mFlingDetector;

	GyroscopeHandler gyroscopeHandler;
	GyroscopeHandler gyroscopeHandler2;
	private float mDefaultModelScale = 1f;
	private PanoramaRenderer mPanoramaRenderer;
	private float mScaleFactor = 1;
	private float xrot;                    //X Rotation
	private float yrot;                    //Y Rotation
	int beginEvents = 0;

	private ScaleGestureDetector mScaleGestureDetector;
	private boolean isGyroAvailable;
	private int currentGyroHandler = 1;
	private float[] currentRotationMatrix;
	private float[] currentRotationMatrix2;
	private float currentProgress = 0;
	private int targetProgress;
	private long lastTime = System.currentTimeMillis();
	boolean autoCorrection = false;
	private Timer timer;

	public PanoramaGLSurfaceView(Context context) {
		super(context);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		//mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mMoveDetector = new MoveGestureDetector(context, new MoveListener());
		mRotateGestureDetector = new RotateGestureDetector(context, new MyRotateGestureDetector());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new RawImageDrawer(getResources());


		mPanoramaRenderer = new PanoramaRenderer(context, mImageDrawer, R.raw.sphere);
		mPanoramaRenderer.setModelScale(mDefaultModelScale);

		setEGLContextClientVersion(2);
		super.setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
		setRenderer(mPanoramaRenderer);
	}

	public PanoramaGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if(isInEditMode())
			return;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PanoramaGLSurfaceView, 0, 0);

		int imageResource = a.getResourceId(R.styleable.PanoramaGLSurfaceView_img, -1);

		a.recycle();

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		//mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mMoveDetector = new MoveGestureDetector(context, new MoveListener());
		mRotateGestureDetector = new RotateGestureDetector(context, new MyRotateGestureDetector());
		mFlingDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

				mPanoramaRenderer.speedX= -TOUCH_SCALE*velocityX;
				mPanoramaRenderer.speedY= -TOUCH_SCALE*velocityY;
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {

			}

			@Override
			public boolean onDown(MotionEvent motionEvent) {
				return false;
			}

			@Override
			public void onShowPress(MotionEvent motionEvent) {

			}

			@Override
			public boolean onSingleTapUp(MotionEvent event) {

				return false;
			}

			@Override
			public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
				return false;
			}
		});
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new RawImageDrawer(getResources());

		mPanoramaRenderer = new PanoramaRenderer(context, mImageDrawer, R.raw.sphere);
		mPanoramaRenderer.setModelScale(mDefaultModelScale);


		setEGLContextClientVersion(2);
		super.setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
		setRenderer(mPanoramaRenderer);

		if(imageResource!=-1) {
			setTexDrawableResourceID(imageResource);
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		boolean retVal = mRotateGestureDetector.onTouchEvent(event);
		retVal = mScaleGestureDetector.onTouchEvent(event) || retVal;

		retVal = mMoveDetector.onTouchEvent(event) || retVal;
		retVal = mFlingDetector.onTouchEvent(event) || retVal;
//		retVal = mGestureDetector.onTouchEvent(event) || retVal;
		return retVal || super.onTouchEvent(event);
	}

	@Override
	protected void onDetachedFromWindow() {
		gyroscopeHandler.stop();
		gyroscopeHandler2.stop();
		if(autoCorrection) {
			timer.cancel();
		}
		super.onDetachedFromWindow();
	}
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(isInEditMode())
			return;

		gyroscopeHandler = new GyroscopeHandler();
		gyroscopeHandler2 = new GyroscopeHandler();
		currentGyroHandler = 2;

		isGyroAvailable = true;

		gyroscopeHandler.start(getContext(), new GyroscopeHandler.OnGyroscopeChanged() {


			@Override
			public void onGyroscopeChange(double x, double y, double z) {

			}


			@Override
			public void onGyroscopeChanged2(float[] currentRotationMatrix) {
				if (isGyroAvailable) {
					PanoramaGLSurfaceView.this.currentRotationMatrix = currentRotationMatrix;
					if (currentGyroHandler == 2) {
						float deltaTime = System.currentTimeMillis() - lastTime;
						lastTime = System.currentTimeMillis();
						currentProgress = lerp(0.01f, currentProgress, targetProgress);
						if (currentProgress < 0.01 && targetProgress == 0) currentProgress = 0;
						if (currentProgress > 0.99 && targetProgress == 1) currentProgress = 1;
						Log.d("gyro1", "onGyroscopeChanged2: currentProgress = " + currentProgress);
					} else {
						if (PanoramaGLSurfaceView.this.currentRotationMatrix == null ||
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
				if (isGyroAvailable) {
					PanoramaGLSurfaceView.this.currentRotationMatrix2 = currentRotationMatrix;
					if (currentGyroHandler == 1) {
						float deltaTime = System.currentTimeMillis() - lastTime;
						lastTime = System.currentTimeMillis();
						currentProgress = lerp(0.01f, currentProgress, targetProgress);
						Log.d("gyro2", "onGyroscopeChanged2: currentProgress = " + currentProgress);
						if (currentProgress < 0.01 && targetProgress == 0) currentProgress = 0;
						if (currentProgress > 0.99 && targetProgress == 1) currentProgress = 1;

					} else {
						if (PanoramaGLSurfaceView.this.currentRotationMatrix == null ||
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

		if(autoCorrection) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					reset();
				}
			}, 0, 10000);
		}
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
		Log.d("Panorama", "reset() called");
		if(currentGyroHandler==1) {
			gyroscopeHandler.reset();
			gyroscopeHandler.restart();
			currentGyroHandler = 2;
			targetProgress = 0;
		} else {
			gyroscopeHandler2.reset();
			gyroscopeHandler2.restart();
			currentGyroHandler = 1;
			targetProgress = 1;
		}

//		mPanoramaRenderer.resetCamera();
	}




	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor = mPanoramaRenderer.getModelScale() / detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.30f, Math.min(mScaleFactor, 1.5f));
			TOUCH_SCALE = DEFAULT_TOUCH_SCALE * mScaleFactor;
 			//     mPanoramaRenderer.mCameraZ *= mScaleFactor;
			mPanoramaRenderer.setModelScale(mScaleFactor);
			invalidate();
			return true;
		}
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			beginEvents++;
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			beginEvents--;
			super.onScaleEnd(detector);

		}
	}

	private class MyRotateGestureDetector implements RotateGestureDetector.OnRotateGestureListener {

		@Override
		public boolean onRotate(RotateGestureDetector detector) {
			mPanoramaRenderer.rotateZ(detector.getRotationDegreesDelta());
			return true;
		}

		@Override
		public boolean onRotateBegin(RotateGestureDetector detector) {
			beginEvents++;
			return true;
		}

		@Override
		public void onRotateEnd(RotateGestureDetector detector) {
			beginEvents--;
		}
	}



	public void setPanoramaResourceId(int tex_resourceID) {
		mPanoramaRenderer.setTex_resourceID(tex_resourceID);
	}

	public void setTexDrawableResourceID(int tex_resourceID) {
		mPanoramaRenderer.setTex_resourceID(tex_resourceID);
	}

	public void setPanoramaBitmap(Bitmap bitmap) {
		mPanoramaRenderer.setTextureBitmap(bitmap);
	}

	private class MoveListener implements MoveGestureDetector.OnMoveGestureListener {
		@Override
		public boolean onMove(MoveGestureDetector detector) {
			if (beginEvents != 0) {
				return true;
			}
			float distanceX = detector.getFocusDelta().x;
			float distanceY = detector.getFocusDelta().y;

			mPanoramaRenderer.rotate(-distanceX*TOUCH_SCALE, -distanceY*TOUCH_SCALE);
			return true;
		}

		@Override
		public boolean onMoveBegin(MoveGestureDetector detector) {
			return true;
		}

		@Override
		public void onMoveEnd(MoveGestureDetector detector) {

		}
	}
}