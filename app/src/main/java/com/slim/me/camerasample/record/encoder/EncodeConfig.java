package com.slim.me.camerasample.record.encoder;


import android.opengl.EGLContext;

/**
 * Created by slimxu on 2017/11/19.
 * 合成视频的参数配置类
 */

public class EncodeConfig {
    public EGLContext sharedContext;
    public String outputPath;
    public int width;
    public int height;

    // Video
    /**
     * {@see MediaFormat.KEY_BIT_RATE}
     */
    public final int videoBitRate;

    /**
     * {@see MediaFormat.KEY_I_FRAME_INTERVAL}
     */
    public final int videoIFrameRate;

    /**
     * {@see MediaFormat.KEY_FRAME_RATE}
     */
    public final int videoFrameRate;

    public final int orientation;

    // Audio
    public final int audioSampleRate;

    public final int audioBitRate;

    public EncodeConfig(String outputPath, int width, int height, int videoBitRate, int videoIFrameRate, int videoFrameRate, int orientation,
                        int audioSampleRate, int audioBitRate) {
        this.outputPath = outputPath;
        this.width = width;
        this.height = height;
        this.videoBitRate = videoBitRate;
        this.videoIFrameRate = videoIFrameRate;
        this.videoFrameRate = videoFrameRate;
        this.orientation = orientation;
        this.audioSampleRate = audioSampleRate;
        this.audioBitRate = audioBitRate;
    }

    public void updateEglContext(EGLContext eglContext) {
        sharedContext = eglContext;
    }

    public EGLContext getSharedEglContext() {
        return sharedContext;
    }
}

