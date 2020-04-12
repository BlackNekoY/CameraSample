package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES30;
import android.support.annotation.NonNull;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.util.OpenGLUtils;


/**
 * 没有任何效果的Filter，仅仅只是把输入的Texture渲染出来
 */
public class NoEffectFilter extends ImageFilter {

    // 是否颠倒纹理采样
    private boolean mFilpY;

    private static final float[] VERTEX_ARRAY = {
            // 位置顶点    // 纹理顶点
            -1, 1,   0, 1,
            1, 1,    1, 1,
            -1, -1,  0, 0,
            1, -1,   1, 0
    };

    @Override
    protected void onBindVAO() {
        int posHandle = GLES30.glGetAttribLocation(getProgram(), "pos");
        int texPosHandle = GLES30.glGetAttribLocation(getProgram(), "texPos");
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 0);
        GLES30.glVertexAttribPointer(texPosHandle, 2, GLES30.GL_FLOAT, false, 4 * 4, 2 * 4);
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glEnableVertexAttribArray(texPosHandle);
    }

    @Override
    protected void onDrawFrame(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(getProgram(), "sTexture"), 0);
        GLES30.glUniform1f(GLES30.glGetUniformLocation(getProgram(), "filpY"), mFilpY? 1.0f : 0);
        // 矩阵
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(getProgram(), "textureMatrix"), 1, false, textureMatrix, 0);
    }

    @NonNull
    @Override
    public String getVertexShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.noeffect_vertex);
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.noeffect_fragment);
    }

    @Override
    protected float[] getVertexArray() {
        return VERTEX_ARRAY;
    }

    public void setFilpY(boolean filpY) {
        mFilpY = filpY;
    }
}
