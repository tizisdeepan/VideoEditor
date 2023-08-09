package com.video.trimmer.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.video.trimmer.interfaces.OnVideoEditedListener


class VideoOptions(private var ctx: Context) {
    companion object {
        const val TAG = "VideoOptions"
    }

    fun trimAndCropVideo(
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        frameCount: Int,
        startPosition: String,
        endPosition: String,
        inputPath: String,
        outputPath: String,
        outputFileUri: Uri,
        listener: OnVideoEditedListener?
    ) {
        val command = StringBuilder()
            .append("-y") //overWrite
            .append(" -i ").append(inputPath)
            .append(" -ss ").append(startPosition)
            .append(" -to ").append(endPosition)
            .append(" -filter:v ")
            .append(" crop=").append("$width:$height:$x:$y")
            .append(" -threads ")
            .append(" 5 ")
            .append(" -preset ")
            .append(" ultrafast ")
            .append(" -strict ")
            .append(" -2 ")
            .append(" -c:a copy ").append(outputPath)
        val session = FFmpegKit.execute(command.toString())
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
             listener?.getResult(outputFileUri)
            Log.e(TAG, "onFinish: ")
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
            listener?.onError("CANCEL")
            Log.e(TAG, "isCancel: " + "CANCEL")
        } else {
            // FAILURE
            listener?.onError("Failed")
            Log.e(TAG, "onFailure: " + "Failed")
        }

    }

}