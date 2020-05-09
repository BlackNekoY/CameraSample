package com.slim.me.camerasample.record.encoder

import android.util.Log

import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * 摄像机音频录制器
 */
class AudioEncoder {

    private val mRecorder: AudioDataRecorder = AudioDataRecorder()
    private val mProcessor: AudioDataProcessor = AudioDataProcessor()
    private var mMuxer: MuxerWrapper? = null
    private var mDateQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>> = LinkedBlockingQueue()

    companion object {
        const val TAG = "AudioEncoder"
    }

    fun setMuxer(muxer: MuxerWrapper) {
        mMuxer = muxer
    }

    fun startEncode(config: EncodeConfig) {
        mDateQueue = LinkedBlockingQueue()
        mRecorder.start(config, mDateQueue)
        mProcessor.start(config, mDateQueue, mMuxer!!)
    }

    fun stopEncode() {
        Log.i(TAG, "stopEncode -- start")
        mRecorder.stop()
        mProcessor.stop()
        mMuxer!!.releaseAudio()
        Log.i(TAG, "stopEncode -- end")
    }
}
