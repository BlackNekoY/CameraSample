package com.slim.me.camerasample.record.render;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

/**
 * Created by slimxu on 2018/1/8.
 */

public class TextureRender {

    private GPUBaseFilter mGPU2DFilter;
    private GPUOESBaseFilter mGPUOESFilter;

    public TextureRender(Context context) {
        mGPU2DFilter = new GPUBaseFilter(context);
        mGPUOESFilter = new GPUOESBaseFilter(context);
        mGPU2DFilter.init();
        mGPUOESFilter.init();
    }

    public TextureRender() {
        mGPU2DFilter = new GPUBaseFilter();
        mGPUOESFilter = new GPUOESBaseFilter();
        mGPU2DFilter.init();
        mGPUOESFilter.init();
    }

    /**
     * Draws the external texture in SurfaceTexture onto the current EGL surface.
     */
    public void drawTexture(int textureType, int textureId, float[] textureMatrix, float[] mvpMatrix) {
        if (textureType == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            mGPUOESFilter.drawTexture(textureId, textureMatrix, mvpMatrix);
        } else if (textureType == GLES20.GL_TEXTURE_2D) {
            mGPU2DFilter.drawTexture(textureId, textureMatrix, mvpMatrix);
        } else {
            throw new RuntimeException("textureType must be GLES11Ext.GL_TEXTURE_EXTERNAL_OES or GLES20.GL_TEXTURE_2D.");
        }
    }

    public void release() {
        mGPU2DFilter.destroy();
        mGPUOESFilter.destroy();
    }
}
