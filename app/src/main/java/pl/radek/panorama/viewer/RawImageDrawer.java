package pl.radek.panorama.viewer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import pl.radek.panorama.R;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */

public class RawImageDrawer {
	HashMap<Integer, Integer> textures = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> texturesToReferencesCount = new HashMap<Integer, Integer>();
	int height;
	int width;

	int mBytesPerFloat = 4;

	Resources resources;
	int program2DHandle;
	FloatBuffer mScreenPositions;
	FloatBuffer mUVs;

	public RawImageDrawer(Resources r) {
		resources = r;

		mScreenPositions = ByteBuffer.allocateDirect(12 * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		//mScreenPositions.put(screenPointsData).position(0);

		mUVs = ByteBuffer.allocateDirect(12 * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
	}


	public void loadTexture(int r_id) {
		//if(textures.containsKey(r_id))return;
		int mTextureDataHandle = TextureHelper.loadTexture(resources, r_id);
		textures.put(r_id, mTextureDataHandle);
	}

	public void loadTexture(Bitmap bitmap) {
		//if(textures.containsKey(r_id))return;
		int mTextureDataHandle = TextureHelper.loadTexture(bitmap);
		textures.put(bitmap.hashCode(), mTextureDataHandle);
	}

	public int getTextureHandlerOrLoad(Bitmap bitmap) {
		if (!textures.containsKey(bitmap.hashCode())) {
			Log.e("texutre", "" + bitmap.hashCode());
			loadTexture(bitmap);
		}
		int currentReferenceCount = 0;
		int handle = textures.get(bitmap.hashCode());
		if(texturesToReferencesCount.containsKey(handle)) {
			currentReferenceCount = texturesToReferencesCount.get(handle);
		}
		texturesToReferencesCount.put(handle, ++currentReferenceCount);


		return (int) handle;
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
			Log.e("texutre", "" + r_id);
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
		return (int) textures.get(r_id);
	}

	public void initImageDrawing() {
		String vertexShaderCode = RawResourceReader.readTextFileFromRawResource(resources, R.raw.program2dvertexshader);
		String fragmentShaderCode = RawResourceReader.readTextFileFromRawResource(resources, R.raw.program2dfragmentshader);

		int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

		String[] attributes = {"vertexPosition_screenspace", "vertexUV"};
		program2DHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, attributes);
	}

//    public void drawNumber(int x, int y, int numHeight, int number) {
//
//        LinkedList<Integer> heap = new LinkedList<Integer>()
//
//        int numWidth=(int)(numHeight*29.0/40.0);
//
//
//        int offsetRight = (int)(Math.log10(number));
//        offsetRight++;
//        if(number==0)
//            offsetRight=1;
//        offsetRight = numWidth * offsetRight;
//
//        int whereX = x;
//
//        do{
//            int cyfra = number%10;
//            number = number/10;
//            heap.push(cyfra);
//        }while(number!=0);
//
//
//
//
//
//        int offset = 0;
//        while(!heap.isEmpty()) {
//            int cyfra = heap.pop();
//            int off=offset*numWidth;
//            offset++;
//
//            switch(cyfra) {
//                case 0 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._0); break;
//                case 1 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._1); break;
//                case 2 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._2); break;
//                case 3 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._3); break;
//                case 4 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._4); break;
//                case 5 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._5); break;
//                case 6 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._6); break;
//                case 7 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._7); break;
//                case 8 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._8); break;
//                case 9 : this.drawIMG(whereX+off, y, numWidth, numHeight, R.raw._9); break;
//                default: break;
//            }
//        }
//    }

//    public void drawBestScore(int score) {
//
//        LinkedList<Integer> heap = new LinkedList<Integer>();
//
//
//        int numWidth=29;
//        int numHeight=40;
//
//
//        int offsetRight = (int)(Math.log10(score));
//        offsetRight++;
//        if(score==0)
//            offsetRight=1;
//        offsetRight = numWidth * offsetRight;
//
//        int whereX = width - offsetRight;
//
//        do{
//            int cyfra = score%10;
//            score = score/10;
//            heap.push(cyfra);
//        }while(score!=0);
//
//
//
//
//
//        int offset = 0;
//        while(!heap.isEmpty()) {
//            int cyfra = heap.pop();
//            int off=offset*numWidth;
//            offset++;
//            switch(cyfra) {
//                case 0 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._0); break;
//                case 1 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._1); break;
//                case 2 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._2); break;
//                case 3 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._3); break;
//                case 4 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._4); break;
//                case 5 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._5); break;
//                case 6 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._6); break;
//                case 7 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._7); break;
//                case 8 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._8); break;
//                case 9 : this.drawIMG(whereX+off, 0, numWidth, numHeight, R.raw._9); break;
//                default: break;
//            }
//        }
//    }
}
