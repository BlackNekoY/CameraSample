package com.slim.me.camerasample.camera_mpeg;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.preview.CameraSurfaceView;
import com.slim.me.camerasample.preview.SurfacePreviewContext;

/**
 * Created by slimxu on 2018/1/3.
 */

public class CameraToMpegActivity extends AppCompatActivity {

    private FrameLayout mPreviewParent;
    private SurfacePreviewContext mPreviewContext;
    private CameraSurfaceView mCameraPreviewView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_to_mpeg);

        mPreviewParent = (FrameLayout) findViewById(R.id.preview_parent);
        setupCameraPreviewView();
    }

    private void setupCameraPreviewView() {
        mPreviewParent.removeAllViews();

        SurfacePreviewContext previewContext = new CameraMpegPreviewContext(this);
        CameraSurfaceView textureView = new CameraSurfaceView(this);

        textureView.setPreviewContext(previewContext);

        mPreviewContext = previewContext;
        mCameraPreviewView = textureView;

        mPreviewParent.addView(mCameraPreviewView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }
}
