package com.slim.me.camerasample.preview;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.camera_mpeg.TextureRender;
import com.slim.me.camerasample.util.GlUtil;
import com.slim.me.camerasample.util.UiUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by slimxu on 2018/1/10.
 */

public class GLSurfaceViewRender implements GLSurfaceView.Renderer {

    private CameraGLSurfaceView mCameraSurfaceView;

    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private TextureRender mTextureRender;

    private final float[] mSTMatrix = new float[16];


    public GLSurfaceViewRender(CameraGLSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = cameraSurfaceView;
        mTextureRender = new TextureRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST,
                GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(mCameraSurfaceView);

        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);
        setupCameraParams();
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        CameraHelper.getInstance().stopPreview();
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(width, height,
                UiUtil.getWindowScreenWidth(mCameraSurfaceView.getContext()),
                UiUtil.getWindowScreenHeight(mCameraSurfaceView.getContext()));
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
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mTextureRender.drawFrame(mTextureId, mSTMatrix);
    }

    private void setupCameraParams() {
        Camera.Parameters param = CameraHelper.getInstance().getCameraParameters();
        if(param != null) {
            param.setPreviewFormat(ImageFormat.YV12);
        }
        CameraHelper.getInstance().setCameraParameters(param);
        CameraHelper.getInstance().setDisplayOrientation(90);
    }
}
