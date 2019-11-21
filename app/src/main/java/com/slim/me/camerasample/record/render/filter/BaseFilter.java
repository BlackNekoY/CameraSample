package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.support.annotation.NonNull;

import com.slim.me.camerasample.util.GlUtil;

import java.nio.FloatBuffer;

/**
 * 滤镜基类
 */
public abstract class BaseFilter {

    private static final float[] INITIALIZE_MATRIX = new float[16];

    private int mProgram = -1;
    private int mVAO = -1;
    private int mVBO = -1;

    static {
        Matrix.setIdentityM(INITIALIZE_MATRIX, 0);
    }

    BaseFilter() {
        init();
    }

    /**
     * 在这里初始化
     * 1. 初始化program
     * 2. 初始化FloatBuffer，VBO,VAO
     */
    private void init() {
        onInit();
        FloatBuffer vertexBuffer = GlUtil.createFloatBuffer(getVertexArray());

        mProgram = GlUtil.createProgram(getVertexShader(), getFragmentShader());
        int[] arr = new int[1];
        // gen VAO
        GLES30.glGenVertexArrays(1, arr, 0);
        mVAO = arr[0];
        // gen VBO
        GLES30.glGenBuffers(1, arr, 0);
        mVBO = arr[0];

        // bind VAO
        GLES30.glBindVertexArray(mVAO);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);
        GlUtil.checkGlError("glBufferData");

        // 子类AttribPointer
        onPreDraw();

        // unbind VAO
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
    }

    final int getProgram() {
        return mProgram;
    }

    protected void onInit(){}

    /**
     * 子类返回顶点着色器
     */
    @NonNull
    protected abstract String getVertexShader();

    /**
     * 子类返回片元着色器
     */
    @NonNull
    protected abstract String getFragmentShader();

    /**
     * 子类返回顶点数组
     */
    protected abstract float[] getVertexArray();

    /**
     * 子类在渲染前的绑定顶点
     */
    protected abstract void onPreDraw();

    /**
     * 渲染
     * @param textureId 纹理ID
     * @param textureMatrix 纹理矩阵
     */
    protected abstract void onDrawFrame(int textureId, float[] cameraMatrix, float[] textureMatrix);
}
