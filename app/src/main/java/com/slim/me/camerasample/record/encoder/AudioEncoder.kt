package com.slim.me.camerasample.record.encoder

import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * 摄像机音频录制器
 * 1. AudioDataRecorder -> 起音频录制线程用于AudioRecord，作为AudioCodec数据提供方
 * 2. AudioDataProcessor -> 起编码线程用于AudioCodec encode
 */
class AudioEncoder {

    private val mRecorder: AudioDataRecorder = AudioDataRecorder()
    private val mProcessor: AudioDataProcessor = AudioDataProcessor()
    private var mMuxer: MuxerWrapper? = null
    private var mDateQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>> = LinkedBlockingQueue()

    companion object {
        const val TAG = "AudioEncoder"
    }

    fun startEncode(config: EncodeConfig, muxer: MuxerWrapper) {
        mMuxer = muxer
        mDateQueue = LinkedBlockingQueue()
        mRecorder.start(config, mDateQueue)
        mProcessor.start(config, mDateQueue, muxer)
    }

    fun stopEncode() {
        mRecorder.stop()
        mProcessor.stop()
        mMuxer?.releaseAudio()
    }
}
