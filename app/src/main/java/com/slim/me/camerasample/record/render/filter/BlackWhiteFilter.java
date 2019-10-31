package com.slim.me.camerasample.record.render.filter;

public class BlackWhiteFilter extends BlankFilter {

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
                    "precision mediump float;\n" +

                    "in vec2 outTexPos;\n" +
                    "uniform sampler2D sTexture; \n" +
                    "out vec4 color;\n" +
                    "void main() { \n" +
                    "   vec4 currColor = texture(sTexture, outTexPos);\n" +
                    "   float grey = 0.3f * currColor.r + 0.59f * currColor.g + 0.11f * currColor.b;\n" +
                    "   color = vec4(grey, grey, grey, 1);\n" +
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
        super.onDrawFrame(textureId, cameraMatrix, textureMatrix);
    }
}
