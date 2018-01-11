package com.slim.me.camerasample.preview;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.UiUtil;

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
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(width, height,
                UiUtil.getWindowScreenWidth(context), UiUtil.getWindowScreenHeight(context));
        if(sizes != null) {
            Camera.Parameters param = CameraHelper.getInstance().getCameraParameters();
            if(param != null) {
                CameraHelper.CustomSize pictureSize = sizes[0];
                CameraHelper.CustomSize previewSize = sizes[1];

                param.setPictureSize(pictureSize.width, pictureSize.height);
                param.setPreviewSize(previewSize.width, previewSize.height);

                CameraHelper.getInstance().setCameraParameters(param);
            }
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
