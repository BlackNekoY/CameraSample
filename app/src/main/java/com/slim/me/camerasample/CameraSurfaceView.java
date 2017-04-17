package com.slim.me.camerasample;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.UIUtil;

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
        setupCameraParams();
        CameraHelper.getInstance().setSurfaceHolder(holder);
        CameraHelper.getInstance().startPreview();
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }

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
        CameraHelper.getInstance().setSurfaceHolder(holder);
        CameraHelper.getInstance().startPreview();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraHelper.getInstance().stopPreview();
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
