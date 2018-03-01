package com.slim.me.camerasample.record.encode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by slimxu on 2018/2/27.
 */

public class AudioEncoder {

    public static final String TAG = "AudioEncoder";

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_COUNT = 1;
    private static final int BIT_RATE = 96000;

    /**
     * AudioFormat.CHANNEL_IN_MONO 单声道 所有设备都支持
     * AudioFormat.CHANNEL_IN_STEREO 双声道
     */
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 音频编码格式
     * AudioFormat.ENCODING_PCM_16BIT 所有设备都支持
     */
    private static final int RECORDER_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 缓冲区大小，这里使用 2*1024
     */
    private static final int BUFFER_ELEMENT_2_REC = 1024;
    private static final int BYTE_PER_ELEMENT = 2;

    private MediaCodec mAACEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaMuxer mMuxer;
    private int mAudioTrackIndex = -1;

    private AudioRecord mAudioRecord;
    private int mBufferSize;

    private boolean mIsRecording;


    public void addAudioTrack(MediaMuxer muxer) {
        mMuxer = muxer;
        mAudioTrackIndex = muxer.addTrack(mAACEncoder.getOutputFormat());
    }

    public void start() {
        // 创建MediaCodec
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        try {
            mAACEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mAACEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAACEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioTrackIndex = -1;

        new RecordThread().start();
    }

    public void drainEncoder(boolean endOfStream) {

    }

    private class RecordThread extends Thread {
        @Override
        public void run() {

            // 创建AudioRecord作为音频源
            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_FORMAT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_FORMAT, BUFFER_ELEMENT_2_REC * BYTE_PER_ELEMENT);
            mAudioRecord.startRecording();

            mIsRecording = true;

            ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_ELEMENT_2_REC);
            int readBytes = -1;
            while (mIsRecording) {
                buf.clear();
                readBytes = mAudioRecord.read(buf, BUFFER_ELEMENT_2_REC);
                if(readBytes > 0) {
                    buf.position(readBytes);
                    buf.flip();
                    encode(buf, readBytes);
                }
            }
        }
    }

    private void encode(ByteBuffer buf, int readBytes) {
        final ByteBuffer[] inputBuffers = mAACEncoder.getInputBuffers();
    }

}
