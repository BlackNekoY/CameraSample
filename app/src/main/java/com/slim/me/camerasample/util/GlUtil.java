package com.slim.me.camerasample.util;

import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Created by slimxu on 2018/1/9.
 */

public class GlUtil {

    public static final String TAG = "GlUtil";

    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     *
     * @param textureTarget Texture类型
     *       1. 相机用 GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     *       2. 图片用GLES20.GL_TEXTURE_2D
     * @param minFilter 缩小过滤类型 (1.GL_NEAREST ; 2.GL_LINEAR)
     * @param magFilter 放大过滤类型
     * @param wrapS X方向边缘环绕
     * @param wrapT Y方向边缘环绕
     * @return 返回创建的TextureId
     */
    public static int createTexture(int textureTarget, int minFilter, int magFilter, int wrapS, int wrapT) {
        int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);
        GlUtil.checkGlError("glGenTextures");
        GLES20.glBindTexture(textureTarget, textureHandle[0]);
        GlUtil.checkGlError("glBindTexture " + textureHandle[0]);
        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, magFilter); //线性插值
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, wrapS);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, wrapT);
        GlUtil.checkGlError("glTexParameter");
        return textureHandle[0];
    }

    public static int createTexture(int textureTarget) {
        return createTexture(textureTarget, GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
