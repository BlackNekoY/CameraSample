package com.slim.me.camerasample.record;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.slim.me.camerasample.record.encode.EncodeConfig;
import com.slim.me.camerasample.record.encode.EncodeInputSurface;
import com.slim.me.camerasample.record.encode.VideoEncoder;

import java.io.IOException;

/**
 * Created by slimxu on 2018/1/8.
 */

public class CameraRecorder {

    public static final int MSG_START_RECORD = 1;
    public static final int MSG_ON_FRAME_AVAILABLE = 2;
    public static final int MSG_STOP_RECORD = 3;

    private VideoEncoder mEncoder;
    private EncodeInputSurface mInputSurface;

    private HandlerThread mRecordThread;
    private RecordHandler mRecordHandler;

    private volatile boolean mIsRecording = false;

    public CameraRecorder() {
        mRecordThread = new HandlerThread("recode_thread");
        mRecordThread.start();
        mRecordHandler = new RecordHandler(mRecordThread.getLooper());

        mEncoder = new VideoEncoder();
        mInputSurface = new EncodeInputSurface();
    }

    public void startRecord(EncodeConfig encodeConfig) {
        Message msg = Message.obtain();
        msg.what = MSG_START_RECORD;
        msg.obj = encodeConfig;
        mRecordHandler.sendMessage(msg);
    }

    public void onFrameAvailable(int textureType, int textureId, float[] textureMatrix, float[] mvpMatrix, long timestamp) {
        Message msg = Message.obtain();
        msg.what = MSG_ON_FRAME_AVAILABLE;
        Object[] args = new Object[5];
        args[0] = textureType;
        args[1] = textureId;
        args[2] = textureMatrix;
        args[3] = mvpMatrix;
        args[4] = timestamp;
        msg.obj = args;
        mRecordHandler.sendMessage(msg);
    }

    public void stopRecord() {
        Message msg = Message.obtain();
        msg.what = MSG_STOP_RECORD;
        mRecordHandler.sendMessage(msg);
    }

    private void handleStartRecord(EncodeConfig encodeConfig) {
        if(mIsRecording) {
            handleStopRecord();
        }
        mIsRecording = true;
        try {
            mEncoder.start(encodeConfig);
            mInputSurface.init(encodeConfig.sharedContext, mEncoder.getInputSurface());
        } catch (IOException e) {
            mIsRecording = false;
            return;
        }
    }

    private void handleOnFrameAvailable(int textureType, int textureId, float[] textureMatrix, float[] mvpMatrix, long timestampNanos){
        mEncoder.frameAvaliable();
        mInputSurface.draw(textureType, textureId, textureMatrix, mvpMatrix, timestampNanos);
    }
    private void handleStopRecord(){
        mEncoder.stop();
        mInputSurface.release();
        mIsRecording = false;
    }


    private class RecordHandler extends Handler {

        public RecordHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    EncodeConfig config = (EncodeConfig) msg.obj;
                    handleStartRecord(config);
                    break;
                case MSG_ON_FRAME_AVAILABLE:
                    Object[] args = (Object[]) msg.obj;
                    handleOnFrameAvailable((int) args[0], (int) args[1], (float[])args[2],(float[]) args[3], (long)args[4]);
                    break;
                case MSG_STOP_RECORD:
                    handleStopRecord();
                    break;
                default:
                    break;
            }
        }
    }

}
