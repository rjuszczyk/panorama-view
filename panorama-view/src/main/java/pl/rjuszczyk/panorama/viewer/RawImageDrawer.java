package pl.rjuszczyk.panorama.viewer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */

public class RawImageDrawer {
	HashMap<Integer, Integer> textures = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> texturesToReferencesCount = new HashMap<Integer, Integer>();

	Resources resources;


	public RawImageDrawer(Resources r) {
		resources = r;
	}


	public void loadTexture(int r_id) {
		int mTextureDataHandle = TextureHelper.loadTexture(resources, r_id);
		textures.put(r_id, mTextureDataHandle);
	}

	public void loadTexture(Bitmap bitmap) {
		boolean a = isPowerOfTwo(bitmap.getHeight());
		boolean b = isPowerOfTwo(bitmap.getWidth());

		Bitmap bitmapScaled = bitmap;

		if(!a || !b) {
			int height = a ? bitmap.getHeight() : nextPowerOfTwo(bitmap.getHeight());
			int width = b ? bitmap.getWidth() : nextPowerOfTwo(bitmap.getWidth());
			bitmapScaled  = Bitmap.createScaledBitmap(bitmap, width,
					height, true);
		}

		int mTextureDataHandle = TextureHelper.loadTexture(bitmapScaled);
		textures.put(bitmap.hashCode(), mTextureDataHandle);
	}

	private boolean isPowerOfTwo(int number) {
		int n = 1;
		while (n < number) {
			if(n==number) {
				return true;
			}
			n<<=1;
		}
		return false;
	}

	private int nextPowerOfTwo(int number) {
		int n = 1;
		while (n < number) {
			n<<=1;
		}
		return n;
	}

	public int getTextureHandlerOrLoad(Bitmap bitmap) {
		if (!textures.containsKey(bitmap.hashCode())) {
			MyLog.e("texutre", "" + bitmap.hashCode());
			loadTexture(bitmap);
		}
		int currentReferenceCount = 0;
		int handle = textures.get(bitmap.hashCode());
		if(texturesToReferencesCount.containsKey(handle)) {
			currentReferenceCount = texturesToReferencesCount.get(handle);
		}
		texturesToReferencesCount.put(handle, ++currentReferenceCount);

		return handle;
	}

	public void releaseHandle(int handle) {

		int count = texturesToReferencesCount.get(handle);
		count--;
		if(count == 0) {
			texturesToReferencesCount.remove(handle);
		} else {
			texturesToReferencesCount.put(handle, count);
		}

		if(count == 0) {
			int removeThisKey = -1;
			for (Map.Entry<Integer, Integer> integerIntegerEntry : textures.entrySet()) {
				if (integerIntegerEntry.getValue() == handle) {
					removeThisKey = integerIntegerEntry.getKey();
				}
			}
			if (removeThisKey != -1) {
				textures.remove(removeThisKey);
			}
			TextureHelper.releaseTexture(handle);
		}
	}

	public int getTextureHandlerOrLoad(int r_id) {
		if (!textures.containsKey(r_id)) {
			MyLog.e("texutre", "" + r_id);
			loadTexture(r_id);
		}
		int handle = textures.get(r_id);
		int currentReferenceCount = 0;
		if(texturesToReferencesCount.containsKey(handle)) {
			currentReferenceCount = texturesToReferencesCount.get(handle);
		}
		texturesToReferencesCount.put(handle, ++currentReferenceCount);
		return handle;
	}

	public int getTextureHandle(int r_id) {
		if (!textures.containsKey(r_id)) {
			return -1;
		}
		return textures.get(r_id);
	}
}
