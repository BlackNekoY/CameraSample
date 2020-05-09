package com.slim.me.camerasample.record.encoder

import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.Surface
import com.slim.me.camerasample.egl.EglCore
import com.slim.me.camerasample.record.render.Texture2DRender
import com.slim.me.camerasample.record.render.filter.GPUImageFilter
import java.util.concurrent.atomic.AtomicBoolean

class VideoFrameProcessor {

    private var mHandlerThread: HandlerThread? = null
    private var mHandler: Handler? = null

    private var mEncodeConfig: EncodeConfig? = null
    private var mIsStarted = AtomicBoolean(false)

    private var mSurface: Surface? = null
    private var mEglCore: EglCore? = null
    private var mEGLSurface: EGLSurface? = null
    private var mTextureRender: Texture2DRender? = null

    companion object {
        private const val TAG = "VideoFrameProcessor"
        private const val MSG_START_RECORD = 1
        private const val MSG_STOP_RECORD = 2
        private const val MSG_VIDEO_FRAME_UPDATE = 3
    }

    fun prepare(encodeConfig: EncodeConfig, surface: Surface) {
        mEncodeConfig = encodeConfig
        mSurface = surface
    }

    fun start() {
        // 开启录制线程
        startEncodeThread()
        Message.obtain(mHandler, MSG_START_RECORD).sendToTarget()
    }

    fun stop() {
        Message.obtain(mHandler, MSG_STOP_RECORD).sendToTarget()
    }

    fun onVideoFrameUpdate(textureId: Int) {
        Message.obtain(mHandler, MSG_VIDEO_FRAME_UPDATE, arrayOf<Any>(textureId)).sendToTarget()
    }

    private fun startEncodeThread() {
        mHandlerThread?.quit()

        mHandlerThread = HandlerThread("video_encode")
        mHandlerThread?.start()
        mHandler = Handler(mHandlerThread!!.looper, EncodeCallback())
    }

    private fun startEncodeInner() {
        // 给当前线程创建EGL环境，并用MediaCodec的Surface为基础创建EGLSurface
        mEglCore = EglCore(mEncodeConfig?.sharedContext)
        mEGLSurface = mEglCore?.createWindowSurface(mSurface)
        mEglCore?.makeCurrent(mEGLSurface)

        // 设置渲染器
        mTextureRender = Texture2DRender()
        mTextureRender?.setFilter(GPUImageFilter())
        mTextureRender?.init()
        mTextureRender?.onSizeChanged(mEncodeConfig!!.width, mEncodeConfig!!.height)

        mIsStarted.set(true)
    }

    private fun stopEncodeInner() {
        Log.i(TAG, "stopEncodeInner -- start")
        mIsStarted.set(false)
        release()
        Log.i(TAG, "stopEncodeInner -- end")
    }

    private fun onVideoFrameInner(textureId: Int) {
        if (mIsStarted.get()) {
            // 将Texture内容画在MediaCodec的Surface上
            mTextureRender?.drawTexture(textureId, null, null)
            mEglCore?.swapBuffers(mEGLSurface)
            Log.d(TAG, "swap buffers")
        }
    }

    private fun release() {
        // 停止线程
        mHandlerThread?.quit()
        mHandlerThread = null
        // 释放render显存
        mTextureRender?.release()
        // 释放EGL
        mEglCore?.release()
        mEGLSurface = null
        mEglCore = null

        mSurface = null
    }

    private inner class EncodeCallback : Handler.Callback {

        override fun handleMessage(msg: Message): Boolean {
            when (msg.what) {
                MSG_START_RECORD -> startEncodeInner()
                MSG_STOP_RECORD -> stopEncodeInner()
                MSG_VIDEO_FRAME_UPDATE -> {
                    val data = msg.obj as Array<Any>
                    val texId = data[0] as Int
                    onVideoFrameInner(texId)
                }
            }
            return true
        }

    }
}