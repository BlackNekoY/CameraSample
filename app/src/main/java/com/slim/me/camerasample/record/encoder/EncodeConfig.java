package com.slim.me.camerasample.record.encoder;


import android.opengl.EGLContext;

/**
 * Created by slimxu on 2017/11/19.
 * 合成视频的参数配置类
 */

public class EncodeConfig {
    public EGLContext sharedContext;
    /**
     * 输出路径
     */
    public String outputPath;

    /**
     * 视频长度
     */
    public long durationMs;

    public int width;
    public int height;

    /**
     * {@see MediaFormat.KEY_BIT_RATE}
     */
    public int bitRate;

    /**
     * {@see MediaFormat.KEY_I_FRAME_INTERVAL}
     */
    public int iFrameRate;

    /**
     * {@see MediaFormat.KEY_FRAME_RATE}
     */
    public int frameRate;

    public int orientation;

    public EncodeConfig(String outputPath, int width, int height, int bitRate, int iFrameRate, int frameRate, int orientation) {
        this.outputPath = outputPath;
        this.width = width;
        this.height = height;
        this.bitRate = bitRate;
        this.iFrameRate = iFrameRate;
        this.frameRate = frameRate;
        this.orientation = orientation;
    }

    public void updateEglContext(EGLContext eglContext) {
        sharedContext = eglContext;
    }

    public EGLContext getSharedEglContext() {
        return sharedContext;
    }
}

