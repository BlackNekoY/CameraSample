package com.slim.me.camerasample.preview;

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

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "CameraPreviewView";

    private SurfaceHolder mHolder;
    private SurfacePreviewContext mPreviewContext;

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

    public SurfacePreviewContext getPreviewContext() {
        return mPreviewContext;
    }

    public void setPreviewContext(SurfacePreviewContext previewContext) {
        mPreviewContext = previewContext;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "createProgram");
        if(mPreviewContext != null) {
            mPreviewContext.surfaceCreated(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        if(mPreviewContext != null) {
            mPreviewContext.surfaceChanged(holder, format, width, height);
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if(mPreviewContext != null) {
            mPreviewContext.surfaceDestroyed(holder);
        }
    }

}
