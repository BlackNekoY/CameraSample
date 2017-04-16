package com.slim.me.camerasample;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.slim.me.camerasample.camera.CameraHelper;

import java.io.IOException;

/**
 * Created by Slim on 2017/3/20.
 */

public class SurfaceTextureActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {

    private SurfaceTexture mSurfaceTexture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_texture);

        mSurfaceTexture = new SurfaceTexture(1);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);

        Camera camera = CameraHelper.getInstance().getCamera();
        if(camera != null) {
            Camera.Parameters param = camera.getParameters();
            param.setPreviewFormat(ImageFormat.YV12);
            camera.setDisplayOrientation(90);
            camera.setParameters(param);

            try {
                camera.setPreviewTexture(mSurfaceTexture);
                camera.setPreviewCallback(this);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraHelper.getInstance().releaseCamera();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d("slimxu", "onFrameAvailable");
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("slimxu", "onPreviewFrame");
    }
}
