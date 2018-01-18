package com.slim.me.camerasample.camera_mpeg;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.encoder.EncodeConfig;
import com.slim.me.camerasample.render.TextureRender;
import com.slim.me.camerasample.util.GlUtil;
import com.slim.me.camerasample.util.UiUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by slimxu on 2018/1/10.
 */

public class GLSurfaceViewRender implements GLSurfaceView.Renderer {

    public static final String TAG = "GLSurfaceViewRender";

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;

    private CameraGLSurfaceView mCameraSurfaceView;

    private int mCameraTextureId;
    private int mEncodeTextureId;
    private SurfaceTexture mSurfaceTexture;

    private CameraRecorder mRecorder;
    private EncodeConfig mEncodeConfig;
    private int mRecordingState = RECORDING_OFF;
    private boolean mRecordingEnabled = false;

    private final float[] mSTMatrix = new float[16];
    private TextureRender mTextureRender;


    public GLSurfaceViewRender(CameraGLSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = cameraSurfaceView;
        mRecorder = new CameraRecorder();
    }

    public void setEncodeConfig(EncodeConfig encodeConfig) {
        mEncodeConfig = encodeConfig;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 创建TextureId
        mCameraTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST,
                GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
        mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(mCameraSurfaceView);

        // 创建OpenGL的render
        mTextureRender = new TextureRender();

        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);
        setupCameraParams();
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
        CameraHelper.getInstance().startPreview();

        mRecordingEnabled = true;
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
        // 将SurfaceTexture上的纹理数据交换到TextureId上
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);

        // 这里draw了Texture后，GLSurfaceView的环境会自动的在调用onDrawFrame后进行swapBuffers将EglSurface 上的内容交换到 View 的 Surface上显示
        mTextureRender.drawTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId, mSTMatrix, null);

        onVideoDrawFrame(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId, mSTMatrix, mSurfaceTexture.getTimestamp());
    }

    private void onVideoDrawFrame(int textureType, int textureId, float[] stMatrix, long timestampNanos) {
        if(mRecordingEnabled && mEncodeConfig != null) {
            switch (mRecordingState){
                case RECORDING_OFF:
                    mRecorder.startRecord(mEncodeConfig);
                    mRecordingState = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    break;
            }
            mRecorder.onFrameAvailable(textureType, textureId, stMatrix, timestampNanos);
        } else {
            switch (mRecordingState) {
                case RECORDING_OFF:
                    break;
                case RECORDING_ON:
                    mRecorder.stopRecord();
                    mRecordingState = RECORDING_OFF;
                    break;
            }
        }
    }

    private void setupCameraParams() {
        Camera.Parameters param = CameraHelper.getInstance().getCameraParameters();
        if(param != null) {
            param.setPreviewFormat(ImageFormat.YV12);
        }
        CameraHelper.getInstance().setCameraParameters(param);
        CameraHelper.getInstance().setDisplayOrientation(90);
    }

    public void notifyPause() {
        if (mTextureRender != null) {
            mTextureRender.release();
        }

        mRecordingEnabled = false;
        mRecorder.stopRecord();
    }
}
