package com.slim.me.camerasample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.slim.me.camerasample.camera_mpeg.CameraToMpegActivity;
import com.slim.me.camerasample.encode_mux.EncodeAndMuxActivity;
import com.slim.me.camerasample.preview.CameraPreviewActivity;
import com.slim.me.camerasample.eglsurface.EGLSurfaceActivity;

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

    public void gotoEGLSurfaceActivity(View view) {
        Intent intent = new Intent(this, EGLSurfaceActivity.class);
        startActivity(intent);
    }

    public void gotoEncodeAndMuxActivity(View view) {
        Intent intent = new Intent(this, EncodeAndMuxActivity.class);
        startActivity(intent);
    }

    public void gotoCameraMpegActivity(View view) {
        Intent intent = new Intent(this, CameraToMpegActivity.class);
        startActivity(intent);
    }
}
