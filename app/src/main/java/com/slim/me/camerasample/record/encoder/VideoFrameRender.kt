package com.slim.me.camerasample.record.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean

class VideoFrameRender{

    private var mProcessThread: ProcessThread? = null
    private var mIsStart: AtomicBoolean = AtomicBoolean(false)

    private var mEncodeConfig: EncodeConfig? = null
    private var mVideoCodec: MediaCodec? = null
    private var mVideoBufferInfo: MediaCodec.BufferInfo? = null
    private var mSurface: Surface? = null
    private var mMuxer: MuxerWrapper? = null

    companion object {
        private const val TAG = "VideoFrameRender"
        private const val VIDEO_MIME_TYPE = "video/avc"
    }

    fun getInputSurface() : Surface {
        return mSurface ?: throw IllegalStateException("can not getInputSurface before prepare.")
    }

    fun prepare(encodeConfig: EncodeConfig, muxer: MuxerWrapper) {
        mEncodeConfig = encodeConfig
        mMuxer = muxer
        prepareVideoCodec(encodeConfig)
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

    private fun prepareVideoCodec(encodeConfig: EncodeConfig) {
        mVideoBufferInfo = MediaCodec.BufferInfo()
        // 配置MediaFormat
        val format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, encodeConfig.width, encodeConfig.height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodeConfig.videoBitRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, encodeConfig.videoIFrameRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, encodeConfig.videoFrameRate)
        // 创建MediaCodec
        try {
            mVideoCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mVideoCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mSurface = mVideoCodec?.createInputSurface()
    }

    private fun release() {
        // 释放VideoCodec
        try {
            mVideoCodec?.stop()
            mVideoCodec?.release()
            mSurface?.release()
        } catch (e: Exception) {
            Log.w(TAG, "mVideoCodec release exception:$e")
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val videoCodec = mVideoCodec ?: return
        val bufferInfo = mVideoBufferInfo ?: return

        if (endOfStream) {
            videoCodec.signalEndOfInputStream()
        }
        // 有一些机器，signalEndOfInputStream之后一直收不到BUFFER_FLAG_END_OF_STREAM，导致录制无法结束。这里添加一个计数，如果连续100次dequeueOutputBuffer还没有结束，就直接抛出异常。
        var endTryTimes = 0
        var encoderOutputBuffers = videoCodec.outputBuffers
        while (true) {
            val encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, 10000L)
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
                encoderOutputBuffers = videoCodec.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 只有在第一次写入视频时会到这里
                val videoFormat = videoCodec.outputFormat
                Log.d(TAG, "VideoCodec: encoder output format changed: $videoFormat")

                mMuxer?.run {
                    addVideoTrack(videoFormat)
                    start()
                    if (!isStarted) {
                        synchronized(this) {
                            while (!isStarted) {
                                sleep(100)
                            }
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

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "VideoCodec: ignoring BUFFER_FLAG_CODEC_CONFIG")
                    bufferInfo.size = 0
                }

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) {
                    Log.d(TAG, "VideoCodec: sent I-FRAME")
                }


                if (bufferInfo.size != 0 && mMuxer?.isStarted == true) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    mMuxer?.writeVideoData(encodedData, bufferInfo)

                    Log.d(TAG, "VideoCodec: sent " + bufferInfo.size + " bytes to muxer, us=" + bufferInfo.presentationTimeUs)
                }

                videoCodec.releaseOutputBuffer(encoderStatus, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
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
                mVideoCodec?.start()
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
                release()
            }
        }
    }
}