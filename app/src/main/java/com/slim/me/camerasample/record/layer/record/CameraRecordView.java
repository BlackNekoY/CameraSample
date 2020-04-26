package com.slim.me.camerasample.record.layer.record;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.app.Constants;
import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.record.encoder.EncodeConfig;
import com.slim.me.camerasample.record.render.Texture2DRender;
import com.slim.me.camerasample.record.render.filter.BeautyFilter;
import com.slim.me.camerasample.record.render.filter.BlackWhiteFilter;
import com.slim.me.camerasample.record.render.filter.GPUImageFilter;
import com.slim.me.camerasample.record.render.filter.ImageFilterGroup;
import com.slim.me.camerasample.record.render.filter.OESFilter;
import com.slim.me.camerasample.record.render.filter.WatermarkFilter;
import com.slim.me.camerasample.util.FileUtils;
import com.slim.me.camerasample.util.OpenGLUtils;

import java.io.File;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 摄像机录制页面
 * GLSurfaceView
 */
public class CameraRecordView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public static final String TAG = "CameraRecordView";

    private int mWidth, mHeight;

    // draw frame
    private int mCameraTextureId = 0;
    private float[] mCameraMatrix = new float[16];
    private SurfaceTexture mSurfaceTexture;
    private Texture2DRender mTexture2DRender;

    // record
    private CameraRecorder mRecorder;
    private boolean mRecording; // 是否正在录制
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
    }

    private File createVideoFile() {
        if (FileUtils.isFileExist(Constants.APP_DIR_PATH)) {
            FileUtils.delete(Constants.APP_DIR_PATH);
        }
        FileUtils.ensureDirExists(new File(Constants.APP_DIR_PATH));
        File videoFile = new File(Constants.APP_DIR_PATH, "slim_" + System.currentTimeMillis() + ".mp4");
        FileUtils.createNewFile(videoFile);
        return videoFile;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 初始化OpenGL
        GLES30.glClearColor(0, 0, 0, 1);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // 初始化滤镜 渲染器
        mTexture2DRender = new Texture2DRender();
        mTexture2DRender.setFilter(new OESFilter());
        mTexture2DRender.init();

        // 初始化相机纹理
        mCameraTextureId = OpenGLUtils.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
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
        GLES30.glViewport(0, 0, width, height);

        preview();
        mTexture2DRender.onSizeChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 更新相机纹理
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mCameraMatrix);

        mTexture2DRender.drawTexture(mCameraTextureId, mCameraMatrix, null);

        onVideoDrawFrame(mTexture2DRender.getTextureId());
    }

    public void onDestroy() {
    }

    private GPUImageFilter getFilter() {
        ArrayList<GPUImageFilter> filters = new ArrayList<>();
        filters.add(new OESFilter());
        filters.add(new BeautyFilter());
        filters.add(new BlackWhiteFilter());
        filters.add(new WatermarkFilter(BitmapFactory.decodeResource(getContext().getResources(), R.drawable.awesomeface)));
        return new ImageFilterGroup(filters);
    }

    private void preview() {
        if (!CameraHelper.getInstance().isOpened()) {
            openCamera();
        }
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
        CameraHelper.getInstance().setPreviewFormat(ImageFormat.YV12);
        CameraHelper.getInstance().setDisplayOrientation(90);
        CameraHelper.getInstance().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        return true;
    }

    private void onVideoDrawFrame(int textureId) {
        if (mRecording) {
            switch (mRecordState) {
                case STATE_RECORD_OFF:
                    // 开始录制，设置录制线程的ShareEGLContext为渲染线程的EGLContext，因为textureID为渲染线程的
                    EncodeConfig encodeConfig = createEncodeConfig();
                    encodeConfig.updateEglContext(EGL14.eglGetCurrentContext());
                    mRecorder.startRecord(encodeConfig);
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
        CameraHelper.CustomSize pictureSize = CameraHelper.getInstance().getMatchedPictureSize(mWidth, mHeight);
        CameraHelper.CustomSize previewSize = CameraHelper.getInstance().getMatchedPreviewSize(mWidth, mHeight);
        if (pictureSize != null) {
            CameraHelper.getInstance().setPictureSize(pictureSize);
        }
        if (previewSize != null) {
            CameraHelper.getInstance().setPreviewSize(previewSize);
        }
    }

    private EncodeConfig createEncodeConfig() {
        File videoFile = createVideoFile();
        // 需要保证视频的长宽是偶数
        int width = mWidth;
        int height = mHeight;
        if (width % 2 != 0) {
            width--;
        }
        if (height % 2 != 0) {
            height--;
        }
        return new EncodeConfig(videoFile.getAbsolutePath(),
                width, height,
                2 * 1024 * 1024,
                1,
                30, 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public void startRecord() {
        mRecording = true;
    }

    public void stopRecord() {
        mRecording = false;
    }

    public void changeFilter(@NonNull GPUImageFilter filter) {
        mTexture2DRender.setFilter(filter);
    }
}
