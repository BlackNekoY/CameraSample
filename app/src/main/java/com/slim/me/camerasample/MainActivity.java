package com.slim.me.camerasample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.slim.me.camerasample.camera.preview.CameraPreviewActivity;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void gotoCameraPreviewActivity(View view) {
        Intent intent = new Intent(this, CameraPreviewActivity.class);
        startActivity(intent);
    }

    public void gotoCameraRecordActivity(View view) {
        startActivity(new Intent(this, com.slim.me.camerasample.camera_record.CameraRecordActivity.class));
    }
}
