package com.slim.me.camerasample;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.UIUtil;

import java.io.IOException;

/**
 * Created by Slim on 2017/3/19.
 */

public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = "CameraTextureView";

    public CameraTextureView(Context context) {
        this(context, null);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    private void setupCameraParams(int width, int height) {
        Camera.Parameters param = CameraHelper.getInstance().getCameraParameters();
        if(param != null) {
            param.setPreviewFormat(ImageFormat.YV12);

            CameraHelper.getInstance().stopPreview();
            CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(width, height,
                    UIUtil.getWindowScreenWidth(getContext()), UIUtil.getWindowScreenHeight(getContext()));
            if(sizes != null) {
                CameraHelper.CustomSize pictureSize = sizes[0];
                CameraHelper.CustomSize previewSize = sizes[1];

                param.setPictureSize(pictureSize.width, pictureSize.height);
                param.setPreviewSize(previewSize.width, previewSize.height);
            }
            CameraHelper.getInstance().setCameraParameters(param);
        }
        CameraHelper.getInstance().setDisplayOrientation(90);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        setupCameraParams(width, height);
        CameraHelper.getInstance().setSurfaceTexture(surface);
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");

        CameraHelper.getInstance().stopPreview();
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(width, height,
                UIUtil.getWindowScreenWidth(getContext()), UIUtil.getWindowScreenHeight(getContext()));
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
        CameraHelper.getInstance().setSurfaceTexture(surface);
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        CameraHelper.getInstance().stopPreview();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureUpdated");
    }


}
