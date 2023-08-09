package com.video.sample

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.video.sample.databinding.ActivityTrimmerBinding
import com.video.sample.databinding.DialogPermissionsBinding

class PermissionsDialog(var ctx: Context, var msg: String) : Dialog(ctx) {
    private lateinit var binding: DialogPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogPermissionsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.message.text = msg

        binding.dismiss.setOnClickListener {
            dismiss()
        }

        binding.settings.setOnClickListener {
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID))
            ctx.startActivity(i)
            dismiss()
        }

        binding.permissionsTitle.typeface = FontsHelper[ctx, FontsConstants.BOLD]
        binding.message.typeface = FontsHelper[ctx, FontsConstants.SEMI_BOLD]
        binding.dismiss.typeface = FontsHelper[ctx, FontsConstants.SEMI_BOLD]
        binding.settings.typeface = FontsHelper[ctx, FontsConstants.SEMI_BOLD]
    }
}