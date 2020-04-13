package com.slim.me.camerasample.record.render.filter;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;


public class WatermarkFilter extends GPUImageFilter {

    private Bitmap mWatermark;
    private int mWatermarkTexture;

    private GPUImageFilter mWaterFilter;

    public WatermarkFilter(Bitmap watermark) {
        mWatermark = watermark;
    }

    @Override
    protected void onInitialized() {
        mWaterFilter = new GPUImageFilter();
        mWaterFilter.init();

        int[] texture = new int[1];
        GLES30.glGenTextures(1, texture, 0);
        mWatermarkTexture = texture[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mWatermarkTexture);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_MIRRORED_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_MIRRORED_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mWatermark, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        mWatermark.recycle();
        mWatermark = null;
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
        GLES30.glViewport(outputWidth - 300, 300, 200, 200);
        mWaterFilter.draw(mWatermarkTexture, null, null);
        GLES30.glViewport(0, 0, outputWidth, outputHeight);
    }

}
