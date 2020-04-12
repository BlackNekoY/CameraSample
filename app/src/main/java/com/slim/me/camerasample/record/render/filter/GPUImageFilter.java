package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.support.annotation.NonNull;

import com.slim.me.camerasample.util.OpenGLUtils;

import java.nio.FloatBuffer;

/**
 * 滤镜基类，只搭建渲染框架
 */
public abstract class GPUImageFilter {

    private static final float[] INITIALIZE_MATRIX = new float[16];

    private int mProgram = -1;
    private int mVAO = -1;
    private int mVBO = -1;
    private int mOutputWidth;
    private int mOutputHeight;

    /**
     * 在这里初始化
     * 1. 初始化program
     * 2. 初始化FloatBuffer，VBO,VAO
     */
    public void init() {
        onInit();

        Matrix.setIdentityM(INITIALIZE_MATRIX, 0);
        FloatBuffer vertexBuffer = OpenGLUtils.createFloatBuffer(getVertexArray());
        mProgram = OpenGLUtils.createProgram(getVertexShader(), getFragmentShader());
        int[] arr = new int[1];

        GLES30.glGenVertexArrays(1, arr, 0);
        mVAO = arr[0];

        GLES30.glGenBuffers(1, arr, 0);
        mVBO = arr[0];

        GLES30.glBindVertexArray(mVAO);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);

        onBindVAO();

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);
    }

    public final void draw(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glUseProgram(mProgram);
        onDrawFrame(textureId,
                cameraMatrix != null? cameraMatrix : INITIALIZE_MATRIX,
                textureMatrix != null? textureMatrix : INITIALIZE_MATRIX);
        GLES30.glBindVertexArray(mVAO);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);

        onAfterDraw(textureId, cameraMatrix, textureMatrix);
    }

    public final void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    int getOutputWidth() {
        return mOutputWidth;
    }

    int getOutputHeight() {
        return mOutputHeight;
    }

    final int getProgram() {
        return mProgram;
    }

    protected void onInit(){}

    @NonNull
    protected abstract String getVertexShader();

    @NonNull
    protected abstract String getFragmentShader();

    protected abstract float[] getVertexArray();

    protected abstract void onBindVAO();

    protected void onAfterDraw(int textureId, float[] cameraMatrix, float[] textureMatrix) {}

    protected abstract void onDrawFrame(int textureId, float[] cameraMatrix, float[] textureMatrix);
}
