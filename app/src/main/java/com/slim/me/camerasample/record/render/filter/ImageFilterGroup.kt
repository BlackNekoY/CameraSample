package com.slim.me.camerasample.record.render.filter

import com.slim.me.camerasample.record.render.FrameBufferFactory
import java.util.ArrayList

class ImageFilterGroup(filters: ArrayList<GPUImageFilter>) : GPUImageFilter() {
    private val mFilters : ArrayList<GPUImageFilter> = filters
    private var mRenderFboFactory: FrameBufferFactory? = null

    override fun onInitialized() {
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
            if (i == mFilters.size - 1) {
                // 滤镜链中最后一个滤镜，直接上屏
                filter.draw(texId, cameraM, textureM)
            } else {
                val fbo = cacheFrameBuffers[i % cacheFrameBuffers.size]
                fbo.run {
                    bind()
                    filter.draw(texId, cameraM, textureM)
                    unbind()
                    texId = getTextureId()
                }
            }
        }
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