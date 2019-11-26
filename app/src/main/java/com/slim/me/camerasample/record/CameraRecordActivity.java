package com.slim.me.camerasample.record;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.record.widget.RecorderButton;

public class CameraRecordActivity extends AppCompatActivity implements RecorderButton.OnRecorderButtonListener {

    private CameraRecordView mView;
    private RecorderButton mRecorderButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);

        mView = findViewById(R.id.record_view);
        mRecorderButton = findViewById(R.id.record_btn);
        mRecorderButton.setListener(this);
        mRecorderButton.setCanPause(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraHelper.getInstance().stopPreview();
        CameraHelper.getInstance().releaseCamera();
    }

    @Override
    public boolean onStartRecorder() {
        mView.startRecord();
        return true;
    }

    @Override
    public void onStopRecorder(boolean isLongClick) {
        mView.stopRecord();
    }

    @Override
    public boolean onHoldRecorder() {
        mView.startRecord();
        return true;
    }

    @Override
    public void onCountDownStart() {

    }

    @Override
    public void onFinish(boolean isLongClick) {
        mView.stopRecord();
    }
}
