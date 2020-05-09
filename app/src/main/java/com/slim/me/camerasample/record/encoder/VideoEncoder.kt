package com.slim.me.camerasample.record.encoder

/**
 * 摄像机视频录制器
 * 1. VideoFrameRender -> 起渲染线程用于GL Draw，作为VideoCodec Surface数据提供者
 * 2. VideoFrameProcessor -> 起编码线程用于VideoCodec Encode
 */
class VideoEncoder {

    private var mFrameRender: VideoFrameRender? = null
    private var mFrameProcessor: VideoFrameProcessor? = null

    private var mMuxer: MuxerWrapper? = null

    companion object {
        const val TAG = "VideoEncoder"
    }

    fun startEncode(encodeConfig: EncodeConfig, muxer: MuxerWrapper) {
        mMuxer = muxer

        mFrameRender = VideoFrameRender()
        mFrameProcessor = VideoFrameProcessor()

        mFrameRender?.prepare(encodeConfig, muxer)
        mFrameProcessor?.prepare(encodeConfig, mFrameRender?.getInputSurface()!!)

        mFrameProcessor?.start()
        mFrameRender?.start()
    }

    fun onVideoFrameUpdate(textureId: Int) {
        mFrameProcessor?.onVideoFrameUpdate(textureId)
    }

    fun stopEncode() {
        mFrameRender?.stop()
        mFrameProcessor?.stop()
        mMuxer?.releaseVideo()
    }
}
