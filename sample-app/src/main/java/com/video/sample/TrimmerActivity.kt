package com.video.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import kotlinx.android.synthetic.main.activity_trimmer.*
import java.io.File
import android.provider.MediaStore
import android.content.ContentResolver


class TrimmerActivity : AppCompatActivity(), OnTrimVideoListener, OnVideoListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trimmer)

        setupPermissions {
            val extraIntent = intent
            var path = ""
            if (extraIntent != null) path = extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_PATH)
            videoTrimmer.setTextTimeSelectionTypeface(FontsHelper[this, FontsConstants.SEMIBOLD])
            videoTrimmer.setTextTimeTypeface(FontsHelper[this, FontsConstants.SEMIBOLD])
            videoTrimmer.setOnTrimVideoListener(this)
            videoTrimmer.setOnVideoListener(this)
            videoTrimmer.setVideoURI(Uri.parse(path))
            videoTrimmer.setVideoInformationVisibility(true)
            videoTrimmer.destinationPath = Environment.getExternalStorageDirectory().toString() + File.separator + "Zoho Social" + File.separator + "Videos" + File.separator
        }

        back.setOnClickListener {
            videoTrimmer.onCancelClicked()
        }

        save.setOnClickListener {
            videoTrimmer.onSaveClicked()
        }
    }

    override fun onTrimStarted() {
        RunOnUiThread(this).safely {
            Toast.makeText(this, "Started Trimming", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getResult(uri: Uri) {
        RunOnUiThread(this).safely {
            Toast.makeText(this, getString(R.string.video_saved_at, uri.path), Toast.LENGTH_SHORT).show()
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            val id = getVideoIdFromFilePath(uri.path)
            Log.e("VIDEO ID", id.toString())
        }
    }

    private fun getVideoIdFromFilePath(filePath: String?): Long? {
        val videosUri = MediaStore.Video.Media.getContentUri("internal")
        val projection = arrayOf(MediaStore.Video.VideoColumns._ID)
        val cursor = contentResolver.query(videosUri, projection, MediaStore.Video.VideoColumns.DATA + " LIKE ?", arrayOf(filePath), null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(projection[0])
        val videoId = if (columnIndex != null) cursor.getLong(columnIndex) else null
        cursor?.close()
        return videoId
    }

    override fun cancelAction() {
        RunOnUiThread(this).safely {
            videoTrimmer.destroy()
            finish()
        }
    }

    override fun onError(message: String) {
        Log.e("ERROR", message)
    }

    override fun onVideoPrepared() {
        RunOnUiThread(this).safely {
            Toast.makeText(this, "onVideoPrepared", Toast.LENGTH_SHORT).show()
        }
    }

    lateinit var dothis: () -> Unit
    private fun setupPermissions(doSomething: () -> Unit) {
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        dothis = doSomething
        if (writePermission != PackageManager.PERMISSION_GRANTED && readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        } else dothis()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            101 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PermissionsDialog(this@TrimmerActivity, "To continue, give Zoho Social access to your Photos.").show()
                } else dothis()
            }
        }
    }
}
