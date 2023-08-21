package com.video.sample

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.video.sample.databinding.ActivityVideoEditorBinding
import com.video.trimmer.interfaces.OnVideoEditedListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.utils.fileFromContentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class EditorActivity : AppCompatActivity(), OnVideoEditedListener, OnVideoListener {

    private val progressDialog: VideoProgressIndeterminateDialog by lazy {
        VideoProgressIndeterminateDialog(
            this,
            "Cropping video. Please wait..."
        )
    }
    private lateinit var binding: ActivityVideoEditorBinding

    private var tempFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupPermissions {
            val extraIntent = intent
            val uri: Uri?
            if (extraIntent != null) {
                uri = Uri.parse(
                    extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_URI_STRING).toString()
                )
                tempFile = uri?.fileFromContentUri(this)
            }

            binding.videoTrimmer.setTextTimeSelectionTypeface(FontsHelper[this, FontsConstants.SEMI_BOLD])
                .setOnTrimVideoListener(this)
                .setOnVideoListener(this)
                .setVideoURI(Uri.parse(tempFile?.path))
                .setBitrate(2)
                .setVideoInformationVisibility(true)
                .setMinDuration(2)
                .setDestinationPath(Environment.getExternalStorageDirectory().path + File.separator + Environment.DIRECTORY_MOVIES)
        }

        binding.back.setOnClickListener {
            binding.videoTrimmer.onCancelClicked()
        }


        binding.save.setOnClickListener {
            binding.videoTrimmer.onSaveClicked()
        }
    }

    override fun onTrimStarted() {
        val context = applicationContext
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Started Trimming", Toast.LENGTH_SHORT).show()
            progressDialog.show()
        }
    }

    override fun getResult(uri: Uri) {
        val context = applicationContext
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Video saved at ${uri.path}", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()

            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, uri)
            val duration =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong()
            val width =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toLong()
            val height =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toLong()
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DATA, uri.path)
            values.put(MediaStore.Video.VideoColumns.DURATION, duration)
            values.put(MediaStore.Video.VideoColumns.WIDTH, width)
            values.put(MediaStore.Video.VideoColumns.HEIGHT, height)
            val id = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?.let { ContentUris.parseId(it) }
            Log.e("VIDEO ID", id.toString())
        }

    }

    override fun cancelAction() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.videoTrimmer.destroy()
            // delete temp file after use it
            tempFile?.delete()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.videoTrimmer.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.videoTrimmer.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // delete temp file after use it
        tempFile?.delete()
    }

    override fun onError(message: String) {
        val context = applicationContext
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
        Log.e("ERROR", message)
        // delete temp file after use it
        tempFile?.delete()
    }

    override fun onProgress(percentage: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            progressDialog.updateProgress("Cropping video. Please wait..  " +
                    "$percentage %")
        }
    }

    override fun onVideoPrepared() {
        Toast.makeText(this, "onVideoPrepared", Toast.LENGTH_SHORT).show()
    }

    lateinit var doThis: () -> Unit
    private fun setupPermissions(doSomething: () -> Unit) {
        val writePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        doThis = doSomething
        if (writePermission != PackageManager.PERMISSION_GRANTED && readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                101
            )
        } else doThis()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PermissionsDialog(
                        this@EditorActivity,
                        "To continue, give Zoho Social access to your Photos."
                    ).show()
                } else doThis()
            }
        }
    }
}
