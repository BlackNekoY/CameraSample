package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

public class OESFilter extends BlankFilter {

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

    @Override
    protected void onInit() {
        setShader(BlankFilter.VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    protected void onBindPointer() {
        super.onBindPointer();
    }

    @Override
    protected void onDrawFrame(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(getProgram(), "sTexture"), 0);
        // 矩阵
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(getProgram(), "textureMatrix"), 1, false, cameraMatrix, 0);
    }
}
