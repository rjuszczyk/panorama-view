package pl.radek.panorama.viewer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import pl.radek.panorama.R;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */

public class ImageDrawer {
	HashMap<Integer, Integer> textures = new HashMap<Integer, Integer>();
	int height;
	int width;

	int mBytesPerFloat = 4;

	Resources resources;
	int program2DHandle;
	FloatBuffer mScreenPositions;
	FloatBuffer mUVs;

	public ImageDrawer(Resources r) {
		resources = r;

		mScreenPositions = ByteBuffer.allocateDirect(12 * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		//mScreenPositions.put(screenPointsData).position(0);

		mUVs = ByteBuffer.allocateDirect(12 * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
	}
	/*public void loadTextures() {

        this.loadTexture(R.raw._0);
        this.loadTexture(R.raw._1);
        this.loadTexture(R.raw._2);
        this.loadTexture(R.raw._3);
        this.loadTexture(R.raw._4);
        this.loadTexture(R.raw._5);
        this.loadTexture(R.raw._6);
        this.loadTexture(R.raw._7);
        this.loadTexture(R.raw._8);
        this.loadTexture(R.raw._9);
        this.loadTexture(R.raw._0);

        this.loadTexture(R.raw.back_button);
        this.loadTexture(R.raw.exit_button);
        this.loadTexture(R.raw.resume_button);
        this.loadTexture(R.raw.start_button);

        this.loadTexture(R.raw.theme_bg);


        this.loadTexture(R.raw.start_game);
        this.loadTexture(R.raw.you_lose);
        this.loadTexture(R.raw.you_lose_text);
        this.loadTexture(R.raw.you_win_text);
        this.loadTexture(R.raw.wide_pause);
        this.loadTexture(R.raw.wide_menu);
        //this.loadTexture(R.raw.winner);
        this.loadTexture(R.raw.loading_meshes);
        this.loadTexture(R.raw.loading_textures);
        this.loadTexture(R.raw.start_button);
        this.loadTexture(R.raw.new_level_unlocked);
        this.loadTexture(R.raw.new_record);
        this.loadTexture(R.raw.notyet1);
        this.loadTexture(R.raw.notyet2);
        this.loadTexture(R.raw.notyet3);
        this.loadTexture(R.raw.notyet4);
        this.loadTexture(R.raw.serce);
        this.loadTexture(R.raw.pause);

        this.loadTexture(R.raw.level1);
        this.loadTexture(R.raw.level2);
        this.loadTexture(R.raw.level3);
        this.loadTexture(R.raw.level2locked);
        this.loadTexture(R.raw.level3locked);

        this.loadTexture(R.raw.tex1);
        this.loadTexture(R.raw.tex2);
        this.loadTexture(R.raw.tex3);
        this.loadTexture(R.raw.tex4);
        this.loadTexture(R.raw.rury2_textura);
        Log.i("textures", "textures loaded");
    }*/

	public void setScreenSize(int h, int w) {
		this.width = w;
		this.height = h;
	}

	public void drawIMG(int imageResourceHandle) {
		double ratio = this.width / this.height;
		int offset = (int) ((ratio - 2.0) * this.height * 0.5);
		drawIMG(offset, 0, this.width - offset, this.height, imageResourceHandle);
	}

	public void drawIMG(float x, float y, float width, float height, int imageResourceHandle) {
		//Log.e("debug2", "image_res="+imageResourceHandle);
		//x=x+20;
		float[] screenPointsData = {
				x, this.height - y,
				x, this.height - y - height,
				x + width, this.height - y,


				x, this.height - y - height,
				x + width, this.height - y - height,
				x + width, this.height - y
		};
		//float max = width>height ? width : height;
		float[] UVsData = {
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,


				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f
		};
		mScreenPositions.position(0);
		mScreenPositions.put(screenPointsData).position(0);
		mUVs.position(0);
		mUVs.put(UVsData).position(0);

		GLES20.glUseProgram(program2DHandle);

		int vertexPosition_screenspace = GLES20.glGetAttribLocation(program2DHandle, "vertexPosition_screenspace");
		int vertexUV = GLES20.glGetAttribLocation(program2DHandle, "vertexUV");
		int myTextureSampler = GLES20.glGetUniformLocation(program2DHandle, "myTextureSampler");
		int screenSizeHalf = GLES20.glGetUniformLocation(program2DHandle, "screenSizeHalf");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		int mTextureDataHandle = getTextureHandler(imageResourceHandle);//textures.get(imageResourceHandle);
		//  the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

		// Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
		GLES20.glUniform1i(myTextureSampler, 0);
		GLES20.glUniform2f(screenSizeHalf, this.width / 2, this.height / 2);
		// Pass in the position information
		mScreenPositions.position(0);
		GLES20.glVertexAttribPointer(vertexPosition_screenspace, 2, GLES20.GL_FLOAT, false, 0, mScreenPositions);
		GLES20.glEnableVertexAttribArray(vertexPosition_screenspace);

		mUVs.position(0);
		GLES20.glVertexAttribPointer(vertexUV, 2, GLES20.GL_FLOAT, false, 0, mUVs);
		GLES20.glEnableVertexAttribArray(vertexUV);

		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
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

	public int getTextureHandler(Bitmap bitmap) {
		if (!textures.containsKey(bitmap.hashCode())) {
			Log.e("texutre", "" + bitmap.hashCode());
			loadTexture(bitmap);
		}
		return (int) textures.get(bitmap.hashCode());
	}

	public int getTextureHandler(int r_id) {
		if (!textures.containsKey(r_id)) {
			Log.e("texutre", "" + r_id);
			loadTexture(r_id);
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
