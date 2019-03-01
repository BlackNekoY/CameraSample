package com.slim.me.camerasample.camera_record;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

import com.slim.me.camerasample.camera.CameraAbility;
import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.GlUtil;
import com.slim.me.camerasample.util.UiUtil;

import java.io.File;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 摄像机录制页面
 * GLSurfaceView
 */
public class CameraRecordView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public static final String TAG = "CameraRecordView";

    private int mCameraTextureId = 0;
    private float[] mTextureMatrix = new float[16];

    private SurfaceTexture mSurfaceTexture;

    private TextureRender mTextureRender;
    private FrameBuffer mFrameBuffer;

    private int mWidth, mHeight;

    private CameraRecorder mRecorder;
    private boolean mRecording; // 是否正在录制
    private EncodeConfig mEncodeConfig;
    private final int STATE_RECORD_ON = 1;
    private final int STATE_RECORD_OFF = 2;
    private int mRecordState = STATE_RECORD_OFF;

    public CameraRecordView(Context context) {
        super(context);
        init();
    }

    public CameraRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mRecorder = new CameraRecorder();
        mEncodeConfig = new EncodeConfig(new File(Environment.getExternalStorageDirectory(), "slim.mp4").toString(),
                0, 0,
                2 * 1024 * 1024,
                1,
                30, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 初始化OpenGL
        GLES30.glClearColor(0, 0, 0, 1);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        mTextureRender = new TextureRender();

        // 初始化相机纹理
        mCameraTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        if (!openCamera()) {
            throw new IllegalStateException("open camera failed.");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        mEncodeConfig.width = width;
        mEncodeConfig.height = height;
        // 需要保证视频的长宽是偶数
        if (mEncodeConfig.width % 2 != 0) {
            mEncodeConfig.width--;
        }
        if (mEncodeConfig.height % 2 != 0) {
            mEncodeConfig.height--;
        }

        GLES30.glViewport(0, 0, width, height);

        preview();

        mFrameBuffer = new FrameBuffer(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 更新相机纹理
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTextureMatrix);


        // 将离屏FBO绑定到当前环境，之后所有的绘制操作都绘制到了FBO
        mFrameBuffer.bind();
        // 将相机纹理画在FBO的Texture上
        mTextureRender.drawTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId, mTextureMatrix);
        // 解绑FBO，这样绘制操作重新会画在0号FBO上，也就是GLSurfaceView的屏幕
        mFrameBuffer.unbind();
        // 将FBO的纹理画在GLSurfaceView上
        mTextureRender.drawTexture(GLES30.GL_TEXTURE_2D, mFrameBuffer.getTextureId(), null);

        onVideoDrawFrame(mFrameBuffer.getTextureId());
    }

    private void preview() {
        CameraHelper.getInstance().stopPreview();
        setPreviewSize();
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
        CameraHelper.getInstance().startPreview();
    }

    private boolean openCamera() {
        if (CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK) != CameraHelper.CODE_OPEN_SUCCESS) {
            Log.d(TAG, "openCamera failed.");
            return false;
        }
        Camera.Parameters params = CameraHelper.getInstance().getCameraParameters();
        if (params == null) {
            Log.d(TAG, "Parameters is null");
            return false;
        }
        params.setPreviewFormat(ImageFormat.YV12);
        if (!CameraHelper.getInstance().setCameraParameters(params)) {
            Log.d(TAG, "setCameraParameters failed.");
            return false;
        }
        if (!CameraHelper.getInstance().setDisplayOrientation(90)) {
            Log.d(TAG, "setDisplayOrientation failed.");
            return false;
        }
        return true;
    }

    private void onVideoDrawFrame(int textureId) {
        if (mRecording) {
            switch (mRecordState) {
                case STATE_RECORD_OFF:
                    // 开始录制，设置录制线程的ShareEGLContext为渲染线程的EGLContext，因为textureID为渲染线程的
                    mEncodeConfig.updateEglContext(EGL14.eglGetCurrentContext());
                    mRecorder.startRecord(mEncodeConfig);
                    mRecordState = STATE_RECORD_ON;
                    break;
                case STATE_RECORD_ON:
                    break;
            }
            mRecorder.onVideoFrameUpdate(textureId);
        } else {
            switch (mRecordState) {
                case STATE_RECORD_OFF:
                    break;
                case STATE_RECORD_ON:
                    mRecorder.stopRecord();
                    mRecordState = STATE_RECORD_OFF;
                    break;
            }
        }
    }

    private void setPreviewSize() {
        CameraHelper.CustomSize[] sizes = CameraHelper.getInstance().getMatchedPreviewPictureSize(mWidth, mHeight,
                UiUtil.getWindowScreenWidth(getContext()),
                UiUtil.getWindowScreenHeight(getContext()));
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
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public void startRecord(boolean start) {
        mRecording = start;
    }

    public boolean isRecording() {
        return mRecording;
    }

}
