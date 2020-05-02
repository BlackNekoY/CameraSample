package com.slim.me.camerasample.record.layer.record;

import android.media.MediaMuxer;
import android.util.Log;

import com.slim.me.camerasample.record.encoder.CameraAudioEncoder;
import com.slim.me.camerasample.record.encoder.CameraVideoEncoder;
import com.slim.me.camerasample.record.encoder.EncodeConfig;
import com.slim.me.camerasample.record.encoder.MuxerWrapper;

import java.io.IOException;

public class CameraRecorder {

    public static final String TAG = "CameraRecorder";

    private CameraVideoEncoder mVideoEncoder;
    private CameraAudioEncoder mAudioEncoder;
    private MuxerWrapper mMuxerWrapper;

    public CameraRecorder() {
        mVideoEncoder = new CameraVideoEncoder();
        mAudioEncoder = new CameraAudioEncoder();
    }

    public void startRecord(EncodeConfig encodeConfig) {
        prepareMuxer(encodeConfig);

        mVideoEncoder.setMuxer(mMuxerWrapper);
        mAudioEncoder.setMuxer(mMuxerWrapper);

        mVideoEncoder.startEncode(encodeConfig);
        mAudioEncoder.startEncode(encodeConfig);
    }

    public void stopRecord() {
        Log.i(TAG, "CameraRecorder: stopRecord");
        mVideoEncoder.stopEncode();
        mAudioEncoder.stopEncode();
    }

    public void onVideoFrameUpdate(int textureId) {
        mVideoEncoder.onVideoFrameUpdate(textureId);
    }

    private void prepareMuxer(EncodeConfig encodeConfig) {
        try {
            MediaMuxer muxer = new MediaMuxer(encodeConfig.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            muxer.setOrientationHint(encodeConfig.orientation);
            mMuxerWrapper = new MuxerWrapper(muxer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
