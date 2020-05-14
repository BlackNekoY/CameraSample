package com.slim.me.camerasample.edit

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.slim.me.camerasample.edit.decode.VideoDecoder

class DecodeGLSurfaceView : SurfaceView, SurfaceHolder.Callback {

    private val mVideoDecoder: VideoDecoder = VideoDecoder()
    private var mEditVideoParam: EditVideoParam? = null

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mVideoDecoder.stop()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        val params = mEditVideoParam ?: return
        mVideoDecoder.prepare(params.videoPath, getHolder().surface)
        mVideoDecoder.start()
    }

    fun setEditParams(param: EditVideoParam) {
        mEditVideoParam = param
    }
}