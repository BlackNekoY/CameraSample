package com.slim.me.camerasample.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by slimxu on 2017/11/19.
 */

public class VideoEncoder {

    public static final String TAG = "VideoEncoder";

    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding

    private MediaCodec mVideoEncoder;
    private Surface mInputSurface;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private int mVideoTrackIndex;

    private boolean mMuxerStarted;
    private MediaMuxer mMuxer;

    public void start(EncodeConfig encodeConfig) throws IOException {
        MediaFormat format = MediaFormat.createAudioFormat(VIDEO_MIME_TYPE, encodeConfig.width, encodeConfig.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodeConfig.bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, encodeConfig.frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, encodeConfig.iFrameRate);

        mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        try {
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch (Exception e) {
            throw e;
        }

        mInputSurface = mVideoEncoder.createInputSurface();
        mVideoEncoder.start();

        mMuxerStarted = false;
        mMuxer = new MediaMuxer(encodeConfig.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mMuxer.setOrientationHint(encodeConfig.orientation);

        mVideoTrackIndex = -1;

    }

    public void stop() {
        // TODO 处理
        drainEncoder(true);
        release();
    }

    private void release() {

    }

    private void drainEncoder(boolean endOfStream) {
        final int TIME_OUT_USEC = 10000;
        if(endOfStream) {
            Log.d(TAG, "sending EOS to encoder.");
            mVideoEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] videoEncoderOutputBuffers = mVideoEncoder.getOutputBuffers();
        while (true) {
            // 取出输出Buffer中被填充好的数据
            // @return: 返回某个INFO_XXX状态，或者是outputBuffer中成功decode的数据index
            int videoEncoderStatus = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIME_OUT_USEC);
            if(videoEncoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 指示dequeueOutputBuffer()操作Time Out了。
                // 输出缓冲区已经没有数据，等待Surface更新
                if(!endOfStream) {
                    Log.d(TAG, "no output available yet");
                    break;
                }else {
                    Log.d(TAG, "no output available, spinning to await EOS");
                }
            }else if(videoEncoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // 输出缓冲区数据有变化，拿出最新的
                videoEncoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            }else if(videoEncoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // OutputFormat发生变化，一般第一次才会进来
                Log.i(TAG, "video format changed " + mVideoEncoder.getOutputFormat());
                if(mMuxerStarted) {
                    throw new RuntimeException("format change twice.");
                }
                setVideoTraceIndex();
                // TODO 启动AudioEncoder
                mMuxer.start();
                mMuxerStarted = true;
            }else if(videoEncoderStatus < 0) {
                // 其他未知错误，忽略
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + videoEncoderStatus);
            }else {
                // 因为循环，所以这里拿到的是上一次循环的OutputBuffer
                // 拿出可用的数据
                ByteBuffer encodeData = videoEncoderOutputBuffers[videoEncoderStatus];
                if(encodeData == null) {
                    throw new RuntimeException("encodeOutputBuffer " + videoEncoderStatus + " was null.");
                }
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if(mBufferInfo.size != 0) {
                        if(!mMuxerStarted) {
                            throw new RuntimeException("muxer hasn't started.");
                        }

                        // adjust the ByteBuffer values to match BufferInfo
                        // 拿到的数据结合BufferInfo，规定一下能使用的范围:offset ~ offset + size
                        encodeData.position(mBufferInfo.offset);
                        encodeData.limit(mBufferInfo.offset + mBufferInfo.size);
                        // Muxer将编码过的数据写入输出缓冲区
                        mMuxer.writeSampleData(mVideoTrackIndex, encodeData, mBufferInfo);
                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs / 1000);
                    }
                }

                mVideoEncoder.releaseOutputBuffer(videoEncoderStatus, false);

                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if(!endOfStream) {
                        // 取出的BufferInfo已经结束了，但是传入的endOfStream参数还未结束
                        Log.w(TAG, "reached end of stream unexpectedly");
                    }else {
                        Log.i(TAG, "end of stream reached");
                    }
                    // 退出
                    break;
                }
            }
        }
    }

    private void setVideoTraceIndex() {
        MediaFormat newFormat = mVideoEncoder.getOutputFormat();
        // 获取VideoTrace的Index，启动MediaMuxer
        mVideoTrackIndex = mMuxer.addTrack(newFormat);
        Log.d(TAG, "video track: " + mVideoTrackIndex);
    }
}
