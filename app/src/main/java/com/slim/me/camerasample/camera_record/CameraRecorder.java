package com.slim.me.camerasample.camera_record;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * 摄像机录制器，里面开启一条录制线程
 */
public class CameraRecorder {

    private static final int MSG_START_RECORD = 1;
    private static final int MSG_STOP_RECORD = 2;
    private static final int MSG_VIDEO_FRAME_UPDATE = 3;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private CameraVideoEncoder mVideoEncoder ;

    public CameraRecorder() {
        mHandlerThread = new HandlerThread("camera_record");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new RecordCallback());

        mVideoEncoder = new CameraVideoEncoder();
    }

    public void startRecord(EncodeConfig encodeConfig) {
        Message.obtain(mHandler, MSG_START_RECORD, encodeConfig).sendToTarget();
    }

    public void stopRecord() {
        Message.obtain(mHandler, MSG_STOP_RECORD).sendToTarget();
    }

    public void onVideoFrameUpdate(int textureId) {
        Message.obtain(mHandler, MSG_VIDEO_FRAME_UPDATE, new Object[]{textureId}).sendToTarget();
    }

    private void handleStartRecord(EncodeConfig encodeConfig) {
        mVideoEncoder.startEncode(encodeConfig);
    }

    private void handleStopRecord() {
        mVideoEncoder.stopEncode();
    }

    private void handleOnVideoFrameUpdate(int textureId) {
        mVideoEncoder.onVideoFrameUpdate(textureId);
    }


    private class RecordCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    handleStartRecord(((EncodeConfig) msg.obj));
                    break;
                case MSG_STOP_RECORD:
                    handleStopRecord();
                    break;
                case MSG_VIDEO_FRAME_UPDATE:
                    Object[] data = ((Object[]) msg.obj);
                    int texId = ((Integer) data[0]);
                    handleOnVideoFrameUpdate(texId);
                    break;
            }
            return true;
        }

    }
}
