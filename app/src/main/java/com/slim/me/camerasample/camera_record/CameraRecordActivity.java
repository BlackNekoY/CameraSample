package com.slim.me.camerasample.camera_record;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.camera.CameraHelper;

public class CameraRecordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraHelper.getInstance().stopPreview();
    }
}
