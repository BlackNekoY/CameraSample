package com.slim.me.camerasample.edit

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.slim.me.camerasample.R

class EditVideoFragment : Fragment() {

    private lateinit var mEditVideoParam: EditVideoParam

    private lateinit var mRootView: View
    private lateinit var mEditSurfaceView: DecodeGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initParams()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRootView =  inflater.inflate(R.layout.fragment_edit, null)
        initSurfaceView()
        return mRootView
    }

    private fun initParams() {
        mEditVideoParam = arguments.getParcelable(EditVideoActivity.EDIT_PARAMS) ?: return
    }

    private fun initSurfaceView() {
        mEditSurfaceView = mRootView.findViewById(R.id.edit_surface_view)
        mEditSurfaceView.setEditParams(mEditVideoParam)
    }
}