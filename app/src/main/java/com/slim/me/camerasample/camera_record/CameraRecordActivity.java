package com.slim.me.camerasample.camera_record;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.camera.CameraHelper;

public class CameraRecordActivity extends AppCompatActivity {

    private CameraRecordView mView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);

        mView = findViewById(R.id.record_view);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraHelper.getInstance().stopPreview();
        CameraHelper.getInstance().releaseCamera();
    }

    public void startRecord(View view) {
        mView.startRecord(!mView.isRecording());
        if (mView.isRecording()) {
            ((Button) view).setText("停止录制");
        } else {
            ((Button) view).setText("开始录制");
        }
    }
}
