package com.slim.me.camerasample.app

import android.app.Application
import android.content.Context

class BaseApplication : Application() {

    companion object {
        private lateinit var sApp : BaseApplication
        fun getIns() : BaseApplication{
            return sApp
        }
    }

    override fun attachBaseContext(base: Context?) {
        sApp = this
        super.attachBaseContext(base)
    }
}
