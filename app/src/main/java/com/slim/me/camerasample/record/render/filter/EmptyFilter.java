package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES30;

import com.slim.me.camerasample.util.GlUtil;

/**
 * 没有任何效果的Filter，仅仅只是把输入的Texture渲染出来
 */
public class EmptyFilter extends BaseFilter {

    protected static final String VERTEX_SHADER =
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

    protected static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
                    "precision mediump float;\n" +

                    "in vec2 outTexPos;\n" +
                    "uniform sampler2D sTexture; \n" +
                    "out vec4 color;\n" +
                    "void main() { \n" +
                    "   color = texture(sTexture, outTexPos);\n" +
                    "} \n";


    @Override
    protected void onInit() {
        setShader(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    protected void onBindPointer() {
        int posHandle = GLES30.glGetAttribLocation(getProgram(), "pos");
        int texPosHandle = GLES30.glGetAttribLocation(getProgram(), "texPos");
        GlUtil.checkGlError("glGetAttribLocation");
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 0);
        GlUtil.checkGlError("glVertexAttribPointer1");
        GLES30.glVertexAttribPointer(texPosHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 2 * 4);
        GlUtil.checkGlError("glVertexAttribPointer2");
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glEnableVertexAttribArray(texPosHandle);
        GlUtil.checkGlError("glEnableVertexAttribArray");
    }

    @Override
    protected void onDrawFrame(int textureId, float[] textureMatrix) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(getProgram(), "sTexture"), 0);
        // 矩阵
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(getProgram(), "textureMatrix"), 1, false, textureMatrix, 0);
    }
}
