package com.video.sample

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import kotlinx.android.synthetic.main.activity_trimmer.*
import java.io.File

class TrimmerActivity : AppCompatActivity(), OnTrimVideoListener, OnVideoListener {

    lateinit var mProgressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trimmer)

        setupPermissions {
            val extraIntent = intent
            var path = ""
            if (extraIntent != null) path = extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_PATH)
            mProgressDialog = ProgressDialog(this)
            mProgressDialog.setCancelable(false)
            mProgressDialog.setCanceledOnTouchOutside(false)
            mProgressDialog.setMessage(getString(R.string.trimming_progress))
            videoTrimmer.setMaxDuration(140)
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
        RunOnUiThread(this@TrimmerActivity).safely {
            Toast.makeText(this@TrimmerActivity, "onTrimStarted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getResult(uri: Uri) {
        RunOnUiThread(this@TrimmerActivity).safely {
            RunOnUiThread(this@TrimmerActivity).safely {
                Toast.makeText(this@TrimmerActivity, "getResult", Toast.LENGTH_SHORT).show()
            }
            runOnUiThread { Toast.makeText(this@TrimmerActivity, getString(R.string.video_saved_at, uri.path), Toast.LENGTH_SHORT).show() }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setDataAndType(uri, "video/*")
            startActivity(intent)
            finish()
        }
    }

    override fun cancelAction() {
        RunOnUiThread(this@TrimmerActivity).safely {
            Toast.makeText(this@TrimmerActivity, "cancelAction", Toast.LENGTH_SHORT).show()
            videoTrimmer.destroy()
            finish()
        }
    }

    override fun onError(message: String) {
        RunOnUiThread(this@TrimmerActivity).safely {
            mProgressDialog.cancel()
//            Toast.makeText(this@TrimmerActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onVideoPrepared() {
        RunOnUiThread(this@TrimmerActivity).safely {
            Toast.makeText(this@TrimmerActivity, "onVideoPrepared", Toast.LENGTH_SHORT).show()
        }
    }

    private val PERMISSIONS_REQUEST_CODE = 101
    lateinit var dothis: () -> Unit
    private fun setupPermissions(doSomething: () -> Unit) {
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        dothis = doSomething
        if (writePermission != PackageManager.PERMISSION_GRANTED && readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
        } else dothis()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PermissionsDialog(this@TrimmerActivity, "To continue, give Zoho Social access to your Photos.").show()
                } else dothis()
            }
        }
    }
}
