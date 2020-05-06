package com.slim.me.camerasample.record.render.filter;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.slim.me.camerasample.util.OpenGLUtils;


public class WatermarkFilter extends GPUImageFilter {

    private Bitmap mWatermark;
    private int mBitmapWidth;
    private int mBitmapHeight;
    private int mWatermarkTexture;

    private GPUImageFilter mWaterFilter;
    private float[] mWatermarkRotateMatrix = new float[16];

    public WatermarkFilter(Bitmap watermark) {
        mWatermark = watermark;
        mBitmapWidth = watermark.getWidth();
        mBitmapHeight = watermark.getHeight();
    }

    @Override
    protected void onInitialized() {
        mWaterFilter = new GPUImageFilter();
        mWaterFilter.init();
        mWatermarkTexture = OpenGLUtils.createTexture2D(mWatermark, GLES30.GL_NEAREST, GLES30.GL_LINEAR, GLES30.GL_MIRRORED_REPEAT, GLES30.GL_MIRRORED_REPEAT);
    }

    @Override
    protected void onPreDraw(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        super.onPreDraw(textureId, cameraMatrix, textureMatrix);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void onAfterDraw(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glViewport(outputWidth - mBitmapWidth - 100, mBitmapHeight + 100, mBitmapWidth, mBitmapHeight);
        Matrix.setIdentityM(mWatermarkRotateMatrix, 0);
        Matrix.translateM(mWatermarkRotateMatrix, 0, 0f, 1f, 0);
        Matrix.rotateM(mWatermarkRotateMatrix, 0, 180, 1.0f, 0.0f, 0.0f);
        mWaterFilter.draw(mWatermarkTexture, null, mWatermarkRotateMatrix);
        GLES30.glViewport(0, 0, outputWidth, outputHeight);
    }

}
