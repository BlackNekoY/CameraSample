package com.slim.me.camerasample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.slim.me.camerasample.record.CameraRecordActivity;
import com.slim.me.camerasample.encode_test.EncodeAndMuxActivity;
import com.slim.me.camerasample.camera.preview.CameraPreviewActivity;
import com.slim.me.camerasample.surfaceview_with_egl.EGLSurfaceActivity;
import com.slim.me.camerasample.texture_test.TextureTestActivity;

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
        Intent intent = new Intent(this, CameraRecordActivity.class);
        startActivity(intent);
    }

    public void gotoTextureTestActivity(View view) {
        Intent intent = new Intent(this, TextureTestActivity.class);
        startActivity(intent);
    }
}
