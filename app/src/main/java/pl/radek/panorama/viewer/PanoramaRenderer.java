package pl.radek.panorama.viewer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import pl.radek.panorama.R;


/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */
public class PanoramaRenderer implements GLSurfaceView.Renderer {

    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] mLightPosInWorldSpace = new float[4];
    private final float[] mLightPosInEyeSpace = new float[4];
    //Handlers end
    private final int mPositionDataSize = 3;
    private final int mNormalDataSize = 3;
    private final int mUVDataSize = 2;
    public float[] mModelMatrix = new float[16];
    public float mCameraZTarget = 1;

    TexturedMesh sphereMeshWithTexture;
    float ratio;
    long mLastTime = -1;
    boolean firstFrame = true;
    private RawImageDrawer mImageDrawer;
    private int mPerVertexProgramHandle;
    private float[] mProjectionMatrix = new float[16];
    private Resources mResources;
    private int mModelResourceId, mTextureResourceId;
    private float[] mViewMatrix = new float[16];
    //private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mLightModelMatrix = new float[16];
    //Handlers start:
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandle;
    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;
    private float mModelScale = 1f;
    private float mModelRotationX = 0;
    private float mModelRotationY = 0;
    private float mModelRotationZ = 0;
    private float mCameraZ = 1;

    private float mModelScaleTarget = 1f;
    private float mModelRotationXTarget = 0;
    private float mModelRotationYTarget = 0;
    private float mModelRotationZTarget = 0;
    private float velocityX = 0;
    private float velocityY = 0;

    public PanoramaRenderer(Context context, RawImageDrawer imageDrawer, int modelResourceId) {
        mResources = context.getResources();
        mImageDrawer = imageDrawer;
        mModelResourceId = modelResourceId;
        mTextureResourceId = -1;
        initTouchRotation();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
// Set the background clear color to black.
        GLES20.glClearColor(100.0f / 256f, 182.0f / 256f, 240.0f / 256f, 0.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);


        //GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);


        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mResources, R.raw.walls_vertexshader);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mResources, R.raw.walls_fragmentshader);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Color", "a_Normal", "a_TexCoordinate"});

        //gameState.setProgramHandle(mPerVertexProgramHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        ratio = (float) width / height;

        PerspectiveMatrix.perspectiveM(mProjectionMatrix, 0, 60.0f, ratio, 0.25f, 100.0f);
        //  gameState.setProjectionMatrix(mProjectionMatrix);


        Mesh sphereMesh = Mesh.getMeshSerialized(mModelResourceId, mResources);
        if(mTextureResourceId == -1) {
            sphereMeshWithTexture = new TexturedMesh(sphereMesh, -1);
        } else {
            int currentTextureHanlde = mImageDrawer.getTextureHandlerOrLoad(mTextureResourceId);
            sphereMeshWithTexture = new TexturedMesh(sphereMesh, currentTextureHanlde);
        }
        //sphereMesh = Mesh.getMeshSerialized(mModelResourceId, mResources, mTextureResourceId);
        //imgDrawer.setScreenSize(height, width);
    }

    public void resetCamera() {


        mModelScaleTarget = 1;
        mModelRotationXTarget = 0;
        mModelRotationYTarget = 0;
        mModelRotationZTarget = 0;
        mCameraZTarget = 1;
    }

    public float getModelScale() {
        return mModelScale;
    }

    public void setModelScale(float scale) {
        if (scale < 0.3)
            scale = 0.3f;
        if (scale > 1.5)
            scale = 1.5f;

        mModelScale = scale;
        mModelScaleTarget = scale;
    }

    public float getModelRotationY() {
        return mModelRotationY;
    }

    public void setModelRotationY(float rotY) {
        mModelRotationY = rotY;
        mModelRotationYTarget = rotY;
    }

    public void setModelRotationZ(float rotZ) {
        mModelRotationZ = rotZ;
        mModelRotationZTarget = rotZ;
    }

    float[] mModelRotationMatrix = null;

    public void setModelRotationMatrix(float[] matrix) {
        mModelRotationMatrix = matrix;
    }

    public void setVelocities(float x, float y) {
        velocityX = x;
        velocityY = y;
    }

    public float getModelRotationX() {
        return mModelRotationX;
    }

    public void setModelRotationX(float rotX) {
        mModelRotationX = rotX;
        mModelRotationXTarget = rotX;
    }

    public void resetModelScale(float defaultScale) {
        mModelScaleTarget = defaultScale;
    }

    private float lerp(float t, float a, float b) {
        return (1 - t) * a + t * b;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        if (firstFrame) {
            firstFrame = false;
            mModelRotationX = 30;
            mModelRotationXTarget = 0;
            mModelRotationY = 80;
            mModelRotationYTarget = 0;
            mModelScaleTarget = 1f;
            mModelScale = 1f;
            Matrix.setIdentityM(mModelMatrix, 0);
        }

        long deltaTime = 1;

        if (mLastTime == -1) {
            mLastTime = System.currentTimeMillis();
        } else {
            long millis = System.currentTimeMillis();
            deltaTime = millis - mLastTime;
            mLastTime = millis;
        }

        float fov = mModelScale * 100f;

        if (fov > 150) {
            fov = 100;
        }
        if (fov < 30) {
            fov = 30;
        }
        mModelScale = fov / 100;
        //100
        //20

        PerspectiveMatrix.perspectiveM(mProjectionMatrix, 0, fov, ratio, 0.25f, 100.0f);

        float factor = deltaTime / 10000f;


        setModelRotationY(getModelRotationY() + velocityX * factor);
        float xrot = getModelRotationX() + velocityY * factor;
        xrot = Math.max(Math.min(xrot, PanoramaGLSurfaceView.MAX_X_ROT), PanoramaGLSurfaceView.MIN_X_ROT);
        setModelRotationX(xrot);

        velocityY = velocityY * (1 - factor * 50);
        velocityX = velocityX * (1 - factor * 50);

        mModelScale = lerp(factor, mModelScale, mModelScaleTarget);
        mModelRotationX = lerp(factor, mModelRotationX, mModelRotationXTarget);
        mModelRotationY = lerp(factor, mModelRotationY, mModelRotationYTarget);
        mModelRotationZ = lerp(factor, mModelRotationZ, mModelRotationZTarget);
        mCameraZ = lerp(factor, mCameraZ, mCameraZTarget);

        final float eyeX = 0;
        final float eyeY = 0;
        final float eyeZ = 0;//0.5f + mCameraZ;

        // We are looking toward the distance
        final float lookX = 0;
        final float lookY = 0;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;


        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        GLES20.glUseProgram(mPerVertexProgramHandle);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix"); //ok
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position"); //ok
        mColorHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_Color"); //ok
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal");

        mTextureUniformHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_TexCoordinate");

        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 1.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        Matrix.setIdentityM(mModelMatrix, 0);

        if (mModelRotationMatrix != null) {
            float[] v = convertM9toM16(mModelRotationMatrix, mModelMatrix);

            Matrix.multiplyMM(mModelMatrix, 0, v, 0, mModelMatrix, 0);
            Matrix.rotateM(mModelMatrix, 0, 90, 1, 0, 0);
        } else {
            float[] right = new float[]{0, 0, 1, 0};
            Matrix.rotateM(mModelMatrix, 0, -mModelRotationX, 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(mModelMatrix, 0, -mModelRotationY, 0.0f, 1.0f, 0.0f);

            Matrix.multiplyMV(right, 0, mModelMatrix, 0, right, 0);
            Matrix.rotateM(mModelMatrix, 0, -mModelRotationZ, right[0], right[1], right[2]);
        }

        //Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.multiplyMM(mModelMatrix, 0, mModelMatrix, 0,touchRotationMatrix,0);


        drawMesh(sphereMeshWithTexture, mProjectionMatrix);
    }

    private float[] convertM9toM16(float[] m9, float[] m16) {
        float[] v = new float[16];


        v[0] = m9[0];
        v[1] = m9[1];
        v[2] = m9[2];
        v[3] = m16[3];

        v[4] = m9[3];
        v[5] = m9[4];
        v[6] = m9[5];
        v[7] = m16[7];

        v[8] = m9[6];
        v[9] = m9[7];
        v[10] = m9[8];
        v[11] = m16[11];

        v[12] = m16[12];
        v[13] = m16[13];
        v[14] = m16[14];
        v[15] = m16[15];
        return v;
    }

    public static void transposeM(float[] mTrans, int mTransOffset, float[] m,
                                  int mOffset) {
        for (int i = 0; i < 3; i++) {
            int mBase = i * 3 + mOffset;
            mTrans[i + mTransOffset] = m[mBase];
            mTrans[i + 3 + mTransOffset] = m[mBase + 1];
            mTrans[i + 6 + mTransOffset] = m[mBase + 2];
        }
    }


    public void drawMesh(TexturedMesh mesh, float[] mProjectionMatrix) {
        FloatBuffer positionsBuffer = mesh.getMesh().getPositionsBuffer();
        FloatBuffer normalsBuffer = mesh.getMesh().getNormalsBuffer();
        FloatBuffer uvsBuffer = mesh.getMesh().getUVsBuffer();

        float[] color = mesh.getMesh().getColor();

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mesh.getTextureDataHandle());

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);


        positionsBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, positionsBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        normalsBuffer.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 0, normalsBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        uvsBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mUVDataSize, GLES20.GL_FLOAT, false, 0, uvsBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);


        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        GLES20.glUniform4f(mColorHandle, color[0], color[1], color[2], color[3]);

        // Draw the mesh.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.getMesh().getSize());

        if (runnableToDo != null) {
            runnableToDo.run();
            runnableToDo = null;
        }
    }

    Runnable runnableToDo = null;

    public void setTex_resourceID(final int tex_resourceID) {
        runnableToDo = new Runnable() {
            @Override
            public void run() {
                int textureHandle = mImageDrawer.getTextureHandlerOrLoad(tex_resourceID);
                int toRemoveHandle = sphereMeshWithTexture.setTextureDataHandle(textureHandle);
                if(toRemoveHandle != -1) {
                    mImageDrawer.releaseHandle(toRemoveHandle);
                }

            }
        };
    }

    public void setTextureBitmap(final Bitmap bitmap) {
        runnableToDo = new Runnable() {
            @Override
            public void run() {
                int textureHandle = mImageDrawer.getTextureHandlerOrLoad(bitmap);
                int toRemoveHandle = sphereMeshWithTexture.setTextureDataHandle(textureHandle);
                if(toRemoveHandle != -1) {
                    mImageDrawer.releaseHandle(toRemoveHandle);
                }
            }
        };
    }

    float[] right = new float[]{1,0,0,0};
    float[] up = new float[]{0,1,0,0};
    float[] touchRotationMatrix = new float[16];
    public void initTouchRotation() {
        Matrix.setIdentityM(touchRotationMatrix, 0);
    }

    public void rotate(float x, float y) {
        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        //Matrix.setRotateEulerM(identity, 0, 0, x, 0);
        Matrix.rotateM(identity, 0, x, 0,1,0);
        Matrix.rotateM(identity, 0, y, 1,0,0);
        Matrix.multiplyMM(touchRotationMatrix, 0, identity, 0, touchRotationMatrix, 0);
    }

    public void rotateZ(float z) {
        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        Matrix.rotateM(identity, 0, z, 0,0,1);
        Matrix.multiplyMM(touchRotationMatrix, 0, identity, 0, touchRotationMatrix, 0);
    }

    public void rotateRight(float v) {

        Matrix.rotateM(touchRotationMatrix, 0, v, up[0], up[1], up[2]);

        float[] identity = new float[16];

        Matrix.setIdentityM(identity, 0);

        Matrix.rotateM(identity, 0, v, up[0], up[1], up[2]);

        Matrix.multiplyMV(right, 0, identity, 0, right, 0);
        Log.d("right", String.format("(%f, %f, %f)", right[0], right[1], right[2]));
    }

    public void rotateUp(float v) {
        Matrix.rotateM(touchRotationMatrix, 0, v, right[0], right[1], right[2]);

        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        Matrix.rotateM(identity, 0, v, right[0], right[1], right[2]);
//
//        Matrix.multiplyMV(up, 0, identity, 0, up, 0);
    }
}
