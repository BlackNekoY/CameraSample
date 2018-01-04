package com.slim.me.camerasample.egl;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.util.Log;

/**
 * 管理EglCore and EGLSurface
 * Created by slimxu on 2018/1/4.
 */

public class EglSurfaceBase {

    public static final String TAG = "EglSurfaceBase";

    private EglCore mEglCore;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

    private int mWidth;
    private int mHeight;

    public EglSurfaceBase(EglCore eglCore) {
        mEglCore = eglCore;
    }

    public void createWindowSurface(Object surface) {
        if(mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("surface already created.");
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);
    }

    public void createOffscreenSurface(int width, int height) {
        if(mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("surface already created.");
        }
        mEGLSurface = mEglCore.createOffScreenSurface(width, height);
    }

    public void releaseEglSurface() {
        mEglCore.releaseEglSurface(mEGLSurface);
        mEGLSurface = EGL14.EGL_NO_SURFACE;
        mWidth = mHeight = -1;
    }

    public void makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface);
    }

    public boolean swapBuffers() {
        boolean result = mEglCore.swapBuffers(mEGLSurface);
        if(!result) {
            Log.e(TAG, "swapBuffers failed.");
        }
        return result;
    }

    public void setPresentationTime(long nsecs) {
        mEglCore.setPresentationTime(mEGLSurface, nsecs);
    }

}
