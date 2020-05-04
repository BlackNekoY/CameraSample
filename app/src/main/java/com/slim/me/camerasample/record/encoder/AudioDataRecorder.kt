package com.slim.me.camerasample.record.encoder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class AudioDataRecorder {

    companion object {
        private const val TAG = "AudioDataRecorder"
        // 单声道
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        // 声道数
        private const val CHANNEL_COUNT = 1
    }

    private var mRecordThread: AudioEncodeThread? = null
    private val mIsRecording: AtomicBoolean = AtomicBoolean(false)
    private var mSampleRate = 0

    fun start(encodeConfig: EncodeConfig, dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) {
        if (mIsRecording.get()) {
            return
        }
        mSampleRate = encodeConfig.audioSampleRate
        mRecordThread?.run {
            if (isAlive && !isInterrupted) {
                interrupt()
            }
        }
        startEncodeThread(dataQueue)
        mIsRecording.set(true)
    }

    fun stop() {
        mIsRecording.set(false)
        mRecordThread?.interrupt()
        mRecordThread = null
    }

    private fun startEncodeThread(dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) {
        mRecordThread = AudioEncodeThread(dataQueue)
        mRecordThread?.start()
    }

    private inner class AudioEncodeThread(private val mDataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) : Thread("Audio-Record-Thread") {

        private var audioRecord: AudioRecord? = null

        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            // 创建AudioRecord作为音频源
            val minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT)
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
            audioRecord?.run {
                startRecording()
                var readBytes: Int
                try {
                    while (!isInterrupted && mIsRecording.get()) {
                        val buf = ByteBuffer.allocateDirect(minBufferSize)
                        buf.clear()
                        try {
                            readBytes = read(buf, minBufferSize)
                            if (readBytes > 0) {
                                buf.position(readBytes)
                                buf.flip()
                                mDataQueue.put(Pair(buf, readBytes))
                            }
                        } catch (e: InterruptedException) {
                            // 线程外部终止
                            e.printStackTrace()
                            break
                        }
                    }
                } catch (e: Exception) {
                } finally {
                    stop()
                    release()
                }
            }
            audioRecord = null
            Log.i(TAG, "AudioEncodeThread finish")
        }
    }
}