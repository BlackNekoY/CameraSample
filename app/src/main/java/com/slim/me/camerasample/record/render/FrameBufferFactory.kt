package com.slim.me.camerasample.record.render

import android.opengl.GLES30
import java.util.*

class FrameBufferFactory {
    private var mWidth = 0
    private var mHeight = 0
    private var mFrameBuffers = LinkedList<FrameBuffer>()

    fun initFrameBuffers(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        deleteFrameBuffers()
    }

    fun deleteFrameBuffers() {
        for (fbo in mFrameBuffers) {
            val ids = intArrayOf(fbo.textureId, fbo.rbo, fbo.fbo)
            GLES30.glDeleteTextures(1, ids, 0)
            GLES30.glDeleteRenderbuffers(1, ids, 1)
            GLES30.glDeleteFramebuffers(1, ids, 2)
        }
        mFrameBuffers.clear()
    }

    fun applyFrameBuffer() : FrameBuffer {
        val frameBuffer: FrameBuffer
        if (mFrameBuffers.isEmpty()) {
            frameBuffer = FrameBuffer(mWidth, mHeight)
        } else {
            frameBuffer = mFrameBuffers.removeFirst()
        }
        return frameBuffer
    }

    fun repayFrameBuffer(frameBuffer: FrameBuffer) {
        mFrameBuffers.addLast(frameBuffer)
    }

}