package com.slim.me.camerasample.record.encode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.slim.me.camerasample.record.CameraRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by slimxu on 2018/3/15.
 */

public class VideoAudioEncoder {

    public static final String TAG = "VideoAudioEncoder";

    // Video
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private MediaCodec mVideoCodec;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private Surface mSurface;

    // Audio
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_COUNT = 1;
    private static final int BIT_RATE = 96000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_ELEMENT_2_REC = 1024;
    private static final int BYTE_PER_ELEMENT = 2;
    private int mBufferSize;
    private AudioRecord mAudioRecord;
    private MediaCodec mAudioCodec;
    private MediaCodec.BufferInfo mAudioBufferInfo;
    private long mLastAudioPts; // 最后一帧时间戳

    // Muxer
    private MediaMuxer mMediaMuxer;
    private int mVideoTrackIndex;
    private int mAudioTrackIndex;
    private boolean mHasVideoAdded;
    private boolean mHasAudioAdded;
    private boolean mMuxerStarted;
    private volatile boolean mIsRecording;

    private Handler mRecordHandler;
    private EncodeConfig mEncodeConfig;
    private final boolean ENABLE_AUDIO = true;

    public void startEncode(EncodeConfig encodeConfig, Handler recordHandler) throws IOException {
        mEncodeConfig = encodeConfig;
        mRecordHandler = recordHandler;

        prepareVideoCodec(encodeConfig);
        if (ENABLE_AUDIO) {
            prepareAudioCodec(encodeConfig);
        }

        mMediaMuxer = new MediaMuxer(encodeConfig.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mMediaMuxer.setOrientationHint(encodeConfig.orientation);

        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
        mHasAudioAdded = false;
        mHasVideoAdded = false;
        mMuxerStarted = false;
        mIsRecording = true;

        if(ENABLE_AUDIO) {
            new RecordThread().start();
        }
    }

    private void prepareAudioCodec(EncodeConfig encodeConfig) throws IOException {
        // 创建MediaCodec
        mAudioBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        mAudioCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        mAudioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioCodec.start();
    }

    private void prepareVideoCodec(EncodeConfig encodeConfig) throws IOException {

        mVideoBufferInfo = new MediaCodec.BufferInfo();

        // 配置MediaFormat
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, encodeConfig.width, encodeConfig.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodeConfig.bitRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, encodeConfig.iFrameRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, encodeConfig.frameRate);
        Log.d(TAG, " encoder format : " + format);

        // 创建MediaCodec
        mVideoCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        // configure
        mVideoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 获取Surface
        mSurface = mVideoCodec.createInputSurface();
        // start
        mVideoCodec.start();
    }


