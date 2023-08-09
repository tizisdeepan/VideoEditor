package com.video.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.video.sample.databinding.ActivityCropperBinding

class CropperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropperBinding

    private val progressDialog: VideoProgressDialog by lazy {
        VideoProgressDialog(
            this,
            "Cropping video. Please wait..."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropperBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.back.setOnClickListener {
            binding.videoCropper.onCancelClicked()
        }

        binding.save.setOnClickListener {
       //     binding.videoCropper.onSaveClicked()
        }
    }

}