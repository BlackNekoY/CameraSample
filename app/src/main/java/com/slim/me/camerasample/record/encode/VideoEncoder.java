package com.slim.me.camerasample.record.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;


import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by slimxu on 2018/1/4.
 */

public class VideoEncoder {

    public static final String TAG = "VideoEncoder";

    private static final String MIME_TYPE = "video/avc";

    private MediaCodec mEncoder;
    private Surface mInputSurface;  // MediaCodec的Surface，从这里拿到更新的Frame并编码成视频
    private MediaCodec.BufferInfo mBufferInfo;

    private EncodeConfig mEncodeConfig;

    private AudioEncoder mAudioEncoder;
    private MuxerWrapper mMuxerWrapper;

    public void start(EncodeConfig encodeConfig) throws IOException {
        mEncodeConfig = encodeConfig;

        mBufferInfo = new MediaCodec.BufferInfo();

        // 配置MediaFormat
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mEncodeConfig.width, mEncodeConfig.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mEncodeConfig.bitRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mEncodeConfig.iFrameRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mEncodeConfig.frameRate);
        Log.d(TAG, " encoder format : " + format);

        // 创建MediaCodec
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);

        // configure
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        // 获取Surface
        mInputSurface = mEncoder.createInputSurface();

        // start
        mEncoder.start();
    }

    public void stop() {
        // 先将队列中的数据处理完毕，再停止
        drainEncoder(true);
        release();

    }

    public void release() {
        if(mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }

        if(mAudioEncoder != null) {
            mAudioEncoder.release();
        }

        if(mMuxerWrapper != null) {
            mMuxerWrapper.release();
        }

    }

    public void frameAvaliable() {
        drainEncoder(false);
    }


    /**
     * 对encoder队列中的数据进行编码。核心逻辑。
     * @param endOfStream，录制是否结束
     */
    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        Log.d(TAG, "drainEncoder(" + endOfStream + ")");
        if (endOfStream) {
            Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }
        // 有一些机器，signalEndOfInputStream之后一直收不到BUFFER_FLAG_END_OF_STREAM，导致录制无法结束。这里添加一个计数，如果连续100次dequeueOutputBuffer还没有结束，就直接抛出异常。
        int endTryTimes = 0;
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 当前队列数据已处理完，等待surface更新，跳出循环。
                if (!endOfStream) {
                    Log.d(TAG, "no output available yet");
                    break;      // out of while
                } else {
                    Log.d(TAG, "no output available, spinning to await EOS");
                    endTryTimes++;
                    if (endTryTimes > 100) {
                        throw new RuntimeException("Encoder is not stopped after dequeue 100 times.");
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 只有在第一次写入视频时会到这里
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);
                mMuxerWrapper.addTrack(MuxerWrapper.TRACK_VIDEO, newFormat);
            } else if (encoderStatus < 0) {
                // 其他未知错误，忽略
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                // 如果有收到surface更新，就将endTryTimes清0。
                endTryTimes = 0;

                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxerWrapper.addSampleData(new MuxerWrapper.MuxerData(MuxerWrapper.TRACK_VIDEO, encodedData, mBufferInfo));
                    Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                            mBufferInfo.presentationTimeUs * 1000);
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "end of stream reached");
                    }
                    break;
                }
            }
        }
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

}
