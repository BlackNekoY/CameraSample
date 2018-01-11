package com.slim.me.camerasample.encode_mux;


import com.slim.me.camerasample.egl.EglCore;
import com.slim.me.camerasample.egl.EglSurfaceBase;
import com.slim.me.camerasample.egl.VideoEncoder;
import com.slim.me.camerasample.encoder.EncodeConfig;

/**
 * Created by slimxu on 2018/1/4.
 */

public class EncodeAndMux {

    public static final String TAG = "EncodeAndMux";

    private static final int NUM_FRAMES = 30;

    private EglSurfaceBase mEglSurface;
    private EncodeAndMuxRender mRender;
    private VideoEncoder mVideoEncoder;
    private EncodeConfig mEncodeConfig;

    private int mWidth = 320;
    private int mHeight = 240;

    public void startEncodeMp4() {
        mEncodeConfig = new EncodeConfig(null, mWidth, mHeight, 0, 0, 0, 0);
        mVideoEncoder = new VideoEncoder();
        mVideoEncoder.start(mEncodeConfig);

        mEglSurface = new EglSurfaceBase(new EglCore());
        mEglSurface.createWindowSurface(mVideoEncoder.getInputSurface());
        mEglSurface.makeCurrent();

        mRender = new EncodeAndMuxRender(mWidth, mHeight);

        for(int i = 0;i < NUM_FRAMES;i++) {
            mVideoEncoder.frameAvaliable();

            mRender.drawFrame(i);
            mEglSurface.setPresentationTime(mVideoEncoder.computePresentationTimeNsec(i));

            mEglSurface.swapBuffers();
        }

        mVideoEncoder.stop();
        mEglSurface.releaseEglSurface();
    }


}
