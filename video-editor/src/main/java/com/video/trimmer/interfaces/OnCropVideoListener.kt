package com.video.trimmer.interfaces

import android.net.Uri

interface OnCropVideoListener {
    fun onCropStarted()
    fun getResult(uri: Uri)
    fun cancelAction()
    fun onError(message: String)
    fun onProgress(progress: Float)
}