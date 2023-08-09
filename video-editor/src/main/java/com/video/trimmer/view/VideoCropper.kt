package com.video.trimmer.view

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import com.video.trimmer.databinding.ViewCropperBinding
import com.video.trimmer.interfaces.OnVideoEditedListener
import com.video.trimmer.utils.BackgroundExecutor
import java.io.File
import java.util.*

class VideoCropper @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCropperBinding

    private lateinit var mSrc: Uri
    private var mOnCropVideoListener: OnVideoEditedListener? = null
    private var mFinalPath: String? = null
    private var mMinRatio: Float = 1f
    private var mMaxRatio: Float = 1.78f
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var destinationPath: String
        get() {
            if (mFinalPath == null) {
                val folder = Environment.getExternalStorageDirectory()
                mFinalPath = folder.path + File.separator
            }
            return mFinalPath ?: ""
        }
        set(finalPath) {
            mFinalPath = finalPath
        }

    init {
        init(context)
    }

    private fun init(context: Context) {
        binding = ViewCropperBinding.inflate(LayoutInflater.from(context), this, true)
        setUpListeners()
    }

    private fun setUpListeners() {
        binding.cropSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onCropProgressChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                loadFrame(seekBar?.progress ?: 0)
            }
        })
    }

    fun onCropProgressChanged(progress: Int) {
        val width: Int
        val height: Int
        val progressRatio = 2
        if (videoWidth > videoHeight) {
            height = (videoWidth / progressRatio).toInt()
            width = videoWidth
        } else {
            width = (progressRatio * videoHeight).toInt()
            height = videoHeight
        }
        binding.cropFrame.setAspectRatio(width, height)
    }

    fun setVideoURI(videoURI: Uri): VideoCropper {
        mSrc = videoURI
        binding.timeLineView.setVideo(mSrc)
        loadFrame(0)
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        videoWidth = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()  ?: 0
        videoHeight = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        return this
    }

    fun onCancelClicked() {
        mOnCropVideoListener?.cancelAction()
    }

    fun setOnCropVideoListener(onVideoEditedListener: OnVideoEditedListener): VideoCropper {
        mOnCropVideoListener = onVideoEditedListener
        return this
    }

    fun setDestinationPath(path: String): VideoCropper {
        destinationPath = path
        return this
    }

    fun setMinMaxRatios(minRatio: Float, maxRatio: Float): VideoCropper {
        mMinRatio = minRatio
        mMaxRatio = maxRatio
        binding.cropFrame.setFixedAspectRatio(true)
        onCropProgressChanged(50)
      //  binding.cropSeekbar.progress = 50
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
                        /*    context.runOnUiThread {
                                binding.cropFrame.setImageBitmap(bitmap)
                            }*/
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    mediaMetadataRetriever.release()
                } catch (e: Throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
            }
        })
    }
}