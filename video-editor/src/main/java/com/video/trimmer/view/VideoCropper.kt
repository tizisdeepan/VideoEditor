package com.video.trimmer.view

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import com.video.trimmer.R
import com.video.trimmer.interfaces.OnCropVideoListener
import com.video.trimmer.utils.BackgroundExecutor
import com.video.trimmer.utils.RealPathUtil
import com.video.trimmer.utils.VideoOptions
import kotlinx.android.synthetic.main.view_cropper.view.*
import org.jetbrains.anko.runOnUiThread
import java.io.File
import java.util.*
import kotlin.math.abs
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.android.synthetic.main.view_cropper.view.timeLineView

class VideoCropper @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var mSrc: Uri
    private var mOnCropVideoListener: OnCropVideoListener? = null
    private var mFinalPath: String? = null
    private var destinationPath: String
        get() {
            if (mFinalPath == null) {
                val folder = Environment.getExternalStorageDirectory()
                mFinalPath = folder.path + File.separator
                Log.e(TAG, "Using default path " + mFinalPath!!)
            }
            return mFinalPath ?: ""
        }
        set(finalPath) {
            mFinalPath = finalPath
            Log.e(TAG, "Setting custom path " + mFinalPath!!)
        }

    init {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_cropper, this, true)
        setUpListeners()
    }

    private fun setUpListeners() {
        handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                loadFrame(seekBar?.progress ?: 0)
            }
        })

        cropFrame.setOnSetCropOverlayReleasedListener {
            Log.e("RECT", it.toString())
        }
    }

    fun setVideoURI(videoURI: Uri): VideoCropper {
        mSrc = videoURI
        timeLineView.setVideo(mSrc)
        loadFrame(0)
        return this
    }

    fun onSaveClicked() {
        val rect = cropFrame.cropRect
        val width = abs(rect.left - rect.right)
        val height = abs(rect.top - rect.bottom)
        val x = rect.left
        val y = rect.top
        val file = File(mSrc.path)
        val root = File(destinationPath)
        root.mkdirs()
        val outputFileUri = Uri.fromFile(File(root, "t_${Calendar.getInstance().timeInMillis}_" + file.nameWithoutExtension + ".mp4"))
        val outPutPath = RealPathUtil.realPathFromUriApi19(context, outputFileUri)
                ?: File(root, "t_${Calendar.getInstance().timeInMillis}_" + mSrc.path.substring(mSrc.path.lastIndexOf("/") + 1)).absolutePath
        val extractor = MediaExtractor()
        var frameRate = 24
        try {
            extractor.setDataSource(file.path)
            val numTracks = extractor.trackCount
            for (i in 0..numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        val duration = java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        val frameCount = duration / 1000 * frameRate
        Log.e("FRAME RATE", frameRate.toString())
        Log.e("FRAME COUNT", (duration / 1000 * frameRate).toString())
        VideoOptions(context).cropVideo(width, height, x, y, file.path, outPutPath, outputFileUri, mOnCropVideoListener, frameCount.toInt())
    }

    fun onCancelClicked() {
        mOnCropVideoListener?.cancelAction()
    }

    fun setOnCropVideoListener(onTrimVideoListener: OnCropVideoListener): VideoCropper {
        mOnCropVideoListener = onTrimVideoListener
        return this
    }

    fun setDestinationPath(path: String): VideoCropper {
        destinationPath = path
        return this
    }

    private fun loadFrame(progress: Int) {
        BackgroundExecutor.execute(object : BackgroundExecutor.Task("", 0L, "") {
            override fun execute() {
                try {
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, mSrc)
                    val videoLengthInMs = (Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000).toLong()
                    val seekDuration = (videoLengthInMs * progress) / 1000
                    val bitmap = mediaMetadataRetriever.getFrameAtTime(seekDuration * 10, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        try {
                            context.runOnUiThread {
                                cropFrame.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    mediaMetadataRetriever.release()
                } catch (e: Throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
            }
        }
        )
    }

    companion object {
        private val TAG = VideoCropper::class.java.simpleName
    }
}