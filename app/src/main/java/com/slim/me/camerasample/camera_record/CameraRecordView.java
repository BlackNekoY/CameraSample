package com.slim.me.camerasample.camera_record;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.GlUtil;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 摄像机录制页面
 * GLSurfaceView
 */
public class CameraRecordView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private final String VERTEX_SHADER = "#version 300 es\n" +
            "layout(location = 0) in vec2 pos;\n" +
            "layout(location = 1) in vec2 texPos;\n" +
            "uniform mat4 matrix;\n" +
            "out vec2 outTexPos;\n" +
            "int main() {\n" +
            "   gl_Position = matrix * vec4(pos, 0, 1);\n" +
            "   outTexPos = texPos;\n" +
            "}\n";

    private final String FRAGMENT_SHADER = "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "in vec2 outTexPos;\n" +
            "uniform samplerExternalOES cameraTexture; \n" +
            "out vec4 color;\n" +
            "int main { \n" +
            "   color = texture(cameraTexture, outTexPos);\n" +
            "} \n";

    private final float[] VERTEX_ARRAY = {
            // 位置顶点    // 纹理顶点
            -1, 1,   0, 1,
            1, 1,    1, 1,
            -1, -1,  0, 0,
            1, -1,   1, 0
    };
    private FloatBuffer mVertexBuf;

    private int mProgram = -1;
    private int mVAO = -1;
    private int mVBO = -1;
    private int mCameraTextureId = 0;
    private SurfaceTexture mSurfaceTexture;

    private float[] mMatrix = new float[16];

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
        mVertexBuf = GlUtil.createFloatBuffer(VERTEX_ARRAY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 初始化OpenGL
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClearColor(0, 0, 0, 1);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        mProgram = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        mVAO = vaos[0];

        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        mVBO = vbos[0];

        GLES30.glBindVertexArray(mVAO);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mVertexBuf.capacity() * 4, mVertexBuf, GLES30.GL_STATIC_DRAW);
        int posHandle = GLES30.glGetAttribLocation(mProgram, "pos");
        int texPosHandle = GLES30.glGetAttribLocation(mProgram, "texPos");
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 0);
        GLES30.glVertexAttribPointer(texPosHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 2 * 4);
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glEnableVertexAttribArray(texPosHandle);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glBindVertexArray(0);

        // 初始化相机纹理
        mCameraTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        // 预览相机
        preview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 更新相机纹理
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mMatrix);

        // 相机纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram, "cameraTexture"), 0);

        // 矩阵
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(mProgram, "matrix"), 1, false, mMatrix, 0);

        GLES30.glBindVertexArray(mVAO);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
    }

    private void preview() {
        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_BACK);
        setupCameraParams();
        CameraHelper.getInstance().setSurfaceTexture(mSurfaceTexture);
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
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }
}
