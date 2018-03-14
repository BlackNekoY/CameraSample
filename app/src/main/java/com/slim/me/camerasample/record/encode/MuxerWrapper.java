package com.slim.me.camerasample.record.encode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * Created by slimxu on 2018/3/10.
 */

public class MuxerWrapper {

    public static final String TAG = "MuxerWrapper";

    public static final int TRACK_VIDEO = 1;
    public static final int TRACK_AUDIO = 2;

    private MediaMuxer mMuxer;
    private boolean mMuxerStarted;
    private boolean mIsAudioTrackAdded;
    private boolean mIsVideoTrackAdded;

    private int mVideoTrackIndex;
    private int mAudioTrackIndex;

    private Vector<MuxerData> mMuxerDatas = new Vector<>();
    private Object mLock = new Object();

    private boolean mIsFinish;

    public MuxerWrapper(EncodeConfig encodeConfig) {
        try {
            mMuxer = new MediaMuxer(encodeConfig.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMuxer.setOrientationHint(encodeConfig.orientation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
        mIsVideoTrackAdded = false;
        mIsAudioTrackAdded = false;
        mMuxerStarted = false;
        mIsFinish = false;
        new MuxerThread().start();
    }

    public synchronized void addTrack(int trackType, MediaFormat format) {
        if(mMuxerStarted) {
            return;
        }
        if( (trackType == TRACK_AUDIO && mIsAudioTrackAdded) ||
                (trackType == TRACK_VIDEO && mIsVideoTrackAdded)) {
            return;
        }

        int track = mMuxer.addTrack(format);

        if(trackType == TRACK_VIDEO) {
            mVideoTrackIndex = track;
            mIsVideoTrackAdded = true;
        }

        if(trackType == TRACK_AUDIO) {
            mAudioTrackIndex = track;
            mIsAudioTrackAdded = true;
        }

        requestStartMuxer();
    }

    public void addSampleData(MuxerData data) {
        if(!mMuxerStarted) {
            return;
        }
        synchronized (mLock) {
            mMuxerDatas.add(data);
            mLock.notify();
        }
    }

    private void requestStartMuxer() {
        synchronized (mLock) {
            if(mIsVideoTrackAdded && mIsAudioTrackAdded) {
                mMuxer.start();
                mMuxerStarted = true;
                mLock.notify();
            }
        }
    }


    private void writeSampleData(int trackType, ByteBuffer encodeData, MediaCodec.BufferInfo bufferInfo) {
        if(trackType == TRACK_VIDEO) {
            mMuxer.writeSampleData(mVideoTrackIndex, encodeData, bufferInfo);
        }else if (trackType == TRACK_AUDIO) {
            mMuxer.writeSampleData(mAudioTrackIndex, encodeData, bufferInfo);
        }
    }


    public void release() {
        if(mMuxer != null) {
            if(mMuxerStarted) {
                mMuxerStarted = false;
                mVideoTrackIndex = -1;
                mAudioTrackIndex = -1;
                mIsVideoTrackAdded = false;
                mIsAudioTrackAdded = false;
                mIsFinish = true;

                synchronized (mLock) {
                    mLock.notify();
                }
            }
        }
    }

    public class MuxerThread extends Thread {
        public MuxerThread() {
            super("muxer-thread");
        }

        @Override
        public void run() {
            while (!mIsFinish) {
                if(!mMuxerStarted) {
                    // 未准备Video Audio track
                    Log.d(TAG, "wait add video and audio track.");
                    synchronized (mLock) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    if(mMuxerDatas.isEmpty()) {
                        // 等待数据
                        Log.d(TAG, "datas empty, wait");
                        synchronized (mLock) {
                            try {
                                mLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        // 将数据writeSampleData
                        MuxerData data = mMuxerDatas.remove(0);
                        writeSampleData(data.trackType, data.encodeData, data.bufferInfo);
                        Log.d(TAG, "write sample data, size=" + data.bufferInfo.size);
                    }
                }
            }

            Log.d(TAG, "really stop, release Muxer.");
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    public static class MuxerData {
        public final int trackType;
        public final ByteBuffer encodeData;
        public final MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackType, ByteBuffer encodeData, MediaCodec.BufferInfo bufferInfo) {
            this.trackType = trackType;
            this.encodeData = encodeData;
            this.bufferInfo = bufferInfo;
        }
    }

}
