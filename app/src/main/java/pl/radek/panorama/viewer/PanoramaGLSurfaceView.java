package pl.radek.panorama.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
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
	Renderer mRenderer;
	RawImageDrawer mImageDrawer;

	GyroscopeHandler gyroscopeHandler;
	private float mDefaultModelScale = 1f;
	private PanoramaRenderer mPanoramaRenderer;
	private float mScaleFactor = 1;
	private float xrot;                    //X Rotation
	private float yrot;                    //Y Rotation


	private ScaleGestureDetector mScaleGestureDetector;
	public PanoramaGLSurfaceView(Context context) {
		super(context);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new RawImageDrawer(getResources());


		mRenderer = new PanoramaRenderer(context, mImageDrawer, R.raw.sphere2, R.raw.pano1024);


		setEGLContextClientVersion(2);
		setRenderer(mRenderer);
	}

	public PanoramaGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if(isInEditMode())
			return;

		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new MyOnGestureListener());
		mScaleFactor = mDefaultModelScale;

		mImageDrawer = new RawImageDrawer(getResources());


		mRenderer = new PanoramaRenderer(context, mImageDrawer, R.raw.sphere2, R.raw.pano1024);



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

		mPanoramaRenderer = (PanoramaRenderer) renderer;

		mPanoramaRenderer.setModelScale(mDefaultModelScale);
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

		gyroscopeHandler = new GyroscopeHandler();


		mPanoramaRenderer.useGyro = true;

		gyroscopeHandler.start(getContext(), new GyroscopeHandler.OnGyroscopeChanged() {


			@Override
			public void onGyroscopeChange(double x, double y, double z) {

			}

			@Override
			public void onGyroscopeChanged2(float[] currentRotationMatrix) {
				if(mPanoramaRenderer.useGyro) {
					mPanoramaRenderer.setModelRotationMatrix(currentRotationMatrix);
				}
			}

			@Override
			public void onGyroscopeNotAvailable() {
				mPanoramaRenderer.useGyro = false;
			}
		});
	}

	public void reset() {
		gyroscopeHandler.reset();
		gyroscopeHandler.restart();
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
			if (mPanoramaRenderer.useGyro) {
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