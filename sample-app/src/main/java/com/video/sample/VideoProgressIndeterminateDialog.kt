package com.video.sample

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.video.sample.databinding.ProgressLoadingIndeterminateBinding

class VideoProgressIndeterminateDialog(private var ctx: Context, private var message: String) : Dialog(ctx) {

    private lateinit var binding: ProgressLoadingIndeterminateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProgressLoadingIndeterminateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binding.messageLabel.text = message

        binding.messageLabel.typeface = FontsHelper[ctx, FontsConstants.SEMI_BOLD]
    }
}