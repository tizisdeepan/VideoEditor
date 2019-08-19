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
import com.video.trimmer.interfaces.OnCropVideoListener
import kotlinx.android.synthetic.main.activity_cropper.*
import kotlinx.android.synthetic.main.activity_cropper.back
import kotlinx.android.synthetic.main.activity_cropper.save
import java.io.File

class CropperActivity : AppCompatActivity(), OnCropVideoListener {

    private val progressDialog: VideoProgressDialog by lazy { VideoProgressDialog(this, "Cropping video. Please wait...") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cropper)

        setupPermissions {
            val extraIntent = intent
            var path = ""
            if (extraIntent != null) path = extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_PATH)
            videoCropper.setVideoURI(Uri.parse(path))
                    .setOnCropVideoListener(this)
                    .setMinMaxRatios(0.3f, 3f)
                    .setDestinationPath(Environment.getExternalStorageDirectory().toString() + File.separator + "Zoho Social" + File.separator + "Videos" + File.separator)
        }

        back.setOnClickListener {
            videoCropper.onCancelClicked()
        }

        save.setOnClickListener {
            videoCropper.onSaveClicked()
        }
    }

    override fun onCropStarted() {
        RunOnUiThread(this).safely {
            Toast.makeText(this, "Started Cropping", Toast.LENGTH_SHORT).show()
            progressDialog.show()
        }
    }

    override fun getResult(uri: Uri) {
        RunOnUiThread(this).safely {
            progressDialog.dismiss()
            RunOnUiThread(this).safely {
                Toast.makeText(this, "Video saved at ${uri.path}", Toast.LENGTH_SHORT).show()
            }
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(this, uri)
            val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toLong()
            val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toLong()
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DATA, uri.path)
            values.put(MediaStore.Video.VideoColumns.DURATION, duration)
            values.put(MediaStore.Video.VideoColumns.WIDTH, width)
            values.put(MediaStore.Video.VideoColumns.HEIGHT, height)
            val id = ContentUris.parseId(contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values))
            Log.e("VIDEO ID", id.toString())
        }
    }

    override fun cancelAction() {
        RunOnUiThread(this).safely {
            finish()
        }
    }

    override fun onError(message: String) {
        Log.e("ERROR", message)
    }

    override fun onProgress(progress: Float) {
        RunOnUiThread(this).safely {
            progressDialog.setProgress(progress)
        }
    }

    lateinit var doThis: () -> Unit
    private fun setupPermissions(doSomething: () -> Unit) {
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        doThis = doSomething
        if (writePermission != PackageManager.PERMISSION_GRANTED && readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        } else doThis()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            101 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PermissionsDialog(this@CropperActivity, "To continue, give Zoho Social access to your Photos.").show()
                } else doThis()
            }
        }
    }
}