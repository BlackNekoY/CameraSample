package com.slim.me.camerasample.camera.preview;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;

import com.slim.me.camerasample.camera.CameraHelper;

/**
 * Created by Slim on 2017/4/23.
 */

public class TexturePreviewContext extends PreviewContext implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    public static final String TAG = "TexturePreviewContext";

    public TexturePreviewContext(@NonNull Context context) {
        super(context);
    }

    private void setupCameraParams(int width, int height) {
        CameraHelper.CustomSize pictureSize = CameraHelper.getInstance().getMatchedPictureSize(width, height);
        CameraHelper.CustomSize previewSize = CameraHelper.getInstance().getMatchedPreviewSize(width, height);
        if (pictureSize != null) {
            CameraHelper.getInstance().setPictureSize(pictureSize);
        }
        if (previewSize != null) {
            CameraHelper.getInstance().setPreviewSize(previewSize);
        }
        CameraHelper.getInstance().setPreviewFormat(ImageFormat.YV12);
        CameraHelper.getInstance().setDisplayOrientation(90);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);
        setupCameraParams(width, height);

        CameraHelper.getInstance().setSurfaceTexture(surface);
        CameraHelper.getInstance().setPreViewCallback(this, true);

        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
        CameraHelper.getInstance().stopPreview();
        CameraHelper.CustomSize pictureSize = CameraHelper.getInstance().getMatchedPictureSize(width, height);
        CameraHelper.CustomSize previewSize = CameraHelper.getInstance().getMatchedPreviewSize(width, height);
        if (pictureSize != null) {
            CameraHelper.getInstance().setPictureSize(pictureSize);
        }
        if (previewSize != null) {
            CameraHelper.getInstance().setPreviewSize(previewSize);
        }
        CameraHelper.getInstance().setSurfaceTexture(surface);
        CameraHelper.getInstance().setPreViewCallback(this, true);

        CameraHelper.getInstance().startPreview();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");

        CameraHelper.getInstance().stopPreview();
        CameraHelper.getInstance().releaseCamera();
        return false;
    }

    /**
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureUpdated");
    }

    /**
     * 这是原生的每一帧数据
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        getPreviewFrame(data);
    }
}
