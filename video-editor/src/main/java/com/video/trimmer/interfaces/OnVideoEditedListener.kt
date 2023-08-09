package com.video.trimmer.interfaces

import android.net.Uri

interface OnVideoEditedListener {
    fun onTrimStarted()
    fun getResult(uri: Uri)
    fun cancelAction()
    fun onError(message: String)
}
