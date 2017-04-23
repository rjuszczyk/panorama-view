package pl.radek.panorama.viewer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */

public class TextureHelper {
	public static int loadTexture(final Resources resources, final int resourceId) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;    // No pre-scaling
		final Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, options);
		return loadTexture(bitmap);
	}

	public static void releaseTexture(int handle) {
		GLES20.glDeleteTextures(1, new int[]{handle}, 0);
	}

	public static int loadTexture(Bitmap bitmap, int handle) {
		final int[] textureHandle = new int[]{handle};

		if (textureHandle[0] != 0) {
			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}
		if (textureHandle[0] == 0) {
			throw new RuntimeException("Error loading texture.");
		}
		Log.i("debug", "texutre loaded");
		return textureHandle[0];
	}
	public static int loadTexture(Bitmap bitmap) {
		final int[] textureHandle = new int[1];
		GLES20.glGenTextures(1, textureHandle, 0);
		return loadTexture(bitmap, textureHandle[0]);
	}
}

