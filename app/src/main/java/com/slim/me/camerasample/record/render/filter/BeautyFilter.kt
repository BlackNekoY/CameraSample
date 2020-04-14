package com.slim.me.camerasample.record.render.filter

import android.opengl.GLES30
import com.slim.me.camerasample.R
import com.slim.me.camerasample.util.OpenGLUtils

class BeautyFilter : GPUImageFilter() {

    override fun getFragmentShader(): String {
        return OpenGLUtils.readShaderFromRawResource(R.raw.beauty)
    }

    override fun onPreDraw(textureId: Int, cameraMatrix: FloatArray?, textureMatrix: FloatArray?) {
        super.onPreDraw(textureId, cameraMatrix, textureMatrix)
        GLES30.glUniform2fv(GLES30.glGetUniformLocation(program, "singleStepOffset"), 1, floatArrayOf(2.0f / outputWidth, 2.0f / outputHeight), 0)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "params"), 0.1f)
    }
}