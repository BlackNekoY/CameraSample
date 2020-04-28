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
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.egl.EGL10;


/**
 * 摄像机视频录制器
 */
public class CameraVideoEncoder {

    public static final String TAG = "CameraVideoEncoder";

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

    private void startEncodeInner(EncodeConfig encodeConfig) {

        mVideoBufferInfo = new MediaCodec.BufferInfo();
        // 配置MediaFormat
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, encodeConfig.width, encodeConfig.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodeConfig.bitRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, encodeConfig.iFrameRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, encodeConfig.frameRate);

        // 创建MediaCodec
        try {
            mVideoCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

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
        mVideoCodec.start();
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
        drainEncoder(true);
        mEncoding = false;
        mTextureRender.release();
        release();
        mMuxer.releaseVideo();
    }

    private void onVideoFrameInner(int textureId) {
        if (mEncoding) {
            // 将Texture内容画在MediaCodec的Surface上
            mTextureRender.drawTexture(textureId, null, null);
            mEglCore.swapBuffers(mEGLSurface);
            // 开始编码
            drainEncoder(false);
        }
    }

    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (endOfStream) {
            mVideoCodec.signalEndOfInputStream();
        }
        // 有一些机器，signalEndOfInputStream之后一直收不到BUFFER_FLAG_END_OF_STREAM，导致录制无法结束。这里添加一个计数，如果连续100次dequeueOutputBuffer还没有结束，就直接抛出异常。
        int endTryTimes = 0;
        ByteBuffer[] encoderOutputBuffers = mVideoCodec.getOutputBuffers();
        while (true) {
            int encoderStatus = mVideoCodec.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 当前队列数据已处理完，等待surface更新，跳出循环。
                if (!endOfStream) {
                    Log.d(TAG, "VideoCodec: no output available yet");
                    break;      // out of while
                } else {
                    Log.d(TAG, "VideoCodec: no output available, spinning to await EOS");
                    endTryTimes++;
                    if (endTryTimes > 100) {
                        throw new RuntimeException("VideoCodec: Encoder is not stopped after dequeue 100 times.");
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mVideoCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 只有在第一次写入视频时会到这里
                MediaFormat videoFormat = mVideoCodec.getOutputFormat();
                Log.d(TAG, "VideoCodec: encoder output format changed: " + videoFormat);
                mMuxer.addVideoTrack(videoFormat);
                mMuxer.start();
            } else if (encoderStatus < 0) {
                // 其他未知错误，忽略
                Log.w(TAG, "VideoCodec: unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                // 如果有收到surface更新，就将endTryTimes清0。
                endTryTimes = 0;

                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("VideoCodec: encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "VideoCodec: ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mVideoBufferInfo.size = 0;
                }

                if (mVideoBufferInfo.size != 0 && mMuxer.isStarted()) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mVideoBufferInfo.offset);
                    encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                    mMuxer.writeVideoData(encodedData, mVideoBufferInfo);

                    Log.d(TAG, "VideoCodec: sent " + mVideoBufferInfo.size + " bytes to muxer, ts=" +
                            mVideoBufferInfo.presentationTimeUs * 1000);
                }

                mVideoCodec.releaseOutputBuffer(encoderStatus, false);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "VideoCodec: reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "VideoCodec: end of stream reached");
                    }
                    break;
                }
            }
        }
    }

    private void release() {
        // 释放VideoCodec
        if (mVideoCodec != null) {
            try {
                mVideoCodec.stop();
            } catch (Exception e) {
                Log.w(TAG, "mVideoCodec stop exception:" + e);
            }
            try {
                mVideoCodec.release();
            } catch (Exception e) {
                Log.w(TAG, "mVideoCodec release exception:" + e);
            }
            mVideoCodec = null;
            Log.d(TAG, "release video codec.");
        }
        // 停止线程
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        // 释放EGL
        if (mEglCore != null) {
            mEglCore.release();
            mEGLSurface = null;
            mEglCore = null;
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
