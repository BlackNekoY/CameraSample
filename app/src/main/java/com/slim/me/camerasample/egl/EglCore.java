package com.slim.me.camerasample.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;

/**
 * 管理EGLContext,EGLDisplay,EGLConfig
 * 还提供了各种操作EGLSurface的方法
 * Created by slimxu on 2018/1/4.
 */

public class EglCore {

    public static final String TAG = "EglCore";
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;

    public EglCore () {
        this(null);
    }

    /**
     * 准备EGL display and context
     */
    public EglCore (EGLContext shareEglContext) {
        if(mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new IllegalStateException("EGL already set up.");
        }
        // 创建EGLDisplay
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if(mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new IllegalStateException("unable to get display.");
        }

        // 初始化EGL14
        int[] version = new int[2]; // [0]是主版本，[1]是次版本
        if(!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new IllegalStateException("unable to initialize EGL14.");
        }

        // 创建EGLConfig
        mEGLConfig = getConfig();
        if (mEGLConfig == null) {
            throw new RuntimeException("Unable to find a suitable EGLConfig");
        }

        if(shareEglContext == null) {
            shareEglContext = EGL14.EGL_NO_CONTEXT;
        }

        // 创建EGLContext
        int[] attriList = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,    // 使用OpenGL ES2.0
                EGL14.EGL_NONE
        };
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, shareEglContext, attriList, 0);
        checkEglError("eglCreateContext");

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values,
                0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    /**
     * 找到适合的EGLConfig
     * @return
     */
    private EGLConfig getConfig() {
        int[] attributeList = {
                EGL14.EGL_RED_SIZE, 8,  //R
                EGL14.EGL_GREEN_SIZE, 8,    //G
                EGL14.EGL_BLUE_SIZE, 8,     //B
                EGL14.EGL_ALPHA_SIZE, 8,    //A
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
        int[] numConfigs = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        if(!EGL14.eglChooseConfig(mEGLDisplay,
                attributeList, 0,
                configs, 0, configs.length,
                numConfigs, 0)) {
            throw new IllegalStateException("unable find EGLConfig.");
        }
        return configs[0];
    }

    public void release() {
        if(mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    /**
     * 创建上屏Surface
     */
    public EGLSurface createWindowSurface(Object surface) {
        int[] surfaceAttri = {
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface, surfaceAttri, 0);
        checkEglError("eglCreateWindowSurface");
        if(eglSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("surface was null.");
        }
        return eglSurface;
    }

    /**
     * 创建离屏Surface
     */
    public EGLSurface createOffScreenSurface(int width, int height) {
        int[] surfaceAttri = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surfaceAttri, 0);
        checkEglError("createOffScreenSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }

    public void releaseEglSurface(EGLSurface surface) {
        EGL14.eglDestroySurface(mEGLDisplay, surface);
    }


    public void makeCurrent(EGLSurface eglSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "NOTE: makeCurrent w/o display");
        }
        if(!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * 将OpenGL画在EGLSurface上的frame，publish到windowSurface 或 offscreenSurface上
     * @param surface
     */
    public boolean swapBuffers(EGLSurface surface) {
        boolean result = EGL14.eglSwapBuffers(mEGLDisplay, surface);
        checkEglError("swapBuffers");
        return result;
    }

    public void setPresentationTime(EGLSurface surface, long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, surface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }


    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}
