package com.slim.me.camerasample.camera_mpeg;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.slim.me.camerasample.egl.EglCore;
import com.slim.me.camerasample.egl.EglSurfaceBase;

/**
 * Created by slimxu on 2018/1/8.
 */

public class EncodeInputSurface {
    private EglSurfaceBase mEglSurface;
    private EglCore mEglCore;
    private Surface mSurface;
    private TextureRender mRender;

    public EncodeInputSurface(Surface surface) {
        mSurface = surface;
        mEglCore = new EglCore();
        mEglSurface = new EglSurfaceBase(mEglCore);
        mEglSurface.makeCurrent();

        mRender = new TextureRender();
    }

    /**
     * Render使用OpenGL画在EglSurface中, 自动swapBuffer
     */
    public void draw(SurfaceTexture surfaceTexture, long timestampNanos) {
        mRender.drawFrame(surfaceTexture);
        mEglSurface.setPresentationTime(timestampNanos);
        mEglSurface.swapBuffers();
    }

    public void release() {
        if(mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if(mEglSurface != null) {
            mEglSurface.releaseEglSurface();
            mEglSurface = null;
        }
        if(mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

}
