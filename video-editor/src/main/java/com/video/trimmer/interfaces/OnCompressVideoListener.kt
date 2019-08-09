package com.video.trimmer.interfaces

import android.net.Uri

interface OnCompressVideoListener {
    fun onCompressStarted()
    fun getResult(uri: Uri)
    fun cancelAction()
    fun onError(message: String)
}