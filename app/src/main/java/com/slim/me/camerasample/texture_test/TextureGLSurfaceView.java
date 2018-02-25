package com.slim.me.camerasample.texture_test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.util.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by slimxu on 2018/2/19.
 */

public class TextureGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private FloatBuffer mVertexBuffer;
    private static final float[] VERTEX_ARRAY = {
            0, 0.5f, 0,
            0.5f, -0.5f, 0,
            -0.5f, -0.5f, 0,
    };

    private FloatBuffer mTextureBuffer;
    private static final float[] TEXTURE_ARRAY = {
            0.5f, 1,
            1, 0,
            0, 0,
    };

    private static final String VERTEX_SHADER =
            "attribute vec3 aPosition;\n" +
                    "attribute vec2 aTextureCoor; \n" +
                    "varying vec2 vTextureCoor;" +
                    "void main() {\n" +
                    "    gl_Position = vec4(aPosition, 1);\n" +
                    "    vTextureCoor = vec2(aTextureCoor.x, 1.0f - aTextureCoor.y);\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "uniform sampler2D texture1; \n" +
                    "uniform sampler2D texture2; \n" +
                    "varying vec2 vTextureCoor;     \n" +
                    "void main() {\n" +
                    "    gl_FragColor = mix(texture2D(texture1, vTextureCoor), texture2D(texture2, vTextureCoor), 0.5);\n" +
                    "}";

    private int mTexture1Id;
    private int mTexture2Id;

    private int mCount;

    private int mProgram;
    private int mPositionHandle;
    private int mTextureCoorHandle;
    private int mTexture1Handle;
    private int mTexture2Handle;


    public TextureGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public TextureGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mProgram = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTextureCoorHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoor");
        mTexture1Handle = GLES20.glGetUniformLocation(mProgram, "texture1");
        mTexture2Handle = GLES20.glGetUniformLocation(mProgram, "texture2");

        mCount = 3;
        mVertexBuffer = createBuffer(VERTEX_ARRAY);
        mTextureBuffer = createBuffer(TEXTURE_ARRAY);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.aio_voicechange_img_loly);
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.aio_voicechange_img_uncle);

        mTexture1Id = createTexture(bitmap1);
        mTexture2Id = createTexture(bitmap2);

        bitmap1.recycle();
        bitmap2.recycle();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoorHandle);

        GLES20.glVertexAttribPointer(mPositionHandle,
                3, GLES20.GL_FLOAT, false, 3 * 4, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTextureCoorHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture1Id);
        GLES20.glUniform1i(mTexture1Handle, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture2Id);
        GLES20.glUniform1i(mTexture2Handle, 1);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mCount);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private int createTexture(Bitmap bitmap) {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureIds[0];
    }

    private int createTexture(int width, int height) {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureIds[0];
    }

    private FloatBuffer createBuffer(float[] buffer) {
        FloatBuffer floatBuffer = ByteBuffer.allocateDirect(buffer.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        floatBuffer.put(buffer);
        floatBuffer.position(0);
        return floatBuffer;
    }
}
