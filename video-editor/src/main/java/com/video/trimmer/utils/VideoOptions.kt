package com.video.trimmer.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.video.trimmer.interfaces.OnCompressVideoListener
import com.video.trimmer.interfaces.OnTrimVideoListener

class VideoOptions(private var ctx: Context) {
    companion object {
        const val TAG = "VideoOptions"
    }

    fun trimVideo(startPosition: String, endPosition: String, inputPath: String, outputPath: String, outputFileUri: Uri, listener: OnTrimVideoListener?) {
        val ff = FFmpeg.getInstance(ctx)
        ff.loadBinary(object : FFmpegLoadBinaryResponseHandler {
            override fun onFinish() {
                Log.e("FFmpegLoad", "onFinish")
            }

            override fun onSuccess() {
                Log.e("FFmpegLoad", "onSuccess")
                val command = arrayOf("-y", "-i", inputPath, "-ss", startPosition, "-to", endPosition, "-c", "copy", outputPath)
                try {
                    ff.execute(command, object : ExecuteBinaryResponseHandler() {
                        override fun onSuccess(message: String?) {
                            super.onSuccess(message)
                            Log.e(TAG, "onSuccess: " + message!!)
                        }

                        override fun onProgress(message: String?) {
                            super.onProgress(message)
                            listener?.onError(message.toString())
                            Log.e(TAG, "onProgress: " + message!!)
                        }

                        override fun onFailure(message: String?) {
                            super.onFailure(message)
                            listener?.onError(message.toString())
                            Log.e(TAG, "onFailure: " + message!!)
                        }

                        override fun onStart() {
                            super.onStart()
                            Log.e(TAG, "onStart: ")
                        }

                        override fun onFinish() {
                            super.onFinish()
                            listener?.getResult(outputFileUri)
                            Log.e(TAG, "onFinish: ")
                        }
                    })
                } catch (e: FFmpegCommandAlreadyRunningException) {
                    listener?.onError(e.toString())
                }
            }

            override fun onFailure() {
                Log.e("FFmpegLoad", "onFailure")
                listener?.onError("Failed")
            }

            override fun onStart() {
            }
        })
        listener?.onTrimStarted()
    }

    fun compressVideo(inputPath: String, outputPath: String, outputFileUri: Uri, width: String, height: String, listener: OnCompressVideoListener?) {
        val ff = FFmpeg.getInstance(ctx)
        ff.loadBinary(object : FFmpegLoadBinaryResponseHandler {
            override fun onFinish() {
                Log.e("FFmpegLoad", "onFinish")
            }

            override fun onSuccess() {
                Log.e("FFmpegLoad", "onSuccess")
                val command = arrayOf("-i", inputPath, "-vf", "scale=$width:$height", outputPath) //iw:ih
                try {
                    ff.execute(command, object : ExecuteBinaryResponseHandler() {
                        override fun onSuccess(message: String?) {
                            super.onSuccess(message)
                            Log.e(TAG, "onSuccess: " + message!!)
                        }

                        override fun onProgress(message: String?) {
                            super.onProgress(message)
                            listener?.onError(message.toString())
                            Log.e(TAG, "onProgress: " + message!!)
                        }

                        override fun onFailure(message: String?) {
                            super.onFailure(message)
                            listener?.onError(message.toString())
                            Log.e(TAG, "onFailure: " + message!!)
                        }

                        override fun onStart() {
                            super.onStart()
                            Log.e(TAG, "onStart: ")
                        }

                        override fun onFinish() {
                            super.onFinish()
                            listener?.getResult(outputFileUri)
                            Log.e(TAG, "onFinish: ")
                        }
                    })
                } catch (e: FFmpegCommandAlreadyRunningException) {
                    listener?.onError(e.toString())
                }
            }

            override fun onFailure() {
                Log.e("FFmpegLoad", "onFailure")
                listener?.onError("Failed")
            }

            override fun onStart() {
            }
        })
        listener?.onCompressStarted()
    }
}