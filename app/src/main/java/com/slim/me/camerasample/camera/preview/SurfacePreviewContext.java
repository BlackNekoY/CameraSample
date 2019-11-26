package com.slim.me.camerasample.camera.preview;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;

import com.slim.me.camerasample.camera.CameraHelper;

/**
 * Created by Slim on 2017/4/23.
 */

public class SurfacePreviewContext extends PreviewContext implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = "SurfacePreviewContext";

    public SurfacePreviewContext(@NonNull Context context) {
        super(context);
    }

    private void setupCameraParams() {
        Camera.Parameters param = CameraHelper.getInstance().getCameraParameters();
        if(param != null) {
            param.setPreviewFormat(ImageFormat.YV12);
        }
        CameraHelper.getInstance().setCameraParameters(param);
        CameraHelper.getInstance().setDisplayOrientation(90);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "createProgram");
        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);

        setupCameraParams();
        CameraHelper.getInstance().setSurfaceHolder(holder);
        CameraHelper.getInstance().setPreViewCallback(this, false);
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        if (holder.getSurface() == null) {
            return;
        }

        CameraHelper.getInstance().stopPreview();
        CameraHelper.CustomSize pictureSize = CameraHelper.getInstance().getMatchedPictureSize(width, height);
        CameraHelper.CustomSize previewSize = CameraHelper.getInstance().getMatchedPreviewSize(width, height);
        if (pictureSize != null) {
            CameraHelper.getInstance().setPictureSize(pictureSize);
        }
        if (previewSize != null) {
            CameraHelper.getInstance().setPreviewSize(previewSize);
        }
        CameraHelper.getInstance().setSurfaceHolder(holder);
        CameraHelper.getInstance().setPreViewCallback(this, false);
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        CameraHelper.getInstance().stopPreview();
        CameraHelper.getInstance().releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        getPreviewFrame(data);
    }
}
