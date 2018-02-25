package com.slim.me.camerasample.encode_test;


import com.slim.me.camerasample.egl.EglCore;
import com.slim.me.camerasample.egl.EglSurfaceBase;
import com.slim.me.camerasample.record.encode.VideoEncoder;
import com.slim.me.camerasample.record.encode.EncodeConfig;

import java.io.IOException;

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

    public void startEncodeMp4() throws IOException {
        mEncodeConfig = new EncodeConfig(null, mWidth, mHeight, 0, 0, 0, 0);
        mVideoEncoder = new VideoEncoder();
        mVideoEncoder.start(mEncodeConfig);

        mEglSurface = new EglSurfaceBase(new EglCore(null));
        mEglSurface.createWindowSurface(mVideoEncoder.getInputSurface());
        mEglSurface.makeCurrent();

        mRender = new EncodeAndMuxRender(mWidth, mHeight);

        for(int i = 0;i < NUM_FRAMES;i++) {
            mVideoEncoder.frameAvaliable();

            mRender.drawFrame(i);
            mEglSurface.setPresentationTime(computePresentationTimeNsec(i));

            mEglSurface.swapBuffers();
        }

        mVideoEncoder.stop();
        mEglSurface.releaseEglSurface();
    }

    public int computePresentationTimeNsec(int frameIndex) {
//        final long ONE_BILLION = 1000000000;
//        return frameIndex * ONE_BILLION / FRAME_RATE;
        return 0;
    }

}
