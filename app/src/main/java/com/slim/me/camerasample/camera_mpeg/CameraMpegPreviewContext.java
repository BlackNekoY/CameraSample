package com.slim.me.camerasample.camera_mpeg;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.preview.SurfacePreviewContext;
import com.slim.me.camerasample.util.GlUtil;

/**
 * Created by slimxu on 2018/1/9.
 */

public class CameraMpegPreviewContext extends SurfacePreviewContext implements SurfaceTexture.OnFrameAvailableListener {

    private CameraRecorder mRecorder;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;

    public CameraMpegPreviewContext(@NonNull Context context) {
        super(context);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);

        mTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST,
                GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        super.surfaceChanged(holder, format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        mSurfaceTexture.release();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }
}
