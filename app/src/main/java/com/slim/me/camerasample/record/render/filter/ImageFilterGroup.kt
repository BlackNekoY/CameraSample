package com.slim.me.camerasample.record.render.filter

import com.slim.me.camerasample.record.render.FrameBufferFactory
import java.util.ArrayList

class ImageFilterGroup(filters: ArrayList<GPUImageFilter>) : GPUImageFilter() {
    private val mFilters : ArrayList<GPUImageFilter> = filters
    private lateinit var mCopyFilter : GPUImageFilter
    private var mRenderFboFactory: FrameBufferFactory? = null

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
    }

    override fun onPreDraw(textureId: Int, cameraMatrix: FloatArray?, textureMatrix: FloatArray?) {
        val factory = mRenderFboFactory ?: return

        val cameraM = cameraMatrix ?: INITIALIZE_MATRIX
        val textureM = textureMatrix ?: INITIALIZE_MATRIX
        var texId = textureId
        val cacheFrameBuffers = arrayOf(factory.applyFrameBuffer(), factory.applyFrameBuffer())
        for (i in mFilters.indices) {
            val filter = mFilters[i]

            val fbo = cacheFrameBuffers[i % cacheFrameBuffers.size]
            fbo.run {
                bind()
                filter.draw(texId, cameraM, textureM)
                unbind()
                texId = getTextureId()
            }
        }
        mCopyFilter.draw(texId, cameraM, textureM)

        for (fbo in cacheFrameBuffers) {
            factory.repayFrameBuffer(fbo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (filter in mFilters) {
            filter.destroy()
        }
    }

    fun setFrameBufferFactory(factory: FrameBufferFactory?) {
        mRenderFboFactory = factory
    }
}