package com.slim.me.camerasample.util;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.slim.me.camerasample.app.BaseApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by slimxu on 2018/1/9.
 */

public class OpenGLUtils {

    public static final String TAG = "OpenGLUtils";

    public static final int SIZEOF_FLOAT = 4;

    public static String readShaderFromRawResource(final int resourceId){
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(BaseApplication.Companion.getIns().getResources().openRawResource(resourceId)));
        String nextLine;
        final StringBuilder body = new StringBuilder();
        try{
            while ((nextLine = bufferedReader.readLine()) != null){
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e){
            return "";
        }
        return body.toString();
    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES30.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES30.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES30.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES30.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES30.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     *
     * @param textureTarget Texture类型
     *       1. 相机用 GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     *       2. 图片用GLES30.GL_TEXTURE_2D
     * @param minFilter 缩小过滤类型 (1.GL_NEAREST ; 2.GL_LINEAR)
     * @param magFilter 放大过滤类型
     * @param wrapS X方向边缘环绕
     * @param wrapT Y方向边缘环绕
     * @return 返回创建的TextureId
     */
    public static int createTexture(int textureTarget, int minFilter, int magFilter, int wrapS, int wrapT) {
        int[] textureHandle = new int[1];

        GLES30.glGenTextures(1, textureHandle, 0);
        GLES30.glBindTexture(textureTarget, textureHandle[0]);
        GLES30.glTexParameterf(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, minFilter);
        GLES30.glTexParameterf(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, magFilter); //线性插值
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, wrapS);
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, wrapT);
        GLES30.glBindTexture(textureTarget, 0);
        return textureHandle[0];
    }

    public static int createTexture(int textureTarget) {
        return createTexture(textureTarget, GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE, GLES30.GL_CLAMP_TO_EDGE);
    }

    public static int createTexture2D(Bitmap bitmap) {
        return createTexture2D(bitmap, GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE, GLES30.GL_CLAMP_TO_EDGE);
    }

    public static int createTexture2D(Bitmap bitmap, int minFilter, int magFilter, int wrapS, int wrapT) {
        final int texId = createTexture(GLES30.GL_TEXTURE_2D, minFilter, magFilter, wrapS, wrapT);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return texId;
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
