package com.video.trimmer.interfaces

interface OnProgressVideoListener {
    fun updateProgress(time: Float, max: Float, scale: Float)
}