package com.slim.me.camerasample.preview;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.UiUtil;

/**
 * Created by Slim on 2017/4/23.
 */

public class TexturePreviewContext extends PreviewContext implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    public static final String TAG = "TexturePreviewContext";

    public TexturePreviewContext(@NonNull Context context) {
        super(context);
    }

    private void setupCameraParams(int width, int height) {
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(width, height,
                UiUtil.getWindowScreenWidth(context), UiUtil.getWindowScreenHeight(context));
        if(sizes != null) {
            CameraHelper.getInstance().setPictureSize(sizes[0]);
            CameraHelper.getInstance().setPreviewSize(sizes[1]);
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
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(width, height,
                UiUtil.getWindowScreenWidth(context), UiUtil.getWindowScreenHeight(context));
        if(sizes != null) {
            CameraHelper.getInstance().setPictureSize(sizes[0]);
            CameraHelper.getInstance().setPreviewSize(sizes[1]);
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
