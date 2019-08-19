package com.video.sample

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import kotlinx.android.synthetic.main.progress_loading.*

class VideoProgressDialog(private var ctx: Context, private var message: String) : Dialog(ctx) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_loading)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        messageLabel.text = message

        messageLabel.typeface = FontsHelper[ctx, FontsConstants.SEMI_BOLD]
    }


    fun setProgress(progress: Float) {
        pieProgress.setProgress(progress)
    }
}