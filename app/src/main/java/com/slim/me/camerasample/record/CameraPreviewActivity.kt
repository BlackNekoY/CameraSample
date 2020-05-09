package com.slim.me.camerasample.record

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.FrameLayout

import com.slim.me.camerasample.R
import com.slim.me.camerasample.camera.CameraHelper

import com.slim.me.camerasample.util.UIUtil.getStatusBarHeight
import kotlin.collections.ArrayList

class CameraPreviewActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CameraPreviewActivity"
        const val PERMISSION_CAMERA_REQUEST_CODE = 100
    }

    private var mPreviewLayout: PreviewLayout? = null
    private lateinit var mPreviewStub: ViewStub

    override fun onCreate(savedInstanceState: Bundle?) {
        initStatusBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_record)
        mPreviewStub = findViewById(R.id.preview_stub)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            requestPermission()
        } else {
            startPreview()
        }
    }

    private fun initStatusBar() {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val decorViewGroup = window.decorView as ViewGroup
        val statusBarView = View(window.context)
        val statusBarHeight = getStatusBarHeight(window.context)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, statusBarHeight)
        params.gravity = Gravity.TOP
        statusBarView.layoutParams = params
        statusBarView.setBackgroundColor(Color.TRANSPARENT)
        decorViewGroup.addView(statusBarView)
    }

    private fun requestPermission() {
        val requestPermissions = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (requestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toTypedArray(), PERMISSION_CAMERA_REQUEST_CODE)
        } else {
            startPreview()
        }
    }

    private fun startPreview() {
        mPreviewLayout = mPreviewStub.inflate() as PreviewLayout?
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                var requestSuccess = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        requestSuccess = false
                    }
                }
                if (requestSuccess) {
                    startPreview()
                } else {
                    finish()
                }
            } else {
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mPreviewLayout?.let {
            CameraHelper.getInstance().stopPreview()
            CameraHelper.getInstance().releaseCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPreviewLayout?.run {
            CameraHelper.getInstance().stopPreview()
            CameraHelper.getInstance().releaseCamera()
            onDestroy()
        }
    }
}
