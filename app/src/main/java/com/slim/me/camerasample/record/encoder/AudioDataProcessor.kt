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

    private var mProcessThread: AudioProcessThread? = null
    private var mMuxer: MuxerWrapper? = null
    private var mIsRecording: AtomicBoolean = AtomicBoolean(false)

    fun start(encodeConfig: EncodeConfig, dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>, muxer: MuxerWrapper) {
        if (mIsRecording.get()) {
            return
        }
        mProcessThread?.run{
            if (isAlive && !isInterrupted) {
                interrupt()
            }
        }

        mMuxer = muxer
        startProcessThread(encodeConfig, dataQueue, muxer)
        mIsRecording.set(true)
    }

    fun stop() {
        mIsRecording.set(false)
        mMuxer = null
        mProcessThread?.interrupt()
        mProcessThread = null
    }

    private fun startProcessThread(encodeConfig: EncodeConfig, dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>, muxer: MuxerWrapper) {
        mProcessThread = AudioProcessThread(encodeConfig, dataQueue, muxer)
        mProcessThread?.start()
    }

    private inner class AudioProcessThread(private val encodeConfig: EncodeConfig,
                                           private val dataQueue: LinkedBlockingQueue<Pair<ByteBuffer, Int>>,
                                           private val muxer: MuxerWrapper) : Thread("Audio-Process-Thread") {

        private var mAudioCodec: MediaCodec? = null
        private var mAudioBuffInfo: MediaCodec.BufferInfo? = null
        private var mCount = 0
        private var mFirstPts = 0L

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
        }

        private fun signEndOfInputStream() {
            onAudioDataReady(ByteBuffer.allocate(0), -1)
            // only used in VideoEncoder
//            audioCodec.signalEndOfInputStream()
        }

        override fun run() {
            prepareCodec(encodeConfig)
            val audioCodec = mAudioCodec ?: return
            try {
                audioCodec.start()
                while (!isInterrupted && mIsRecording.get()) {
                    try {
                        val pair = dataQueue.take()
                        onAudioDataReady(pair.first, pair.second)
                        drainEncoder()
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                drainEncoder()
                signEndOfInputStream()
                drainEncoder()
                try {
                    release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Log.i(TAG, "AudioProcessThread finish")
        }


        private var prevOutputPTSUs = 0L
        private fun getPts() : Long {
            var result = System.nanoTime() / 1000L
            if (result < prevOutputPTSUs) {
                result = (prevOutputPTSUs - result) + result
            }
		    return result
        }

        private fun onAudioDataReady(buffer: ByteBuffer, readSize: Int) {
            // 将data送往InputBuffer编码
            val audioCodec = mAudioCodec ?: return
            val inputBufferIndex = audioCodec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = audioCodec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.put(buffer)
                    if (readSize < 0) {
                        audioCodec.queueInputBuffer(inputBufferIndex, 0, 0, getPts(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else
                        audioCodec.queueInputBuffer(inputBufferIndex, 0, readSize, getPts(), 0)
                }
            }
        }

        private fun drainEncoder() {
            val audioCodec = mAudioCodec ?: return
            val bufferInfo = mAudioBuffInfo ?: return
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
                    val audioFormat = audioCodec.outputFormat
                    Log.d(TAG, "AudioCodec: encoder output format changed: $audioFormat")
                    muxer.addAudioTrack(audioFormat)
                    muxer.start()
                    if (muxer.isStarted) {
                        synchronized(muxer) {
                            while (!muxer.isStarted) {
                                sleep(100)
                            }
                        }
                    }
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
                        bufferInfo.presentationTimeUs = getPts()
                        muxer.writeAudioData(outputBuffer, bufferInfo)
                        prevOutputPTSUs = bufferInfo.presentationTimeUs

                        Log.d(TAG, "AudioCodec: sent " + bufferInfo.size + " bytes to muxer, us=" + bufferInfo.presentationTimeUs)
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