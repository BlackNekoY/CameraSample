package com.slim.me.camerasample.util;

import android.opengl.Matrix;

/**
 * Created by slimxu on 2018/1/14.
 */

public class FilterUtil {
    public static final String TAG = "FilterUtil";

//    public static final String NO_FILTER_VERTEX_SHADER =
//                    "uniform mat4 uMVPMatrix;\n" +
//                    "uniform mat4 uTextureMatrix;\n" +
//                    "attribute vec4 position;\n" +
//                    "attribute vec4 inputTextureCoordinate;\n" +
//
//                    "varying vec2 textureCoordinate;\n" +
//
//                    "void main() {\n" +
//                    "    gl_Position = uMVPMatrix * position;\n" +
//                    "    textureCoordinate = (uTextureMatrix * inputTextureCoordinate).xy;\n" +
//                    "}\n";

    //默认的顶点shader
    public static final String NO_FILTER_VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "attribute vec4 inputTextureCoordinate2;\n" +
                    "attribute vec4 inputTextureCoordinate3;\n" +
                    "attribute vec4 inputTextureCoordinate4;\n" +
                    "attribute vec4 inputTextureCoordinate5;\n" +
                    "attribute vec4 inputTextureCoordinate6;\n" +

                    "varying vec2 textureCoordinate;\n" +
                    "varying vec2 textureCoordinate2;\n" +
                    "varying vec2 textureCoordinate3;\n" +
                    "varying vec2 textureCoordinate4;\n" +
                    "varying vec2 textureCoordinate5;\n" +
                    "varying vec2 textureCoordinate6;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * position;\n" +
                    "    textureCoordinate = (uTextureMatrix * inputTextureCoordinate).xy;\n" +
                    "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
                    "    textureCoordinate3 = inputTextureCoordinate3.xy;\n" +
                    "    textureCoordinate4 = inputTextureCoordinate4.xy;\n" +
                    "    textureCoordinate5 = inputTextureCoordinate5.xy;\n" +
                    "    textureCoordinate6 = inputTextureCoordinate6.xy;\n" +
                    "}";

    //默认的片段shader
    public static final String NO_FILTER_FRAGMENT_SHADER =
            "precision lowp float;\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputImageTexture;\n" +

                    "void main() {\n" +
                    "gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "}";

    /**
     * 需要绘制的图形顶点坐标，也就是整个屏幕
     * 标准化设备坐标
     */
    public static float VERTEXT_COORDS[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };
    /**
     *
     */
    public static final float TEXUTURE_COORDS[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    public static void checkGlError(String op) {
        GlUtil.checkGlError(op);
    }

    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    public static float[] caculateCenterCropMvpMatrix(int textureWidth, int textureHeight, int surfaceWidth, int surfaceHeight) {
        float surfaceRatio = (float) surfaceWidth / surfaceHeight;
        float textureRatio = (float) textureWidth / textureHeight;
        float scaleX = 1.0f, scaleY = 1.0f;
        if (surfaceRatio < textureRatio) {
            scaleX = (textureRatio * surfaceHeight) / surfaceWidth;
        } else if (surfaceRatio > textureRatio) {
            scaleY = surfaceWidth / (textureRatio * surfaceHeight);
        }
        float[] mvpMatrix = new float[16];
        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f);
        return mvpMatrix;
    }

    public static float[] caculateAbsoluteMvpMatrix(int textureWidth, int textureHeight, int surfaceWidth, int surfaceHeight) {
        float scaleX = (float) textureWidth / surfaceWidth;
        float scaleY = (float) textureHeight / surfaceHeight;
        float[] mvpMatrix = new float[16];
        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f);
        return mvpMatrix;
    }

}

