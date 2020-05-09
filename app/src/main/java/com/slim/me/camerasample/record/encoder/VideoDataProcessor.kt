package com.slim.me.camerasample.record.encoder

import android.media.MediaCodec
import android.util.Log
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean

class VideoDataProcessor(private val mVideoCodec: MediaCodec,
                         private val mBufferInfo: MediaCodec.BufferInfo,
                         private val mMuxer: MuxerWrapper) {

    private var mProcessThread: ProcessThread? = null
    private var mIsStart: AtomicBoolean = AtomicBoolean(false)

    companion object {
        const val TAG = "VideoDataProcessor"
    }

    fun start() {
        mProcessThread?.run{
            if (isAlive && !isInterrupted) {
                interrupt()
            }
        }
        mIsStart.set(true)
        mProcessThread = ProcessThread()
        mProcessThread?.start()
    }

    fun stop() {
        mIsStart.set(false)
        mProcessThread?.interrupt()
        mProcessThread = null
    }

    private fun release() {
        // 释放VideoCodec
        try {
            mVideoCodec.stop()
        } catch (e: Exception) {
            Log.w(TAG, "mVideoCodec stop exception:$e")
        }

        try {
            mVideoCodec.release()
        } catch (e: Exception) {
            Log.w(TAG, "mVideoCodec release exception:$e")
        }

        Log.d(TAG, "release video codec.")
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val TIMEOUT_USEC = 10000
        if (endOfStream) {
            mVideoCodec.signalEndOfInputStream()
        }
        // 有一些机器，signalEndOfInputStream之后一直收不到BUFFER_FLAG_END_OF_STREAM，导致录制无法结束。这里添加一个计数，如果连续100次dequeueOutputBuffer还没有结束，就直接抛出异常。
        var endTryTimes = 0
        var encoderOutputBuffers = mVideoCodec.outputBuffers
        while (true) {
            val encoderStatus = mVideoCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC.toLong())
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 当前队列数据已处理完，等待surface更新，跳出循环。
                if (!endOfStream) {
                    Log.d(TAG, "VideoCodec: no output available yet")
                    break      // out of while
                } else {
                    Log.d(TAG, "VideoCodec: no output available, spinning to await EOS")
                    endTryTimes++
                    if (endTryTimes > 100) {
                        throw RuntimeException("VideoCodec: Encoder is not stopped after dequeue 100 times.")
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mVideoCodec.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 只有在第一次写入视频时会到这里
                val videoFormat = mVideoCodec.outputFormat
                Log.d(TAG, "VideoCodec: encoder output format changed: $videoFormat")
                mMuxer.addVideoTrack(videoFormat)
                mMuxer.start()
                if (!mMuxer.isStarted) {
                    synchronized(mMuxer) {
                        while (!mMuxer.isStarted) {
                            sleep(100)
                        }
                    }
                }
            } else if (encoderStatus < 0) {
                // 其他未知错误，忽略
                Log.w(TAG, "VideoCodec: unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
            } else {
                // 如果有收到surface更新，就将endTryTimes清0。
                endTryTimes = 0

                val encodedData = encoderOutputBuffers[encoderStatus]
                        ?: throw RuntimeException("VideoCodec: encoderOutputBuffer " + encoderStatus +
                                " was null")

                if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "VideoCodec: ignoring BUFFER_FLAG_CODEC_CONFIG")
                    mBufferInfo.size = 0
                }

                if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) {
                    Log.d(TAG, "VideoCodec: sent I-FRAME")
                }

                if (mBufferInfo.size != 0 && mMuxer.isStarted) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset)
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size)
                    mMuxer.writeVideoData(encodedData, mBufferInfo)

                    Log.d(TAG, "VideoCodec: sent " + mBufferInfo.size + " bytes to muxer, us=" + mBufferInfo.presentationTimeUs)
                }

                mVideoCodec.releaseOutputBuffer(encoderStatus, false)

                if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "VideoCodec: reached end of stream unexpectedly")
                    } else {
                        Log.d(TAG, "VideoCodec: end of stream reached")
                    }
                    break
                }
            }
        }
    }

    inner class ProcessThread : Thread("Video-Process-Thread") {
        override fun run() {
            try {
                mVideoCodec.start()
                while (!isInterrupted && mIsStart.get()) {
                    try {
                        drainEncoder(false)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                drainEncoder(false)
                drainEncoder(true)
                try {
                    release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}