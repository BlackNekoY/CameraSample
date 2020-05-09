package com.slim.me.camerasample.record.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.slim.me.camerasample.egl.EglCore;
import com.slim.me.camerasample.record.render.Texture2DRender;
import com.slim.me.camerasample.record.render.filter.GPUImageFilter;

import java.io.IOException;

/**
 * 摄像机视频录制器
 */
public class VideoEncoder {

    public static final String TAG = "VideoEncoder";

    private static final int MSG_START_RECORD = 1;
    private static final int MSG_STOP_RECORD = 2;
    private static final int MSG_VIDEO_FRAME_UPDATE = 3;

    private String VIDEO_MIME_TYPE = "video/avc";

    private MediaCodec mVideoCodec;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private boolean mEncoding = false;

    private Surface mSurface;
    private EglCore mEglCore;
    private EGLSurface mEGLSurface;
    private Texture2DRender mTextureRender;

    private VideoDataProcessor mProcessor;

    // 录制线程相关
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private MuxerWrapper mMuxer;

    public void setMuxer(MuxerWrapper muxer) {
        mMuxer = muxer;
    }

    public void startEncode(EncodeConfig encodeConfig) {
        // 开启录制线程
        startEncodeThread();
        Message.obtain(mHandler, MSG_START_RECORD, encodeConfig).sendToTarget();
    }

    public void stopEncode() {
        Message.obtain(mHandler, MSG_STOP_RECORD).sendToTarget();
    }

    public void onVideoFrameUpdate(int textureId) {
        Message.obtain(mHandler, MSG_VIDEO_FRAME_UPDATE, new Object[]{textureId}).sendToTarget();
    }

    private void prepareVideoCodec(EncodeConfig encodeConfig) {
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        // 配置MediaFormat
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, encodeConfig.width, encodeConfig.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodeConfig.videoBitRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, encodeConfig.videoIFrameRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, encodeConfig.videoFrameRate);
        // 创建MediaCodec
        try {
            mVideoCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private void startEncodeInner(EncodeConfig encodeConfig) {
        prepareVideoCodec(encodeConfig);
        // 给当前线程创建EGL环境，并用MediaCodec的Surface为基础创建EGLSurface
        mSurface = mVideoCodec.createInputSurface();
        mEglCore = new EglCore(encodeConfig.sharedContext);
        mEGLSurface = mEglCore.createWindowSurface(mSurface);
        mEglCore.makeCurrent(mEGLSurface);

        // 设置渲染器
        mTextureRender = new Texture2DRender();
        mTextureRender.setFilter(new GPUImageFilter());
        mTextureRender.init();
        mTextureRender.onSizeChanged(encodeConfig.width, encodeConfig.height);

        // start
        mProcessor = new VideoDataProcessor(mVideoCodec, mVideoBufferInfo, mMuxer);
        mProcessor.start();
        mEncoding = true;
    }

    private void startEncodeThread() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        mHandlerThread = new HandlerThread("video_encode");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new EncodeCallback());
    }

    private void stopEncodeInner() {
        Log.i(TAG, "stopEncodeInner -- start");
        mEncoding = false;
        mProcessor.stop();
        release();
        Log.i(TAG, "stopEncodeInner -- end");
    }

    private void onVideoFrameInner(int textureId) {
        if (mEncoding) {
            // 将Texture内容画在MediaCodec的Surface上
            mTextureRender.drawTexture(textureId, null, null);
            mEglCore.swapBuffers(mEGLSurface);
            Log.d(TAG, "swap buffers");
        }
    }

    private void release() {
        mSurface.release();
        // 释放render显存
        mTextureRender.release();

        // 释放EGL
        if (mEglCore != null) {
            mEglCore.release();
            mEGLSurface = null;
            mEglCore = null;
        }

        // 通知Muxer释放Video
        mMuxer.releaseVideo();

        // 停止线程
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    private class EncodeCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    startEncodeInner(((EncodeConfig) msg.obj));
                    break;
                case MSG_STOP_RECORD:
                    stopEncodeInner();
                    break;
                case MSG_VIDEO_FRAME_UPDATE:
                    Object[] data = ((Object[]) msg.obj);
                    int texId = ((Integer) data[0]);
                    onVideoFrameInner(texId);
                    break;
            }
            return true;
        }

    }
}
