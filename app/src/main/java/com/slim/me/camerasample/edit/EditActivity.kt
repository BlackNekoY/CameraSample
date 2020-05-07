package com.slim.me.camerasample.edit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils

class EditActivity : AppCompatActivity() {

    private lateinit var mVideoPath: String

    companion object {
        const val TAG = "EditActivity"
        const val VIDEO_PATH = "video_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initParams()
    }

    private fun initParams() {
        intent?.let {
            mVideoPath = it.getStringExtra(VIDEO_PATH)
        }
        if (TextUtils.isEmpty(mVideoPath)) {
            finish()
        }
    }
}