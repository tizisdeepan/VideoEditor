package com.video.sample

import android.content.Context
import android.util.Log
import org.jetbrains.anko.runOnUiThread

class RunOnUiThread(var context: Context?) {
    fun safely(dothis: () -> Unit) {
        if (context != null) {
            context?.runOnUiThread {
                try {
                    dothis.invoke()
                } catch (e: Exception) {
                    Log.e("runonui - ${context!!::class.java.canonicalName}", e.toString())
                    e.printStackTrace()
                }
            }
        }
    }
}