package com.slim.me.camerasample.record.encoder

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class AudioDataProcessor {

    companion object {
        private const val TAG = "AudioDataProcessor"
        // 单声道
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        // 声道数
        private const val CHANNEL_COUNT = 1
    }

    private var mAudioCodec: MediaCodec? = null
    private var mAudioBuffInfo: MediaCodec.BufferInfo? = null
    private var mProcessThread: AudioProcessThread? = null
    private var mMuxer: MuxerWrapper? = null
    private var mDurationUs = 0L
    private var mCount = 0
    private var mFirstPts = 0L
    private var mIsRecording: AtomicBoolean = AtomicBoolean(false)

    fun start(encodeConfig: EncodeConfig, dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>, muxer: MuxerWrapper, durationUs: Long) {
        release()
        mDurationUs = durationUs
        mMuxer = muxer
        prepareCodec(encodeConfig)
        mAudioCodec?.let {
            startProcessThread(dataQueue)
            mIsRecording.set(true)
        }
    }

    fun stop() {
        Log.i(TAG, "stop -- start")
        mIsRecording.set(false)
        release()
        Log.i(TAG, "stop -- end")
    }

    private fun prepareCodec(encodeConfig: EncodeConfig) {
        try {
            mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, encodeConfig.audioSampleRate, CHANNEL_COUNT)
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, encodeConfig.audioBitRate)
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
            mAudioCodec?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            mAudioBuffInfo = MediaCodec.BufferInfo()
        } catch (e: IOException) {
            e.printStackTrace()
            mAudioCodec = null
            mAudioBuffInfo = null
        }
    }

    private fun startProcessThread(dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) {
        val codec = mAudioCodec ?: return
        val muxer = mMuxer ?: return
        val bufferInfo = mAudioBuffInfo ?: return

        mProcessThread = AudioProcessThread(codec, muxer, bufferInfo, dataQueue)
        mProcessThread?.start()
    }

    private fun release() {
        mAudioCodec?.run {
            try {
                stop()
            } catch (e: Exception) {
                Log.w(TAG, "mAudioCodec stop exception:$e")
            }
            try {
                release()
            } catch (e: Exception) {
                Log.w(TAG, "mAudioCodec release exception:$e")
            }
            Log.d(TAG, "release video codec.")
        }
        mAudioCodec = null
        mAudioBuffInfo = null
        mMuxer = null
        mCount = 0
        mFirstPts = 0L
        mDurationUs = 0L
        mProcessThread?.run {
            if (!isInterrupted) {
                interrupt()
            }
        }
        mProcessThread = null
    }

    private inner class AudioProcessThread(private val audioCodec: MediaCodec,
                                           private val muxer: MuxerWrapper,
                                           private val bufferInfo: MediaCodec.BufferInfo,
                                           private val mDataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>) : Thread("Audio-Process-Thread") {

        override fun run() {
            audioCodec.start()
            while (!isInterrupted && mIsRecording.get()) {
                try {
                    val pair = mDataQueue.take()
                    onAudioDataReady(pair.first, pair.second)
                } catch (e: InterruptedException) {
                    break
                }
            }
            Log.i(TAG, "stop AudioProcessThread")
        }

        private fun onAudioDataReady(buffer: ByteBuffer, readSize: Int) {
            // 将data送往InputBuffer编码
            val inputBufferIndex = audioCodec.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = audioCodec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.put(buffer)
                    audioCodec.queueInputBuffer(inputBufferIndex, 0, readSize, getPts(), 0)
                }
            }
            drainEncoder()
        }

        private fun getPts() : Long {
            if (mFirstPts == 0L) {
                mFirstPts = mDurationUs
                mCount = 0
            }
            val ptsUs = System.nanoTime() / 1000L
            var pts = mFirstPts + mCount * mDurationUs
            if (ptsUs - pts > 30000) {
                mFirstPts += ptsUs - pts
                pts = mFirstPts + mCount * mDurationUs
            }
            mCount++
            return pts
        }

        /**
         * while true循环，从OutputBuffer中取数据写入Muxer
         */
        private fun drainEncoder() {
            var encoderOutputBuffers = audioCodec.outputBuffers
            while (true) {
                val encoderStatus = audioCodec.dequeueOutputBuffer(bufferInfo, 10000)
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // 当前队列数据已处理完，跳出循环。
                    Log.d(TAG, "AudioCodec: no output available yet")
                    break      // out of while
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = audioCodec.outputBuffers
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // 只有在第一次写入视频时会到这里
                    val videoFormat = audioCodec.outputFormat
                    Log.d(TAG, "AudioCodec: encoder output format changed: $videoFormat")
                    muxer.addAudioTrack(videoFormat)
                    muxer.start()
                } else if (encoderStatus < 0) {
                    // 其他未知错误，忽略
                    Log.w(TAG, "AudioCodec: unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                } else {
                    val outputBuffer = encoderOutputBuffers[encoderStatus]
                            ?: throw RuntimeException("AudioCodec: encoderOutputBuffer " + encoderStatus +
                                    " was null")

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                        Log.d(TAG, "AudioCodec: ignoring BUFFER_FLAG_CODEC_CONFIG")
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0 && muxer.isStarted) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeAudioData(outputBuffer, bufferInfo)

                        Log.d(TAG, "AudioCodec: sent " + bufferInfo.size + " bytes to muxer, ts=" +
                                bufferInfo.presentationTimeUs * 1000)
                    }

                    audioCodec.releaseOutputBuffer(encoderStatus, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        }
    }
}