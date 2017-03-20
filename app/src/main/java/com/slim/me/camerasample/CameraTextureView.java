package com.slim.me.camerasample;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by Slim on 2017/3/19.
 */

public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = "CameraTextureView";
    private CameraHelper mHelper;

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

    private void setupCameraParams(Camera camera) {
        Camera.Parameters param = camera.getParameters();
        param.setPreviewFormat(ImageFormat.YV12);
        camera.setDisplayOrientation(90);

        camera.setParameters(param);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable");
            Camera camera = CameraHelper.getInstance().getCamera();

        if(camera != null) {
            setupCameraParams(camera);
            try {
                camera.setPreviewTexture(surface);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");

        Camera camera = CameraHelper.getInstance().getCamera();

        if (camera != null) {
            camera.stopPreview();

            Camera.Size previewSize = CameraHelper.getFitPreviewSize(camera, width, height);
            if (previewSize != null) {
                camera.getParameters().setPreviewSize(previewSize.width, previewSize.height);
            }

            Camera.Size pictureSize = CameraHelper.getFitPictureSize(camera, width, height);
            if(pictureSize != null) {
                camera.getParameters().setPictureSize(pictureSize.width, pictureSize.height);
            }

            try {
                camera.setPreviewTexture(surface);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Camera camera = CameraHelper.getInstance().getCamera();
        if(camera != null) {
            try {
                camera.setPreviewTexture(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.stopPreview();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureUpdated");
    }


}
