package com.slim.me.camerasample.encode_mux;

import android.opengl.GLES20;

import com.slim.me.camerasample.egl.EglCore;
import com.slim.me.camerasample.egl.EglSurfaceBase;
import com.slim.me.camerasample.egl.VideoEncoder;

/**
 * Created by slimxu on 2018/1/4.
 */

public class EncodeAndMux {

    public static final String TAG = "EncodeAndMux";

    private static final int NUM_FRAMES = 30;
    // RGB color values for generated frames
    private static final int TEST_R0 = 0;
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 236;
    private static final int TEST_G1 = 50;
    private static final int TEST_B1 = 186;

    private EglSurfaceBase mEglSurface;
    private VideoEncoder mVideoEncoder;

    private int mWidth = 320;
    private int mHeight = 240;

    public void startEncodeMp4() {
        mVideoEncoder = new VideoEncoder();
        mVideoEncoder.start(mWidth, mHeight);

        mEglSurface = new EglSurfaceBase(new EglCore());
        mEglSurface.createWindowSurface(mVideoEncoder.getInputSurface());
        mEglSurface.makeCurrent();

        for(int i = 0;i < NUM_FRAMES;i++) {
            mVideoEncoder.frameAvaliable();

            generateSurfaceFrame(i);
            mEglSurface.setPresentationTime(mVideoEncoder.computePresentationTimeNsec(i));

            mEglSurface.swapBuffers();
        }

        mVideoEncoder.stop();
        mEglSurface.releaseEglSurface();
    }

    /**
     * Generates a frame of data using GL commands.  We have an 8-frame animation
     * sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the clear color.
     */
    private void generateSurfaceFrame(int frameIndex) {
        frameIndex %= 8;

        int startX, startY;
        if (frameIndex < 4) {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (mWidth / 4);
            startY = mHeight / 2;
        } else {
            startX = (7 - frameIndex) * (mWidth / 4);
            startY = 0;
        }

        GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(startX, startY, mWidth / 4, mHeight / 2);
        GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }
}
