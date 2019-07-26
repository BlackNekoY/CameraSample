package com.slim.me.camerasample.camera_record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 摄像机音频录制器
 */
public class CameraAudioEncoder {

    public static final String TAG = "CameraAudioEncoder";

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_COUNT = 1;
    private static final int BIT_RATE = 96000;
    private static final int BUFFER_SIZE = 1024;

    private MediaCodec mAudioCodec;
    private MediaCodec.BufferInfo mAudioBuffInfo;

    private AudioEncodeThread mThread;
    private AudioRecord mAudioRecord;
    private int mBufferSize;
    private volatile boolean mIsRecording;

    private MuxerWrapper mMuxer;

    public void setMuxer(MuxerWrapper muxer) {
        mMuxer = muxer;
    }

    public void startEncode(EncodeConfig config) {
        mIsRecording = true;
        prepareCodec();
        prepareEncodeThread();
    }

    public void stopEncode() {
        Log.d(TAG, "stopEncode");
        mIsRecording = false;
    }

    private void prepareCodec() {
        try {
            mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT);
            audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);
            mAudioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            mAudioBuffInfo = new MediaCodec.BufferInfo();

            mAudioCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            mAudioCodec = null;
            mAudioBuffInfo = null;
        }
    }

    private void prepareEncodeThread() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
            mThread = null;
        }
        mThread = new AudioEncodeThread();
        mThread.start();
    }

    private void onAudioDataReady(ByteBuffer buffer, int readSize) {
        // 将data送往InputBuffer编码
        final long TIMEOUT = 0;
        int inputBufferIndex = mAudioCodec.dequeueInputBuffer(TIMEOUT);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(inputBufferIndex);
            if (inputBuffer != null) {
                inputBuffer.put(buffer);
                long audioAbsolutePtsUs = (System.nanoTime()) / 1000L;
                mAudioCodec.queueInputBuffer(inputBufferIndex, 0, readSize, audioAbsolutePtsUs, 0);
            }
        }
        drainEncoder(false);
    }

    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
//        if (endOfStream) {
//            mAudioCodec.signalEndOfInputStream();
//        }
        // 有一些机器，signalEndOfInputStream之后一直收不到BUFFER_FLAG_END_OF_STREAM，导致录制无法结束。这里添加一个计数，如果连续100次dequeueOutputBuffer还没有结束，就直接抛出异常。
        int endTryTimes = 0;
        ByteBuffer[] encoderOutputBuffers = mAudioCodec.getOutputBuffers();
        while (true) {
            int encoderStatus = mAudioCodec.dequeueOutputBuffer(mAudioBuffInfo, TIMEOUT_USEC);
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
                encoderOutputBuffers = mAudioCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 只有在第一次写入视频时会到这里
                MediaFormat videoFormat = mAudioCodec.getOutputFormat();
                Log.d(TAG, "VideoCodec: encoder output format changed: " + videoFormat);
                mMuxer.addAudioTrack(videoFormat);
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

                if ((mAudioBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "VideoCodec: ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mAudioBuffInfo.size = 0;
                }

                if (mAudioBuffInfo.size != 0 && mMuxer.isStarted()) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mAudioBuffInfo.offset);
                    encodedData.limit(mAudioBuffInfo.offset + mAudioBuffInfo.size);
                    mMuxer.writeAudioData(encodedData, mAudioBuffInfo);

                    Log.d(TAG, "VideoCodec: sent " + mAudioBuffInfo.size + " bytes to muxer, ts=" +
                            mAudioBuffInfo.presentationTimeUs * 1000);
                }

                mAudioCodec.releaseOutputBuffer(encoderStatus, false);

                if ((mAudioBuffInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
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
        if (mAudioCodec != null) {
            try {
                mAudioCodec.stop();
            } catch (Exception e) {
                Log.w(TAG, "mVideoCodec stop exception:" + e);
            }
            try {
                mAudioCodec.release();
            } catch (Exception e) {
                Log.w(TAG, "mVideoCodec release exception:" + e);
            }
            mAudioCodec = null;
            Log.d(TAG, "release video codec.");
        }
    }

    private class AudioEncodeThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // 创建AudioRecord作为音频源
            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE * 2);
            mAudioRecord.startRecording();

            ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
            int readBytes = -1;
            while (mIsRecording) {
                buf.clear();
                readBytes = mAudioRecord.read(buf, BUFFER_SIZE);
                if(readBytes > 0) {
                    buf.position(readBytes);
                    buf.flip();
                    onAudioDataReady(buf, readBytes);
                }
            }

            // 退出录制 释放资源
            drainEncoder(true);
            release();
            mMuxer.releaseAudio();
        }
    }

}
