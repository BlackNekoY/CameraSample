package com.slim.me.camerasample.camera_record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.util.Log;
import android.view.Surface;

import com.slim.me.camerasample.egl.EglCore;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * 摄像机视频录制器
 */
public class CameraVideoEncoder {

    public static final String TAG = "CameraVideoEncoder";

    private String VIDEO_MIME_TYPE = "video/avc";

    private MediaCodec mVideoCodec;
    private MediaMuxer mMuxer;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private int mVideoTrackIndex = -1;
    private boolean mMuxerStarted = false;
    private boolean mEncoding = false;

    private Surface mSurface;
    private EglCore mEglCore;
    private EGLSurface mEGLSurface;
    private TextureRender mTextureRender;


    public void startEncode(EncodeConfig encodeConfig) {
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

        try {
            mMuxer = new MediaMuxer(encodeConfig.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMuxer.setOrientationHint(encodeConfig.orientation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 给当前线程创建EGL环境，并用MediaCodec的Surface为基础创建EGLSurface
        mSurface = mVideoCodec.createInputSurface();
        mEglCore = new EglCore(encodeConfig.sharedContext);
        mEGLSurface = mEglCore.createWindowSurface(mSurface);
        mEglCore.makeCurrent(mEGLSurface);

        // 设置渲染器
        mTextureRender = new TextureRender();

        // start
        mVideoCodec.start();
        mEncoding = true;
    }

    public void stopEncode() {
        drainEncoder(true);
        mEncoding = false;
        release();
    }

    public void onVideoFrameUpdate(int textureId) {
        if (mEncoding) {
            // 将Texture内容画在MediaCodec的Surface上
            mTextureRender.drawTexture(GLES30.GL_TEXTURE_2D, textureId, null);
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
                mVideoTrackIndex = mMuxer.addTrack(videoFormat);

                if(!mMuxerStarted) {
                    mMuxerStarted = true;
                    mMuxer.start();
                }
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

                if (mVideoBufferInfo.size != 0 && mMuxerStarted) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mVideoBufferInfo.offset);
                    encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                    mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mVideoBufferInfo);

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
        if (mMuxer != null) {
            try {
                if (mMuxerStarted) {
                    mMuxerStarted = false;
                    mMuxer.stop();
                }
                mMuxer.release();
            } catch (Exception e){
                Log.e(TAG, "Muxer stop exception:" + e, e);
            }
            mMuxer = null;
            Log.d(TAG, "release muxer.");
        }
    }
}
