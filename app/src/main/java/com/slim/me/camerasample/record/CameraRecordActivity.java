package com.slim.me.camerasample.record;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.record.widget.RecorderButton;

import static com.slim.me.camerasample.util.UIUtil.getStatusBarHeight;

public class CameraRecordActivity extends AppCompatActivity implements RecorderButton.OnRecorderButtonListener {

    private CameraRecordView mView;
    private RecorderButton mRecorderButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initStatusBar();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);

        mView = findViewById(R.id.record_view);
        mRecorderButton = findViewById(R.id.record_btn);
        mRecorderButton.setListener(this);
        mRecorderButton.setCanPause(false);
    }

    private void initStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ViewGroup decorViewGroup = (ViewGroup) window.getDecorView();
        View statusBarView = new View(window.getContext());
        int statusBarHeight = getStatusBarHeight(window.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, statusBarHeight);
        params.gravity = Gravity.TOP;
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(Color.BLACK);
        decorViewGroup.addView(statusBarView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        CameraHelper.getInstance().stopPreview();
        CameraHelper.getInstance().releaseCamera();
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
