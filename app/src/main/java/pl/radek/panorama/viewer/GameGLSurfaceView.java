package pl.radek.panorama.viewer;

import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import pl.radek.panorama.R;
import pl.radek.panorama.gyroscope.GyroscopeHandler;
import pl.radek.panorama.gyroscope.GyroscopeHandler2;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */
public class GameGLSurfaceView extends GLSurfaceView
{

	private static final float NS2S = 1.0f / 1000000000.0f;
	public static float MAX_X_ROT = 88f;
	public static float MIN_X_ROT = -88f;
	private final float TOUCH_SCALE = 0.2f;
	private final float[] deltaRotationVector = new float[4];
	GestureDetector mGestureDetector;
	Renderer mRenderer;
	ImageDrawer mImageDrawer;
	float rotX = 0;
	float rotY = 0;
	float rotZ = 0;
	GyroscopeHandler2 gyroscopeHandler;
	private float mDefaultModelScale = 1f;
	private GameRenderer mGameRenderer;
	private float mScaleFactor = 1;
	private float xrot;                    //X Rotation
	private float yrot;                    //Y Rotation
	private float timestamp;

	private ScaleGestureDetector mScaleGestureDetector;
	public GameGLSurfaceView(Context context) {
		super(context);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new ImageDrawer(getResources());


		mRenderer = new GameRenderer(context, mImageDrawer, R.raw.sphere2, R.raw.pano1024);


		setEGLContextClientVersion(2);
		setRenderer(mRenderer);
	}

	public GameGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new ImageDrawer(getResources());


		mRenderer = new GameRenderer(context, mImageDrawer, R.raw.sphere2, R.raw.pano1024);



		setEGLContextClientVersion(2);
		setRenderer(mRenderer);
	}

	public boolean onTouchEvent(MotionEvent event) {
		boolean retVal = mScaleGestureDetector.onTouchEvent(event);
		retVal = mGestureDetector.onTouchEvent(event) || retVal;
		return retVal || super.onTouchEvent(event);
	}

	public void setRenderer(Renderer renderer) {
		super.setRenderer(renderer);

		mGameRenderer = (GameRenderer) renderer;

		mGameRenderer.setModelScale(mDefaultModelScale);
	}

	@Override
	protected void onDetachedFromWindow() {
		gyroscopeHandler.stop();
		super.onDetachedFromWindow();
	}
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(isInEditMode())
			return;

		gyroscopeHandler = new GyroscopeHandler2();



		mGameRenderer.useGyro = true;

		gyroscopeHandler.start(getContext(), new GyroscopeHandler2.OnGyroscopeChanged() {
//			@Override
//			public void onGyroscopeChange(double x, double y, double z) {
//
//				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//					mGameRenderer.setModelRotationX((float) (-(z + 90)));
//					mGameRenderer.setModelRotationY((float) -x);
//				} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//					mGameRenderer.setModelRotationX((float) (y));
//					mGameRenderer.setModelRotationY((float) -x);
//					mGameRenderer.setModelRotationZ((float) -z);
//				}
//			}

			@Override
			public void onGyroscopeChanged(float[] currentRotationMatrix) {
				if(mGameRenderer.useGyro) {
					mGameRenderer.setModelRotationMatrix(currentRotationMatrix);
				}
			}

			@Override
			public void onGyroscopeNotAvailable() {
				mGameRenderer.useGyro = false;
			}
		});
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor = mGameRenderer.getModelScale() / detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.30f, Math.min(mScaleFactor, 1.5f));
			//     mGameRenderer.mCameraZ *= mScaleFactor;
			mGameRenderer.setModelScale(mScaleFactor);
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
			if (mGameRenderer.useGyro) {
				return true;
			}
			xrot = mGameRenderer.getModelRotationX() - distanceY * TOUCH_SCALE;
			xrot = Math.max(Math.min(xrot, MAX_X_ROT), MIN_X_ROT);
			yrot = mGameRenderer.getModelRotationY() - distanceX * TOUCH_SCALE;

			mGameRenderer.setModelRotationX(xrot);
			mGameRenderer.setModelRotationY(yrot);

			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			mGameRenderer.resetModelScale(mDefaultModelScale);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (mGameRenderer.useGyro) {
				return true;
			}

			mGameRenderer.setVelocities(velocityX, velocityY);

			return false;
		}
	}
}