package com.slim.me.camerasample.record.render

import android.opengl.GLES30

object FrameBufferFactory {
    private var mFrameBuffers = arrayOfNulls<FrameBuffer>(3)

    fun initFrameBuffers(width: Int, height: Int) {
        deleteFrameBuffers()
        for (i in 0..2) {
            mFrameBuffers[i] = FrameBuffer(width, height)
        }
    }

    fun deleteFrameBuffers() {
        for (fbo in mFrameBuffers) {
            if (fbo != null) {
                val ids = intArrayOf(fbo.textureId, fbo.rbo, fbo.fbo)
                GLES30.glDeleteTextures(1, ids, 0)
                GLES30.glDeleteRenderbuffers(1, ids, 1)
                GLES30.glDeleteFramebuffers(1, ids, 2)
            }
        }
        mFrameBuffers = arrayOfNulls(3)
    }

    fun getFrameBuffers() : Array<FrameBuffer?> {
        return mFrameBuffers
    }
}