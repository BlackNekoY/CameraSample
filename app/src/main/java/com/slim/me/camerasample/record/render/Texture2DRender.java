package com.slim.me.camerasample.record.render;

import android.opengl.GLES30;

public class Texture2DRender extends TextureOESRender {

    /**
     * 2D的片元着色器
     */
    protected final String FRAGMENT_SHADER =
            "#version 300 es\n" +
            "precision mediump float;\n" +

            "in vec2 outTexPos;\n" +
            "uniform sampler2D sTexture; \n" +
            "out vec4 color;\n" +
            "void main() { \n" +
            "   color = texture(sTexture, outTexPos);\n" +
            "} \n";

    public Texture2DRender () {
        mTextureType = GLES30.GL_TEXTURE_2D;
        setShaders(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
