package com.slim.me.camerasample.camera_mpeg;

import com.slim.me.camerasample.egl.VideoEncoder;

/**
 * Created by slimxu on 2018/1/8.
 */

public class CameraRecorder {
    private VideoEncoder mEncoder;
    private EncodeInputSurface mInputSurface;

    public void start(int width, int height) {
        mEncoder = new VideoEncoder();
        mEncoder.start(width, height);
        mInputSurface = new EncodeInputSurface(mEncoder.getInputSurface());
    }

}
