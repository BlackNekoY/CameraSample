package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.support.annotation.NonNull;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.util.OpenGLUtils;

import java.nio.FloatBuffer;

public class GPUImageFilter {

    protected static final float[] INITIALIZE_MATRIX = new float[16];
    protected static final float[] VERTEX_ARRAY = {
            // 位置顶点    // 纹理顶点
            -1, 1,   0, 1,
            1, 1,    1, 1,
            -1, -1,  0, 0,
            1, -1,   1, 0
    };

    private int mProgram = -1;
    private int mVAO = -1;
    private int mVBO = -1;
    protected int outputWidth;
    protected int outputHeight;
    private boolean mIsInitialized = false;

    private void onInit() {
        Matrix.setIdentityM(INITIALIZE_MATRIX, 0);
        FloatBuffer vertexBuffer = OpenGLUtils.createFloatBuffer(VERTEX_ARRAY);
        mProgram = OpenGLUtils.createProgram(getVertexShader(), getFragmentShader());
        int[] arr = new int[1];

        GLES30.glGenVertexArrays(1, arr, 0);
        mVAO = arr[0];

        GLES30.glGenBuffers(1, arr, 0);
        mVBO = arr[0];

        GLES30.glBindVertexArray(mVAO);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);

        int posHandle = GLES30.glGetAttribLocation(getProgram(), "pos");
        int texPosHandle = GLES30.glGetAttribLocation(getProgram(), "texPos");
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 0);
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glVertexAttribPointer(texPosHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 2 * 4);
        GLES30.glEnableVertexAttribArray(texPosHandle);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);
    }

    public final boolean isInit() {
        return mIsInitialized;
    }

    public final void init() {
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    public final void draw(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        if (cameraMatrix == null) {
            cameraMatrix = INITIALIZE_MATRIX;
        }
        if (textureMatrix == null) {
            textureMatrix = INITIALIZE_MATRIX;
        }
        GLES30.glUseProgram(mProgram);
        onPreDraw(textureId, cameraMatrix, textureMatrix);
        GLES30.glBindVertexArray(mVAO);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
        onAfterDraw(textureId, cameraMatrix, textureMatrix);
    }

    public void onOutputSizeChanged(final int width, final int height) {
        outputWidth = width;
        outputHeight = height;
    }

    final int getProgram() {
        return mProgram;
    }

    protected void onInitialized(){}

    @NonNull
    protected String getVertexShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.base_vertex);
    }

    @NonNull
    protected String getFragmentShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.base_fragment);
    }

    protected void onPreDraw(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(getProgram(), "inputImageTexture"), 0);
        GLES30.glUniform1f(GLES30.glGetUniformLocation(getProgram(), "filpY"), 0f);
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(getProgram(), "textureMatrix"), 1, false, textureMatrix, 0);
    }
    protected void onAfterDraw(int textureId, float[] cameraMatrix, float[] textureMatrix) {}

}
