package com.slim.me.camerasample.record.render

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
            fbo.release()
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