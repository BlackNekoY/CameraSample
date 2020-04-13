package com.slim.me.camerasample.record.render.filter

import android.opengl.GLES30
import com.slim.me.camerasample.record.render.FrameBuffer
import java.util.ArrayList

class ImageFilterGroup(filters: ArrayList<GPUImageFilter>) : GPUImageFilter() {
    private val mFilters : ArrayList<GPUImageFilter> = filters
    private lateinit var mCopyFilter : GPUImageFilter
    private var mFrameBuffers = arrayOfNulls<FrameBuffer>(3)
    private var mRenderFrameBuffer : FrameBuffer? = null

    override fun onInitialized() {
        mCopyFilter = GPUImageFilter()
        mCopyFilter.init()
        for (filter in mFilters) {
            filter.init()
        }
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        for (filter in mFilters) {
            filter.onOutputSizeChanged(width, height)
        }
        deleteFrameBuffers()
        for (i in 0..2) {
            mFrameBuffers[i] = FrameBuffer(width, height)
        }
    }

    override fun onPreDraw(textureId: Int, cameraMatrix: FloatArray?, textureMatrix: FloatArray?) {
        val cameraM = cameraMatrix ?: INITIALIZE_MATRIX
        val textureM = textureMatrix ?: INITIALIZE_MATRIX
        var texId = textureId
        mRenderFrameBuffer?.unbind()
        for (i in mFilters.indices) {
            val filter = mFilters[i]
            val fbo = mFrameBuffers[i % 3]
            fbo?.run {
                bind()
                filter.draw(texId, cameraM, textureM)
                unbind()
                texId = getTextureId()
            }
        }
        mRenderFrameBuffer?.bind()
        mCopyFilter.draw(texId, cameraM, textureM)
    }


    fun setRenderFrameBuffer(renderFrameBuffer: FrameBuffer) {
        mRenderFrameBuffer = renderFrameBuffer
    }

    private fun deleteFrameBuffers() {
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
}