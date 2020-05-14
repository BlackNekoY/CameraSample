package com.slim.me.camerasample.edit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.slim.me.camerasample.R
import com.slim.me.camerasample.util.FileUtils

class EditVideoActivity : AppCompatActivity() {

    private var mEditVideoParam: EditVideoParam? = null
    private var mFragment: EditVideoFragment? = null

    companion object {
        const val TAG = "EditVideoActivity"
        const val EDIT_PARAMS = "EDIT_PARAMS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        initParams()

        mFragment = EditVideoFragment()
        val arguments = Bundle()
        arguments.putParcelable(EDIT_PARAMS, mEditVideoParam)
        mFragment?.arguments = arguments
        supportFragmentManager.beginTransaction()
                .add(R.id.edit_fragment_container, mFragment)
                .commit()
    }

    private fun initParams() {
        intent?.let {
            mEditVideoParam = it.getParcelableExtra(EDIT_PARAMS)
        }
        if (mEditVideoParam == null) {
            finish()
        }
        if (TextUtils.isEmpty(mEditVideoParam?.videoPath) && FileUtils.isFileExist(mEditVideoParam?.videoPath)) {
            finish()
        }
    }
}