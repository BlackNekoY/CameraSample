package com.slim.me.camerasample.record.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;

import com.slim.me.camerasample.util.FilterUtil;
import com.slim.me.camerasample.util.GlUtil;

import java.nio.FloatBuffer;

/**
 * Created by slimxu on 2018/1/14.
 */


public class GPUBaseFilter {
    private static final String TAG = "GPUBaseFilter";

    private static final FloatBuffer VERTEX_BUF = GlUtil.createFloatBuffer(FilterUtil.VERTEXT_COORDS);
    private static final FloatBuffer TEXTURE_BUF = GlUtil.createFloatBuffer(FilterUtil.TEXUTURE_COORDS);

    private String mVertexShader;
    private String mFragmentShader;

    private int mProgram;
    private boolean mIsInitialized;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected int mTextureType = GLES20.GL_TEXTURE_2D;

    protected String mFilterName;
    public GPUBaseFilter(Context context) {
        this(context, null, null);
    }

    public GPUBaseFilter() {
        this(null);
    }

    public GPUBaseFilter(Context context, final String vertexShader, final String fragmentShader){
        mTextureType = GLES20.GL_TEXTURE_2D;
        setShaders(vertexShader, fragmentShader);
    }

    /**
     * 这里不允许多次设置，一个filter只控制一种效果展示任务
     */
    public void setShaders(final String vertexShader, final String fragmentShader) {
        if (TextUtils.isEmpty(mVertexShader) && !mIsInitialized) {
            mVertexShader = vertexShader;
            mFragmentShader = fragmentShader;
        } else {
            throw new RuntimeException("you can't set shader here!");
        }
    }

    public void init() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;

        if (TextUtils.isEmpty(mVertexShader)) {
            //这种使用默认的顶点和纹理shader
            mVertexShader = FilterUtil.NO_FILTER_VERTEX_SHADER;
            mFragmentShader = FilterUtil.NO_FILTER_FRAGMENT_SHADER;
        }
        mProgram = GlUtil.createProgram(mVertexShader, mFragmentShader);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program " + getClass().getSimpleName());
        }

        onInitialized();
    }

    protected void onInitialized() {
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    protected int getProgram(){
        return mProgram;
    }

    public void destroy() {
        if (mIsInitialized) {
            Log.d(TAG, "destroy");
            mIsInitialized = false;
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
            onDestroy();
        }
    }

    protected void onDestroy() {
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }


    /**
     * Draws the external texture in SurfaceTexture onto the current EGL surface.
     */
    public void drawTexture(int textureId, float[] textureMatrix, float[] mvpMatrix){
        FilterUtil.checkGlError("onDrawFrame start");
        int program = getProgram();

        if (textureMatrix == null) {
            textureMatrix = new float[16];
            Matrix.setIdentityM(textureMatrix, 0);
        }

        if (mvpMatrix == null) {
            mvpMatrix = new float[16];
            Matrix.setIdentityM(mvpMatrix, 0);
        }

        GLES20.glUseProgram(program);
        FilterUtil.checkGlError("glUseProgram");
        int aPosition = GLES20.glGetAttribLocation(program, "position");
        FilterUtil.checkLocation(aPosition, "position");
        int aTextureCoord = GLES20.glGetAttribLocation(program, "inputTextureCoordinate");
        FilterUtil.checkLocation(aTextureCoord, "inputTextureCoordinate");
        int uMVPMatrix = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        FilterUtil.checkLocation(uMVPMatrix, "uMVPMatrix");
        int uTextureMatrix = GLES20.glGetUniformLocation(program, "uTextureMatrix");
        FilterUtil.checkLocation(uTextureMatrix, "uTextureMatrix");

        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 2 * GlUtil.SIZEOF_FLOAT, VERTEX_BUF);
        FilterUtil.checkGlError("glVertexAttribPointer position");
        GLES20.glEnableVertexAttribArray(aPosition);
        FilterUtil.checkGlError("glEnableVertexAttribArray positionHandle");

        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 2 * GlUtil.SIZEOF_FLOAT, TEXTURE_BUF);
        FilterUtil.checkGlError("glVertexAttribPointer mTextureHandle");
        GLES20.glEnableVertexAttribArray(aTextureCoord);
        FilterUtil.checkGlError("glEnableVertexAttribArray textureHandle");

        GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uTextureMatrix, 1, false, textureMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureType, textureId);

        onDrawTexture();

        //打开混合，设置混合因子
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        FilterUtil.checkGlError("glDrawArrays");

        GLES20.glBindTexture(mTextureType, 0);
    }

    protected void onDrawTexture() {
    }
}
