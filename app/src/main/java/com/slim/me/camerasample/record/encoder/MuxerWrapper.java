package com.slim.me.camerasample.record.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.annotation.NonNull;
import android.util.Log;

import java.nio.ByteBuffer;

public class MuxerWrapper {

    public static final String TAG = "MuxerWrapper";

    private MediaMuxer mMuxer;

    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;

    private volatile boolean mIsStarted = false;
    private boolean mAudioReleased = false;
    private boolean mVideoReleased = false;

    public MuxerWrapper(@NonNull MediaMuxer muxer) {
        mMuxer = muxer;
    }

    public void addVideoTrack (MediaFormat videoFormat) {
        mVideoTrackIndex = mMuxer.addTrack(videoFormat);
    }

    public void addAudioTrack (MediaFormat audioFormat) {
        mAudioTrackIndex = mMuxer.addTrack(audioFormat);
    }

    public void start() {
        if (mVideoTrackIndex != -1 && mAudioTrackIndex != -1) {
            mMuxer.start();
            mIsStarted = true;
        }
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public void writeAudioData(@NonNull ByteBuffer byteBuf, @NonNull MediaCodec.BufferInfo bufferInfo) {
        mMuxer.writeSampleData(mAudioTrackIndex, byteBuf, bufferInfo);
    }

    public void writeVideoData (@NonNull ByteBuffer byteBuf, @NonNull MediaCodec.BufferInfo bufferInfo) {
        mMuxer.writeSampleData(mVideoTrackIndex, byteBuf, bufferInfo);
    }

    public void releaseAudio() {
        mAudioReleased = true;
        if (mIsStarted && mVideoReleased) {
            release();
        }
    }

    public void releaseVideo() {
        mVideoReleased = true;
        if (mIsStarted && mAudioReleased) {
            release();
        }
    }

    private void release() {
        if (mMuxer != null) {
            try {
                if (mIsStarted) {
                    mIsStarted = false;
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
