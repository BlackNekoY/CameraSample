package com.slim.me.camerasample.edit.decode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.slim.me.camerasample.task.TaskThread
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class VideoDecoder {
    private var mVideoExtractor: MediaExtractor? = null
    private var mVideoCodec: MediaCodec? = null

    private val mEditing: AtomicBoolean = AtomicBoolean(false)
    private var mExtractorThread: ExtractorThread? = null

    companion object {
        private const val TAG = "VideoDecoder"
        private const val VIDEO_MIME_TYPE = "video/avc"
    }

    fun prepare(videoPath: String, surface: Surface) {
        try {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(videoPath)
            var videoTrackIndex = -1
            for (i in 0..mediaExtractor.trackCount) {
                val mediaFormat = mediaExtractor.getTrackFormat(i)
                if (mediaFormat.getString(MediaFormat.KEY_MIME).contains("video")) {
                    videoTrackIndex = i
                    break
                }
            }
            if (videoTrackIndex >= 0) {
                val mediaFormat = mediaExtractor.getTrackFormat(videoTrackIndex)
                val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
                val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
                val duration = mediaFormat.getLong(MediaFormat.KEY_DURATION) // TimeUnit: ts
                mediaExtractor.selectTrack(videoTrackIndex)

                try {
                    mVideoCodec = MediaCodec.createDecoderByType(VIDEO_MIME_TYPE)
                    mVideoCodec?.configure(mediaFormat, surface, null, 0)
                    mVideoCodec?.start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            mVideoExtractor = mediaExtractor
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun start() {
        if (mEditing.get()) {
            return
        }
        mExtractorThread?.run {
            if (isAlive && !isInterrupted) {
                interrupt()
            }
        }
        val extractor = mVideoExtractor ?: return
        val codec = mVideoCodec ?: return
        mEditing.set(true)
        mExtractorThread = ExtractorThread(extractor, codec)
        mExtractorThread?.start()
    }

    fun stop() {
        if (!mEditing.get()) {
            return
        }
        mEditing.set(false)
        mExtractorThread?.interrupt()
        mExtractorThread = null
        mVideoExtractor = null
        mVideoCodec = null
    }

    inner class ExtractorThread(private val extractor: MediaExtractor,
                                private val videoCodec: MediaCodec) : TaskThread("Video-Extractor-Thread") {
        private lateinit var buffer: ByteBuffer
        private val bufferInfo = MediaCodec.BufferInfo()
        private var startTime = 0L
        private var firstFrame = false
        override fun isTaskWorking(): Boolean {
            return mEditing.get()
        }

        override fun onTaskPreRun() {
            buffer = ByteBuffer.allocate(100 * 1024)
            startTime = System.currentTimeMillis()
            firstFrame = true
        }

        override fun onTaskRunning() {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size >= 0) {
                buffer.position(size)
                buffer.flip()
                putBufferToCodec(buffer, size, extractor.sampleTime, videoCodec)
                decode()
                extractor.advance()
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                videoCodec.flush()
                startTime = System.currentTimeMillis()
                firstFrame = true
            }
        }

        override fun onTaskFinish() {
            try {
                videoCodec.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                videoCodec.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                extractor.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            this@VideoDecoder.stop()
        }

        private fun putBufferToCodec(buffer: ByteBuffer, size: Int, pts: Long, videoCodec: MediaCodec) {
            val inputBufferIndex = videoCodec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = videoCodec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.put(buffer)
                    if (size < 0) {
                        videoCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        videoCodec.queueInputBuffer(inputBufferIndex, 0, size, pts, 0)
                    }
                }
            }
        }

        private fun decode() {
            val outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10000L)
            if (outputBufferIndex >= 0) {
                if (!firstFrame) {
                    delay(bufferInfo, startTime)
                } else {
                    firstFrame = false
                }
                videoCodec.releaseOutputBuffer(outputBufferIndex, true)
            }
        }

        private fun delay(bufferInfo: MediaCodec.BufferInfo, startMillis: Long) {
            while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMillis) {
                try {
                    sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}