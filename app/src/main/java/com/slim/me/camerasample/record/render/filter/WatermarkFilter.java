package com.slim.me.camerasample.record.render.filter;

import android.graphics.Bitmap;
import android.opengl.GLES30;

import com.slim.me.camerasample.util.OpenGLUtils;


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
        GLES30.glViewport(outputWidth - 300, 300, 200, 200);
        mWaterFilter.draw(mWatermarkTexture, null, null);
        GLES30.glViewport(0, 0, outputWidth, outputHeight);
    }

}
