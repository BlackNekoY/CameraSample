package com.slim.me.camerasample.encoder;

import android.opengl.GLES11Ext;
import android.view.Surface;

import com.slim.me.camerasample.egl.EglCore;
import com.slim.me.camerasample.egl.EglSurfaceBase;
import com.slim.me.camerasample.render.TextureRender;

/**
 * Created by slimxu on 2018/1/8.
 */

public class EncodeInputSurface {
    private EglSurfaceBase mEglSurface;
    private EglCore mEglCore;
    private Surface mSurface;
    private TextureRender mRender;

    public void init(Surface surface) {
        mSurface = surface;
        mEglCore = new EglCore();
        mEglSurface = new EglSurfaceBase(mEglCore);
        mEglSurface.createWindowSurface(surface);
        mEglSurface.makeCurrent();

        // 创建画在EglSurface上的Render
        mRender = new TextureRender();
    }

    /**
     * Render使用OpenGL画在EglSurface中, 自动swapBuffer
     * 此方法运行在GLSurfaceView的GL线程中
     */
    public void draw(int textureType, int textureId, float[] stMatrix, long timestampNanos) {
        mRender.drawTexture(textureType, textureId, stMatrix, null);
        // 这里应该是另外一条线程
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