    /**
     * 编码视频数据
     * 因为视频数据是通过Surface直接送入OutputBuffer的，所以直接取用编码
     */
    private void drainVideoEncoder(boolean endOfStream) {
        Log.d(TAG, "drainVideoEncoder(" + endOfStream + ")");
        final int TIMEOUT_USEC = 10000;
        if (endOfStream) {
            Log.d(TAG, "VideoCodec: sending EOS.");
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
                mVideoTrackIndex = mMediaMuxer.addTrack(videoFormat);

                if(!mMuxerStarted && ( (ENABLE_AUDIO && mHasAudioAdded) || !ENABLE_AUDIO )) {
                    mMuxerStarted = true;
                    mMediaMuxer.start();
                }
                mHasVideoAdded = true;
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
                    mMediaMuxer.writeSampleData(mVideoTrackIndex, encodedData, mVideoBufferInfo);

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

    /**
     * 编码音频数据
     * 需要外部传入数据
     * @param endOfStream
     */
    private void drainAudioEncoder(PCMFrame frame, boolean endOfStream) {
        Log.d(TAG, "drainAudioEncoder(" + endOfStream + ")");
        final int TIMEOUT_USEC = 10000;
        int endTryTimes = 0;
        // 先往AudioCodec中写数据
        ByteBuffer[] inputBuffers = mAudioCodec.getInputBuffers();
        while(true) {
            final int inputBufferIndex = mAudioCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                // 有可用的InputBuffer
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(frame.data);
                inputBuffer.position(frame.data.length);
                inputBuffer.flip();

                mAudioCodec.queueInputBuffer(inputBufferIndex, 0, frame.data.length, frame.pts, 0);
                
                break;
            }else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // TODO 这里什么意思？
                if (!endOfStream) {
                    break;
                }else {
                    endTryTimes++;
                    if(endTryTimes > 10) {
                        break;
                    }
                }
            }
        }
        
        // 再往Muxer中写数据
        ByteBuffer[] outputBuffers = mAudioCodec.getOutputBuffers();
        endTryTimes = 0;
        while (true) {
            int encoderStatus = mAudioCodec.dequeueOutputBuffer(mAudioBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                } else {
                    endTryTimes++;
                    if (endTryTimes > 10) {
                        break;
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mAudioCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mHasAudioAdded) {
                    throw new IllegalStateException("audio start twice.");
                }
                MediaFormat audioFormat = mAudioCodec.getOutputFormat();
                Log.d(TAG, "AudioCodec: encoder output format changed: " + audioFormat);
                mAudioTrackIndex = mMediaMuxer.addTrack(audioFormat);
                
                if (!mMuxerStarted && mHasVideoAdded) {
                    mMuxerStarted = true;
                    mMediaMuxer.start();
                }
                mHasAudioAdded = true;
            } else if (encoderStatus < 0) {
                Log.d(TAG, "AudioCodec: unexpected result from encoder.dequeueOutputBuffer:" + encoderStatus);
            } else {
                endTryTimes = 0;
                ByteBuffer encodedData = outputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("AudioCodec: encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mAudioBufferInfo.size = 0;
                }

                if (mAudioBufferInfo.size != 0 && mMuxerStarted) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mAudioBufferInfo.offset);
                    encodedData.limit(mAudioBufferInfo.offset + mAudioBufferInfo.size);
//                    mBufferInfo.presentationTimeUs = frame.pts/1000;

                    Log.d(TAG, "AudioCodec: before writeSampleData audio " + mAudioBufferInfo.size
                            + ", offset=" + mAudioBufferInfo.offset
                            + " to muxer, ts=" + mAudioBufferInfo.presentationTimeUs);

                    if(mAudioBufferInfo.presentationTimeUs >= mLastAudioPts) {
                        mLastAudioPts = mAudioBufferInfo.presentationTimeUs;
                        mMediaMuxer.writeSampleData(mAudioTrackIndex, encodedData, mAudioBufferInfo);
                        Log.d(TAG, "AudioCodec: end writeSampleData");
                    } else {
                        Log.e(TAG, "AudioCodec: handleAudioFrame, find older frame");
                    }
                }
                mAudioCodec.releaseOutputBuffer(encoderStatus, false);

                if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "AudioCodec: reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "AudioCodec: end of stream reached");
                    }
                    break;
                }
            }
        }
    }

    public void onVideoFrameAvailable() {
        if(mIsRecording) {
            drainVideoEncoder(false);
        }
    }

    public void onAudioFrameAvailable(PCMFrame pcmFrame) {
        if(mIsRecording) {
            drainAudioEncoder(pcmFrame, false);
        }
    }
    
    public void stop() {
        Log.d(TAG, "stop.");

        drainVideoEncoder(true);
        if(ENABLE_AUDIO) {
            drainAudioEncoder(new PCMFrame(new byte[]{}, -1, mLastAudioPts), true);
        }

        mIsRecording = false;
        release();
    }

    private void release() {
        if (mAudioCodec != null && ENABLE_AUDIO) {
            try {
                mAudioCodec.stop();
            } catch (Exception e) {
                Log.w(TAG, "mAudioCodec stop exception:" + e);
            }
            try {
                mAudioCodec.release();
            } catch (Exception e) {
                Log.w(TAG, "mAudioCodec release exception:" + e);
            }
            mAudioCodec = null;
            Log.d(TAG, "release audio codec.");
        }
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
        if (mMediaMuxer != null) {
            try {
                if (mMuxerStarted) {
                    mMuxerStarted = false;
                    mMediaMuxer.stop();
                }
                mMediaMuxer.release();
            } catch (Exception e){
                Log.e(TAG, "Muxer stop exception:" + e, e);
            }
            mMediaMuxer = null;
            Log.d(TAG, "release muxer.");
        }
    }

    public Surface getInputSurface() {
        return mSurface;
    }

    public static class PCMFrame {
        final byte[] data;
        final int readBytes;
        final long pts;

        public PCMFrame(byte[] data, int readBytes, long pts) {
            this.data = data;
            this.readBytes = readBytes;
            this.pts = pts;
        }
    }

    private class RecordThread extends Thread {
        @Override
        public void run() {

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // 创建AudioRecord作为音频源
            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_FORMAT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_FORMAT, BUFFER_ELEMENT_2_REC * BYTE_PER_ELEMENT);
            mAudioRecord.startRecording();

            int readBytes = -1;
            while (mIsRecording) {
                byte[] audioDatas = new byte[BUFFER_ELEMENT_2_REC * BYTE_PER_ELEMENT];
                readBytes = mAudioRecord.read(audioDatas, 0, BUFFER_ELEMENT_2_REC * BYTE_PER_ELEMENT);
                if(readBytes > 0) {
                    Message msg = Message.obtain();
                    msg.what = CameraRecorder.MSG_AUDIO_FRAME_AVAILABLE;
                    msg.obj = new PCMFrame(audioDatas, readBytes, System.nanoTime() / 1000);
                    mRecordHandler.sendMessage(msg);
                }
            }

            Log.d(TAG, "RecordThread really stop.");
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }
}
