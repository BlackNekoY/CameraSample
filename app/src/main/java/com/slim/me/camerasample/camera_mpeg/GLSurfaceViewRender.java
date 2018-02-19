package com.slim.me.camerasample.camera_mpeg;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.encoder.EncodeConfig;
import com.slim.me.camerasample.render.RenderBuffer;
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
    private SurfaceTexture mSurfaceTexture;

    private CameraRecorder mRecorder;
    private EncodeConfig mEncodeConfig;
    private int mRecordingState = RECORDING_OFF;
    public boolean mRecordingEnabled = false;

    private final float[] mSTMatrix = new float[16];
    private TextureRender mTextureRender;
    private RenderBuffer mRenderFBO;

    private int mSurfaceWidth, mSurfaceHeight;
    private int mPreviewWidth, mPreviewHeight;


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
        mCameraTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(mCameraSurfaceView);

        // 创建OpenGL的render
        mTextureRender = new TextureRender();

        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);
        setupCameraParams();
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
        CameraHelper.getInstance().startPreview();

        mRecordingState = RECORDING_OFF;
        mRecordingEnabled = false;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        CameraHelper.getInstance().stopPreview();
        setPreviewSize();
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
        CameraHelper.getInstance().startPreview();

        mPreviewWidth = CameraHelper.getInstance().getCameraParameters().getPreviewSize().width;
        mPreviewHeight = CameraHelper.getInstance().getCameraParameters().getPreviewSize().height;

        mRenderFBO = new RenderBuffer(mSurfaceWidth, mSurfaceHeight, GLES20.GL_TEXTURE0);
    }

    private void setPreviewSize() {
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(mSurfaceWidth, mSurfaceHeight,
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
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 将SurfaceTexture上的纹理数据交换到TextureId上
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);

        float[] mvpMatrix = caculateCenterCropMvpMatrix(mPreviewWidth, mPreviewHeight, mSurfaceWidth, mSurfaceHeight);
        mvpMatrix = null;

        // 不使用FBO的方式
//        mTextureRender.drawTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId, mSTMatrix, mvpMatrix);
//        onVideoDrawFrame(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId, mSTMatrix, mvpMatrix, mSurfaceTexture.getTimestamp());

        // 这里draw了Texture后，GLSurfaceView的环境会自动的在调用onDrawFrame后进行swapBuffers将EglSurface 上的内容交换到 View 的 Surface上显示
        mRenderFBO.bind();
        mTextureRender.drawTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId, mSTMatrix, mvpMatrix);
        mRenderFBO.unbind();
        mTextureRender.drawTexture(GLES20.GL_TEXTURE_2D, mRenderFBO.getTextId(), null, null);

        onVideoDrawFrame(GLES20.GL_TEXTURE_2D, mRenderFBO.getTextId(), null, null, mSurfaceTexture.getTimestamp());
    }

    private void onVideoDrawFrame(int textureType, int textureId, float[] textureMatrix, float[] mvpMatrix, long timestampNanos) {
        if(mRecordingEnabled && mEncodeConfig != null) {
            switch (mRecordingState){
                case RECORDING_OFF:
                    mEncodeConfig.updateEglContext(EGL14.eglGetCurrentContext());
                    mRecorder.startRecord(mEncodeConfig);
                    mRecordingState = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    break;
            }
            mRecorder.onFrameAvailable(textureType, textureId, textureMatrix, mvpMatrix, timestampNanos);
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

    public static float[] caculateCenterCropMvpMatrix(int textureWidth, int textureHeight, int surfaceWidth, int surfaceHeight) {
        float surfaceRatio = (float) surfaceWidth / surfaceHeight;
        float textureRatio = (float) textureWidth / textureHeight;
        float scaleX = 1.0f, scaleY = 1.0f;
        if (surfaceRatio < textureRatio) {
            scaleX = (textureRatio * surfaceHeight) / surfaceWidth;
        } else if (surfaceRatio > textureRatio) {
            scaleY = surfaceWidth / (textureRatio * surfaceHeight);
        }
        float[] mvpMatrix = new float[16];
        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f);
        return mvpMatrix;
    }
}
