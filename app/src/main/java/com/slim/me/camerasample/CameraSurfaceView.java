package com.slim.me.camerasample;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.slim.me.camerasample.camera.CameraHelper;

import java.io.IOException;


/**
 * Created by Slim on 2017/3/18.
 */

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = "CameraPreviewView";

    private SurfaceHolder mHolder;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera camera = CameraHelper.getInstance().getCamera();

        if (camera != null) {
            setupCameraParams(camera);
            try {
//                camera.setPreviewCallback(this);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupCameraParams(Camera camera) {
        Camera.Parameters param = camera.getParameters();
        param.setPreviewFormat(ImageFormat.YV12);
        camera.setDisplayOrientation(90);

        camera.setParameters(param);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
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
//                camera.setPreviewCallback(this);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Camera camera = CameraHelper.getInstance().getCamera();
        if (camera != null) {
            camera.setPreviewCallback(null);
            try {
                camera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.stopPreview();
        }
    }

    /**
     * 相机的每一帧数据
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // 这里的数据是NV21 或者 YV12格式，需要转换为I420
        Log.d(TAG, "onPreviewFrame, data length:" + data.length);
    }

}
