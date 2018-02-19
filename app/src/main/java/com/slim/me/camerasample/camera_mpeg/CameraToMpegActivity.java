package com.slim.me.camerasample.camera_mpeg;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.encoder.EncodeConfig;

import java.io.File;

/**
 * Created by slimxu on 2018/1/3.
 */

public class CameraToMpegActivity extends AppCompatActivity implements View.OnClickListener {

    private FrameLayout mPreviewParent;
    private Button mRecordBtn;
    private CameraGLSurfaceView mCameraPreviewView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_to_mpeg);

        mPreviewParent = (FrameLayout) findViewById(R.id.preview_parent);
        mRecordBtn = (Button) findViewById(R.id.record);
        mRecordBtn.setOnClickListener(this);
        setupCameraPreviewView();
    }

    private void setupCameraPreviewView() {
        mPreviewParent.removeAllViews();
        mCameraPreviewView = new CameraGLSurfaceView(this);
        mCameraPreviewView.getRender().setEncodeConfig(new EncodeConfig(
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "video-" + System.currentTimeMillis() + ".mp4",
                360, 640,
                2 * 1024 * 1024,
                1,
                30,
                0));
        mPreviewParent.addView(mCameraPreviewView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record:
                mCameraPreviewView.getRender().mRecordingEnabled = !mCameraPreviewView.getRender().mRecordingEnabled;
                if(mCameraPreviewView.getRender().mRecordingEnabled) {
                    mRecordBtn.setText("停止录制");
                }else {
                    mRecordBtn.setText("开始录制");
                }
                break;
        }
    }
}
