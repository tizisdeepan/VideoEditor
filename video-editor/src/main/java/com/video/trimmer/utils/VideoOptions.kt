package com.video.trimmer.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.video.trimmer.interfaces.OnCompressVideoListener
import com.video.trimmer.interfaces.OnCropVideoListener
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

    fun cropVideo(width: Int, height: Int, x: Int, y: Int, inputPath: String, outputPath: String, outputFileUri: Uri, listener: OnCropVideoListener?, frameCount: Int) {
        val ff = FFmpeg.getInstance(ctx)
        ff.loadBinary(object : FFmpegLoadBinaryResponseHandler {
            override fun onFinish() {
                Log.e("FFmpegLoad", "onFinish")
            }

            override fun onSuccess() {
                Log.e("FFmpegLoad", "onSuccess")
                val command = arrayOf("-i", inputPath, "-filter:v", "crop=$width:$height:$x:$y", "-threads", "5", "-preset", "ultrafast", "-strict", "-2", "-c:a", "copy", outputPath)
                try {
                    ff.execute(command, object : ExecuteBinaryResponseHandler() {
                        override fun onSuccess(message: String?) {
                            super.onSuccess(message)
                            Log.e(TAG, "onSuccess: " + message!!)
                        }

                        override fun onProgress(message: String?) {
                            super.onProgress(message)
                            if (message != null) {
                                val messageArray = message.split("frame=")
                                if (messageArray.size >= 2) {
                                    val secondArray = messageArray[1].trim().split(" ")
                                    if (secondArray.isNotEmpty()) {
                                        val framesString = secondArray[0].trim()
                                        try {
                                            val frames = framesString.toInt()
                                            val progress = (frames.toFloat() / frameCount.toFloat()) * 100f
                                            listener?.onProgress(progress)
                                        } catch (e: Exception) {
                                        }
                                    }
                                }
                            }
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
        listener?.onCropStarted()
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