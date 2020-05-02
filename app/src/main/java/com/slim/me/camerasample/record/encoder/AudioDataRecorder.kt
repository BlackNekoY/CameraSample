package com.slim.me.camerasample.record.encoder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
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

    private var mAudioRecord: AudioRecord? = null

    private var mRecordThread: AudioEncodeThread? = null
    private var mBufferSize = 0
    private var mDurationUs = 0L
    private val mIsRecording: AtomicBoolean = AtomicBoolean(false)

    fun start(encodeConfig: EncodeConfig, dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) {
        release()
        mBufferSize = AudioRecord.getMinBufferSize(encodeConfig.audioSampleRate, CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT)
        mDurationUs = (1000000 * (mBufferSize.toDouble() / CHANNEL_COUNT / 2 / encodeConfig.audioSampleRate)).toLong()
        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, encodeConfig.audioSampleRate, CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT, mBufferSize)
        mAudioRecord?.let {
            startEncodeThread(it, dataQueue)
            mIsRecording.set(true)
        }
    }

    fun stop() {
        Log.i(TAG, "stop -- start")
        mIsRecording.set(false)
        release()
        Log.i(TAG, "stop -- end")
    }

    fun getDurationUs() : Long {
        return mDurationUs
    }

    private fun startEncodeThread(audioRecord: AudioRecord, dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) {
        mRecordThread = AudioEncodeThread(audioRecord, dataQueue)
        mRecordThread?.start()
    }

    private fun release() {
        mAudioRecord?.run {
            stop()
            release()
        }
        mAudioRecord = null
        mRecordThread?.run {
            if (!isInterrupted) {
                interrupt()
            }
        }
        mRecordThread = null
        mBufferSize = 0
    }

    private inner class AudioEncodeThread(private val audioRecord: AudioRecord,
            private val mDataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) : Thread("Audio-Record-Thread") {

        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            // 创建AudioRecord作为音频源
            audioRecord.startRecording()
            var readBytes: Int
            while (!isInterrupted && mIsRecording.get()) {
                val buf = ByteBuffer.allocateDirect(mBufferSize)
                buf.clear()
                try {
                    readBytes = audioRecord.read(buf, mBufferSize)
                    buf.position(readBytes)
                    buf.flip()
                    mDataQueue.put(Pair(buf, readBytes))
                } catch (e: InterruptedException) {
                    // 线程外部终止
                    e.printStackTrace()
                    break
                }
            }
            Log.i(TAG, "stop AudioEncodeThread")
        }
    }
}