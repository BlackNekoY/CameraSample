package com.slim.me.camerasample.record;

import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.camera.CameraHelper;

import static com.slim.me.camerasample.util.UIUtil.getStatusBarHeight;

public class CameraRecordActivity extends AppCompatActivity {

    private PreviewLayout mPreviewLayout;
    private OrientationEventListener mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initStatusBar();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mPreviewLayout = findViewById(R.id.preview);
        mListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
//                if (!CameraHelper.getInstance().isPreviewing()) {
//                    return;
//                }
//                final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//                switch (wm.getDefaultDisplay().getRotation()) {
//                    case Surface.ROTATION_0:
//                        CameraHelper.getInstance().setDisplayOrientation(90);
//                        break;
//                    case Surface.ROTATION_90:
//                        CameraHelper.getInstance().setDisplayOrientation(0);
//                        break;
//                    case Surface.ROTATION_180:
//                        CameraHelper.getInstance().setDisplayOrientation(270);
//                        break;
//                    case Surface.ROTATION_270:
//                        CameraHelper.getInstance().setDisplayOrientation(180);
//                        break;
//                }
            }
        };
        if (mListener.canDetectOrientation()) {
            mListener.enable();
        } else {
            mListener.disable();
        }
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
        statusBarView.setBackgroundColor(Color.TRANSPARENT);
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
        mPreviewLayout.onDestroy();
        mListener.disable();
    }
}
