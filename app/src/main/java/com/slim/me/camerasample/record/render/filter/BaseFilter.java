package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.slim.me.camerasample.util.GlUtil;

import java.nio.FloatBuffer;

/**
 * 滤镜基类
 */
public abstract class BaseFilter {

    private static final float[] VERTEX_ARRAY = {
            // 位置顶点    // 纹理顶点
            -1, 1,   0, 1,
            1, 1,    1, 1,
            -1, -1,  0, 0,
            1, -1,   1, 0
    };
    private FloatBuffer mVertexBuf = GlUtil.createFloatBuffer(VERTEX_ARRAY);
    private String mVertexShader;
    private String mFragmentShader;

    private int mProgram = -1;
    private int mVAO = -1;
    private int mVBO = -1;

    public BaseFilter() {
        init();
    }

    /**
     * 在这里初始化
     * 1. 初始化program
     * 2. 初始化FloatBuffer，VBO,VAO
     */
    private void init() {
        onInit();

        mProgram = GlUtil.createProgram(mVertexShader, mFragmentShader);
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
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mVertexBuf.capacity() * 4, mVertexBuf, GLES30.GL_STATIC_DRAW);
        GlUtil.checkGlError("glBufferData");

        // 子类AttribPointer
        onBindPointer();

        // unbind VAO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);
    }

    protected void setShader(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    protected int getProgram() {
        return mProgram;
    }

    public final void draw(int textureId, float[] textureMatrix) {
        if (textureMatrix == null) {
            textureMatrix = new float[16];
            Matrix.setIdentityM(textureMatrix, 0);
        }
        GLES30.glUseProgram(mProgram);

        onDrawFrame(textureId, textureMatrix);

        GLES30.glBindVertexArray(mVAO);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
    }

    /**
     * 子类初始化，在这里设置Shader
     */
    protected abstract void onInit();

    /**
     * 绑定顶点
     */
    protected abstract void onBindPointer();

    /**
     * 渲染
     * @param textureId 纹理ID
     * @param textureMatrix 纹理矩阵
     */
    protected abstract void onDrawFrame(int textureId, float[] textureMatrix);
}
