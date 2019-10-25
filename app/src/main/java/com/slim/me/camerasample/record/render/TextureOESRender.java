package com.slim.me.camerasample.record.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.slim.me.camerasample.util.GlUtil;

import java.nio.FloatBuffer;

/**
 * 摄像机纹理渲染器
 */
public class TextureOESRender {

    private static final String VERTEX_SHADER =
            "#version 300 es\n" +
            "layout(location = 0) in vec2 pos;\n" +
            "layout(location = 1) in vec2 texPos;\n" +
            "out vec2 outTexPos;\n" +
            "uniform mat4 textureMatrix;\n" +
            "void main() {\n" +
            "   gl_Position = vec4(pos, 0, 1);\n" +
            "   vec4 texTranformPos = textureMatrix * vec4(texPos, 0, 1); \n" +
            "   outTexPos = vec2(texTranformPos.x, texTranformPos.y);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +

            "in vec2 outTexPos;\n" +
            "uniform samplerExternalOES sTexture; \n" +
            "out vec4 color;\n" +
            "void main() { \n" +
            "   color = texture(sTexture, outTexPos);\n" +
            "} \n";
    private final float[] VERTEX_ARRAY = {
            // 位置顶点    // 纹理顶点
            -1, 1,   0, 1,
            1, 1,    1, 1,
            -1, -1,  0, 0,
            1, -1,   1, 0
    };

    private String mVertexShader;
    private String mFragmentShader;

    private FloatBuffer mVertexBuf;

    private int mProgram = -1;
    private int mVAO = -1;
    private int mVBO = -1;

    /**
     * 必须在GL线程调用，初始化program
     */
    public TextureOESRender() {
        mVertexShader = VERTEX_SHADER;
        mFragmentShader = FRAGMENT_SHADER;
        init();
    }

    private void init() {
        mVertexBuf = GlUtil.createFloatBuffer(VERTEX_ARRAY);

        mProgram = GlUtil.createProgram(mVertexShader, mFragmentShader);

        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        mVAO = vaos[0];

        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        mVBO = vbos[0];

        GLES30.glBindVertexArray(mVAO);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mVertexBuf.capacity() * 4, mVertexBuf, GLES30.GL_STATIC_DRAW);
        GlUtil.checkGlError("glBufferData");

        int posHandle = GLES30.glGetAttribLocation(mProgram, "pos");
        int texPosHandle = GLES30.glGetAttribLocation(mProgram, "texPos");
        GlUtil.checkGlError("glGetAttribLocation");
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 0);
        GlUtil.checkGlError("glVertexAttribPointer1");
        GLES30.glVertexAttribPointer(texPosHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 2 * 4);
        GlUtil.checkGlError("glVertexAttribPointer2");
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glEnableVertexAttribArray(texPosHandle);
        GlUtil.checkGlError("glEnableVertexAttribArray");
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glBindVertexArray(0);
    }

    public void drawTexture(int textureId, float[] textureMatrix) {
        if (textureMatrix == null) {
            textureMatrix = new float[16];
            Matrix.setIdentityM(textureMatrix, 0);
        }
        GLES30.glUseProgram(mProgram);
        // 相机纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgram, "sTexture"), 0);

        // 矩阵
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(mProgram, "textureMatrix"), 1, false, textureMatrix, 0);

        GLES30.glBindVertexArray(mVAO);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
    }

}
