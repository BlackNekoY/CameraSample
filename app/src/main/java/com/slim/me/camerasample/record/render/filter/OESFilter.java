package com.slim.me.camerasample.record.render.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.support.annotation.NonNull;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.util.OpenGLUtils;

public class OESFilter extends GPUImageFilter {

    @Override
    protected void onPreDraw(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(getProgram(), "inputImageTexture"), 0);
        GLES30.glUniform1f(GLES30.glGetUniformLocation(getProgram(), "filpY"), 0f);
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(getProgram(), "textureMatrix"), 1, false, cameraMatrix, 0);
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.oes);
    }
}
