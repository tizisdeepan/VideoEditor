package com.video.trimmer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import com.video.trimmer.R
import com.video.trimmer.databinding.ViewTrimmerBinding
import com.video.trimmer.interfaces.OnProgressVideoListener
import com.video.trimmer.interfaces.OnRangeSeekBarListener
import com.video.trimmer.interfaces.OnVideoEditedListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.utils.BackgroundExecutor
import com.video.trimmer.utils.RealPathUtil
import com.video.trimmer.utils.TrimVideoUtils
import com.video.trimmer.utils.UiThreadExecutor
import com.video.trimmer.utils.VideoOptions
import com.video.trimmer.utils.drawableToBitmap
import com.video.trimmer.utils.fileFromContentUri
import java.io.File
import java.lang.ref.WeakReference
import java.util.Calendar
import kotlin.math.abs

class VideoTrimmer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var mSrc: Uri
    private var mFinalPath: String? = null

    private var mMaxDuration: Int = -1
    private var mMinDuration: Int = -1
    private var mListeners: ArrayList<OnProgressVideoListener> = ArrayList()

    private var mOnVideoEditedListener: OnVideoEditedListener? = null
    private var mOnVideoListener: OnVideoListener? = null

    private lateinit var binding: ViewTrimmerBinding

    private var mDuration = 0f
    private var mTimeVideo = 0f
    private var mStartPosition = 0f

    private var mEndPosition = 0f
    private var mResetSeekBar = true
    private val mMessageHandler = MessageHandler(this)
    private var originalVideoWidth: Int = 0
    private var originalVideoHeight: Int = 0
    private var videoPlayerWidth: Int = 0
    private var videoPlayerHeight: Int = 0
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
        binding = ViewTrimmerBinding.inflate(LayoutInflater.from(context), this, true)
        setUpListeners()
        setUpMargins()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        mListeners = ArrayList()
        mListeners.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Float, max: Float, scale: Float) {
                updateVideoProgress(time)
            }
        })

        val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            })

        binding.iconVideoPlay.setOnClickListener {
            onClickVideoPlayPause()
        }
        binding.videoLoader.setOnErrorListener { _, what, _ ->
            mOnVideoEditedListener?.onError("Something went wrong reason : $what")
            false
        }

        binding.layoutSurfaceView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })

        binding.timeLineBar.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                binding.handlerTop.visibility = View.GONE
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })

        binding.videoLoader.setOnPreparedListener { mp -> onVideoPrepared(mp) }
        binding.videoLoader.setOnCompletionListener { onVideoCompleted() }
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        val duration = (mDuration * progress / 1000L)
        if (fromUser) {
            if (duration < mStartPosition) setProgressBarPosition(mStartPosition)
            else if (duration > mEndPosition) setProgressBarPosition(mEndPosition)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        binding.videoLoader.pause()
        binding.iconVideoPlay.visibility = View.VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        binding.videoLoader.pause()
        binding.iconVideoPlay.visibility = View.VISIBLE

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        binding.videoLoader.seekTo(duration)
        notifyProgressUpdate(false)
    }

    private fun setProgressBarPosition(position: Float) {
        if (mDuration > 0) binding.handlerTop.progress = (1000L * position / mDuration).toInt()
    }

    private fun setUpMargins() {
        val marge = binding.timeLineBar.thumbs[0].widthBitmap
        val lp = binding.timeLineView.layoutParams as LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        binding.timeLineView.layoutParams = lp
    }

    fun handleUi() {
        binding.iconVideoPlay.visibility = View.VISIBLE
        binding.videoLoader.pause()
    }

    fun onSaveClicked() {
        mOnVideoEditedListener?.onTrimStarted()

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        val metaDataKeyDuration =
            java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))

        val file = mSrc.fileFromContentUri(context)

        if (mTimeVideo < MIN_TIME_FRAME) {
            if (metaDataKeyDuration - mEndPosition > MIN_TIME_FRAME - mTimeVideo) mEndPosition += MIN_TIME_FRAME - mTimeVideo
            else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) mStartPosition -= MIN_TIME_FRAME - mTimeVideo
        }

        val root = File(destinationPath)
        root.mkdirs()
        val outputFileUri = Uri.fromFile(
            File(
                root,
                "t_${Calendar.getInstance().timeInMillis}_" + file.nameWithoutExtension + ".mp4"
            )
        )
        val outPutPath = RealPathUtil.realPathFromUriApi19(context, outputFileUri)
            ?: File(
                root,
                "t_${Calendar.getInstance().timeInMillis}_" + mSrc.path?.substring(
                    mSrc.path!!.lastIndexOf("/") + 1
                )
            ).absolutePath
        Log.e("SOURCE", file.path)
        Log.e("DESTINATION", outPutPath)
        val extractor = MediaExtractor()
        var frameRate = 24
        try {
            extractor.setDataSource(file.path)
            val numTracks = extractor.trackCount
            for (i in 0..numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }
        val duration =
            java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        Log.e("FRAME RATE", frameRate.toString())
        Log.e("FRAME COUNT", (duration / 1000 * frameRate).toString())
        handleVideoCrop(file)
    }

    private fun handleVideoCrop(file: File) {
        val rect = binding.cropFrame.cropRect ?: return
        val width = abs(rect.left - rect.right)
        val height = abs(rect.top - rect.bottom)
        val x = rect.left
        val y = rect.top
        val root = File(destinationPath)
        root.mkdirs()
        val outputFileUri = Uri.fromFile(
            File(
                root,
                "t_${Calendar.getInstance().timeInMillis}_" + file.nameWithoutExtension + ".mp4"
            )
        )
        val outPutPath = RealPathUtil.realPathFromUriApi19(context, outputFileUri)
            ?: File(
                root,
                "t_${Calendar.getInstance().timeInMillis}_" + mSrc.path?.substring(
                    mSrc.path!!.lastIndexOf("/") + 1
                )
            ).absolutePath
        val extractor = MediaExtractor()
        var frameRate = 24
        try {
            extractor.setDataSource(file.absolutePath)
            val numTracks = extractor.trackCount
            for (i in 0..numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
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
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.let { java.lang.Long.parseLong(it) } ?: 0
        val frameCount = duration / 1000 * frameRate
        val xPercentage = (x * 100) / videoPlayerWidth
        val yPercentage = (y * 100) / videoPlayerHeight

        var width2: Int = width
        var height2: Int = height
        var x2: Int = x
        var y2: Int = y

        width2 = (width * originalVideoWidth) / videoPlayerWidth
        x2 = originalVideoWidth * xPercentage / 100

        height2 = (height * originalVideoHeight) / videoPlayerHeight
        y2 = originalVideoHeight * (yPercentage + 1)  / 100

        VideoOptions(context).trimAndCropVideo(
            width2,
            height2,
            x2,
            y2,
            frameCount.toInt(),
            TrimVideoUtils.stringForTime(mStartPosition),
            TrimVideoUtils.stringForTime(mEndPosition),
            file.path,
            outPutPath,
            outputFileUri,
            mOnVideoEditedListener
        )
    }

    private fun onClickVideoPlayPause() {
        if (binding.videoLoader.isPlaying) {
            binding.iconVideoPlay.visibility = View.VISIBLE
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            binding.videoLoader.pause()
        } else {
            binding.iconVideoPlay.visibility = View.GONE
            if (mResetSeekBar) {
                mResetSeekBar = false
                binding.videoLoader.seekTo(mStartPosition.toInt())
            }
            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
            binding.videoLoader.start()
        }
    }

    fun onCancelClicked() {
        binding.videoLoader.stopPlayback()
        mOnVideoEditedListener?.cancelAction()
    }

    private fun onVideoPrepared(mp: MediaPlayer) {
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = binding.layoutSurfaceView.width
        val screenHeight = binding.layoutSurfaceView.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = binding.videoLoader.layoutParams

        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        videoPlayerWidth = lp.width
        videoPlayerHeight = lp.height
        binding.videoLoader.layoutParams = lp
        setupCropper(lp.width, lp.height)

        binding.iconVideoPlay.visibility = View.VISIBLE
        mDuration = binding.videoLoader.duration.toFloat()
        setSeekBarPosition()
        setTimeFrames()
        mOnVideoListener?.onVideoPrepared()

    }

    private fun setSeekBarPosition() {
        when {
            mDuration >= mMaxDuration && mMaxDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMaxDuration / 2
                mEndPosition = mDuration / 2 + mMaxDuration / 2
                binding.timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                binding.timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }

            mDuration <= mMinDuration && mMinDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMinDuration / 2
                mEndPosition = mDuration / 2 + mMinDuration / 2
                binding.timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                binding.timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }

            else -> {
                mStartPosition = 0f
                mEndPosition = mDuration
            }
        }
        binding.videoLoader.seekTo(mStartPosition.toInt())
        mTimeVideo = mDuration
        binding.timeLineBar.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        binding.textTimeSelection.text = String.format(
            "%s %s - %s %s",
            TrimVideoUtils.stringForTime(mStartPosition),
            seconds,
            TrimVideoUtils.stringForTime(mEndPosition),
            seconds
        )
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L)
                binding.videoLoader.seekTo(mStartPosition.toInt())
            }

            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L)
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        binding.videoLoader.pause()
        binding.iconVideoPlay.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        binding.videoLoader.seekTo(mStartPosition.toInt())
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0f) return
        val position = binding.videoLoader.currentPosition
        if (all) {
            for (item in mListeners) {
                item.updateProgress(position.toFloat(), mDuration, (position * 100 / mDuration))
            }
        } else {
            mListeners[0].updateProgress(
                position.toFloat(),
                mDuration,
                (position * 100 / mDuration)
            )
        }
    }

    private fun updateVideoProgress(time: Float) {
        if (binding.videoLoader == null) return
        if (time <= mStartPosition && time <= mEndPosition) binding.handlerTop.visibility =
            View.GONE
        else binding.handlerTop.visibility = View.VISIBLE
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            binding.videoLoader.pause()
            binding.iconVideoPlay.visibility = View.VISIBLE
            mResetSeekBar = true
            return
        }
        setProgressBarPosition(time)
    }

    fun setVideoInformationVisibility(visible: Boolean): VideoTrimmer {
        binding.timeFrame.visibility = if (visible) View.VISIBLE else View.GONE
        return this
    }

    fun setOnTrimVideoListener(onVideoEditedListener: OnVideoEditedListener): VideoTrimmer {
        mOnVideoEditedListener = onVideoEditedListener
        return this
    }

    fun setOnVideoListener(onVideoListener: OnVideoListener): VideoTrimmer {
        mOnVideoListener = onVideoListener
        return this
    }

    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    fun setMaxDuration(maxDuration: Int): VideoTrimmer {
        mMaxDuration = maxDuration * 1000
        return this
    }

    fun setMinDuration(minDuration: Int): VideoTrimmer {
        mMinDuration = minDuration * 1000
        return this
    }

    fun setDestinationPath(path: String): VideoTrimmer {
        destinationPath = path
        return this
    }

    fun setVideoURI(videoURI: Uri): VideoTrimmer {
        mSrc = videoURI
        binding.videoLoader.setVideoURI(mSrc)
        binding.videoLoader.requestFocus()
        binding.timeLineView.setVideo(mSrc)
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        val metaDateWidth =  mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toInt() ?: 0
        val metaDataHeight =  mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toInt() ?: 0

        //If the rotation is 90 or 270 the width and height will be transposed.
        when(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()) {
              90,270 -> {
                originalVideoWidth = metaDataHeight
                originalVideoHeight = metaDateWidth
            }
            else -> {
                originalVideoWidth = metaDateWidth
                originalVideoHeight = metaDataHeight
            }
        }

        return this
    }

    private fun setupCropper(width: Int, height: Int) {
        binding.cropFrame.setFixedAspectRatio(false)
        binding.cropFrame.layoutParams = binding.cropFrame.layoutParams?.let {
            it.width = width
            it.height = height
            it
        }
        binding.cropFrame.setImageBitmap(
            context.getDrawable(android.R.color.transparent)
                ?.let { drawableToBitmap(it, width, height) })
    }

    fun setTextTimeSelectionTypeface(tf: Typeface?): VideoTrimmer {
        if (tf != null) binding.textTimeSelection.typeface = tf
        return this
    }

    private class MessageHandler(view: VideoTrimmer) : Handler() {
        private val mView: WeakReference<VideoTrimmer> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.binding.videoLoader == null) return
            view.notifyProgressUpdate(true)
            if (view.binding.videoLoader.isPlaying) sendEmptyMessageDelayed(0, 10)
        }
    }

    companion object {
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }
}
