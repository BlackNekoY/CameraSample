package com.slim.me.camerasample.task

import java.lang.Exception

abstract class TaskThread(threadName: String) : Thread(threadName) {

    final override fun run() {
        try {
            onTaskPreRun()
            while (isTaskWorking() && !isInterrupted) {
                try {
                    onTaskRunning()
                } catch (e: InterruptedException) {
                    break
                }
            }
        } catch (e: Exception) {
            onTaskException(e)
        } finally {
            onTaskFinish()
        }
    }

    protected abstract fun isTaskWorking() : Boolean
    protected open fun onTaskPreRun() {}
    protected open fun onTaskRunning() {}
    protected open fun onTaskFinish() {}
    protected open fun onTaskException(e: Exception) {
        e.printStackTrace()
    }
}