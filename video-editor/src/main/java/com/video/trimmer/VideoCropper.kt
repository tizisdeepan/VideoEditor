package com.video.trimmer

import android.content.Context
import android.graphics.Typeface
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
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.VideoView
import com.video.trimmer.interfaces.OnProgressVideoListener
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.utils.*
import com.video.trimmer.view.Thumb
import kotlinx.android.synthetic.main.view_cropper.view.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class VideoCropper @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var mSrc: Uri
    private var mFinalPath: String? = null
    private var mListeners: ArrayList<OnProgressVideoListener> = ArrayList()

    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnVideoListener: OnVideoListener? = null

    private var mDuration = 0
    private var mTimeVideo = 0
    private var mStartPosition = 0

    private var mEndPosition = 0
    private var mResetSeekBar = true
    private val mMessageHandler = MessageHandler(this)

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
        setUpMargins()
    }

    private fun setUpListeners() {
        mListeners = ArrayList()
        mListeners.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                updateVideoProgress(time)
            }
        })
        mListeners.add(timeVideoView)

        val gestureDetector = GestureDetector(context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        onClickVideoPlayPause()
                        return true
                    }
                }
        )

        video_loader.setOnErrorListener { _, what, _ ->
            mOnTrimVideoListener?.onError("Something went wrong reason : $what")
            false
        }

        video_loader.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

        video_loader.setOnPreparedListener { mp -> onVideoPrepared(mp) }
        video_loader.setOnCompletionListener { onVideoCompleted() }
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        var duration = (mDuration * progress / 1000L).toInt()
        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition)
                duration = mStartPosition
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition)
                duration = mEndPosition
            }
            setTimeVideo(duration)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        video_loader.pause()
        icon_video_play.visibility = View.VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        video_loader.pause()
        icon_video_play.visibility = View.VISIBLE

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        video_loader.seekTo(duration)
        setTimeVideo(duration)
        notifyProgressUpdate(false)
    }

    private fun setProgressBarPosition(position: Int) {
        if (mDuration > 0) handlerTop.progress = (1000L * position / mDuration).toInt()
    }

    private fun setUpMargins() {
        val lp = timeLineView.layoutParams as RelativeLayout.LayoutParams
        lp.setMargins(0, 0, 0, 0)
        timeLineView.layoutParams = lp
    }

    fun onSaveClicked() {
        mOnTrimVideoListener?.onTrimStarted()
        icon_video_play.visibility = View.VISIBLE
        video_loader.pause()

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        val METADATA_KEY_DURATION = java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))

        val file = File(mSrc.path)

        if (mTimeVideo < MIN_TIME_FRAME) {
            if (METADATA_KEY_DURATION - mEndPosition > MIN_TIME_FRAME - mTimeVideo) mEndPosition += MIN_TIME_FRAME - mTimeVideo
            else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) mStartPosition -= MIN_TIME_FRAME - mTimeVideo
        }

        val root = File(destinationPath)
        root.mkdirs()
        val outputFileUri = Uri.fromFile(File(root, "t_${Calendar.getInstance().timeInMillis}_" + file.nameWithoutExtension + ".mp4"))
        val outPutPath = RealPathUtil.realPathFromUriApi19(context, outputFileUri)
                ?: File(root, "t_${Calendar.getInstance().timeInMillis}_" + mSrc.path.substring(mSrc.path.lastIndexOf("/") + 1)).absolutePath
        Log.e("SOURCE", file.path)
        Log.e("DESTINATION", outPutPath)
        VideoOptions(context).trimVideo(TrimVideoUtils.stringForTime(mStartPosition), TrimVideoUtils.stringForTime(mEndPosition), file.path, outPutPath, outputFileUri, mOnTrimVideoListener)
    }

    private fun onClickVideoPlayPause() {
        if (video_loader.isPlaying) {
            icon_video_play.visibility = View.VISIBLE
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            video_loader.pause()
        } else {
            icon_video_play.visibility = View.GONE
            if (mResetSeekBar) {
                mResetSeekBar = false
                video_loader.seekTo(mStartPosition)
            }
            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
            video_loader.start()
        }
    }

    fun onCancelClicked() {
        video_loader.stopPlayback()
        mOnTrimVideoListener?.cancelAction()
    }

    private fun onVideoPrepared(mp: MediaPlayer) {
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = layout_surface_view.width
        val screenHeight = layout_surface_view.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = video_loader.layoutParams

        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        video_loader.layoutParams = lp

        icon_video_play.visibility = View.VISIBLE

        mDuration = video_loader.duration
        setSeekBarPosition()

        setTimeFrames()
        setTimeVideo(0)

        mOnVideoListener?.onVideoPrepared()
    }

    private fun setSeekBarPosition() {
        video_loader.seekTo(mStartPosition)
        mTimeVideo = mDuration
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        textTimeSelection.text = String.format("%s %s - %s %s", TrimVideoUtils.stringForTime(mStartPosition), seconds, TrimVideoUtils.stringForTime(mEndPosition), seconds)
    }

    private fun setTimeVideo(position: Int) {
        val seconds = context.getString(R.string.short_seconds)
        textTime.text = String.format("%s %s", TrimVideoUtils.stringForTime(position), seconds)
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L).toInt()
                video_loader.seekTo(mStartPosition)
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L).toInt()
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        video_loader.pause()
        icon_video_play.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        video_loader.seekTo(mStartPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0) return

        val position = video_loader.currentPosition
        if (all) {
            for (item in mListeners) {
                item.updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
            }
        } else {
            mListeners[0].updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
        }
    }

    private fun updateVideoProgress(time: Int) {
        if (video_loader == null) return
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            video_loader.pause()
            icon_video_play.visibility = View.VISIBLE
            mResetSeekBar = true
            return
        }
        setProgressBarPosition(time)
        setTimeVideo(time)
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    fun setVideoInformationVisibility(visible: Boolean): VideoCropper {
        timeFrame.visibility = if (visible) View.VISIBLE else View.GONE
        return this
    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener): VideoCropper {
        mOnTrimVideoListener = onTrimVideoListener
        return this
    }

    /**
     * Listener for some [VideoView] events
     *
     * @param onVideoListener interface for events
     */
    fun setOnVideoListener(onVideoListener: OnVideoListener): VideoCropper {
        mOnVideoListener = onVideoListener
        return this
    }

    /**
     * Cancel all current operations
     */
    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    fun setDestinationPath(path: String): VideoCropper {
        destinationPath = path
        return this
    }

    fun setVideoURI(videoURI: Uri): VideoCropper {
        mSrc = videoURI
        video_loader.setVideoURI(mSrc)
        video_loader.requestFocus()
        timeLineView.setVideo(mSrc)
        return this
    }

    fun setTextTimeSelectionTypeface(tf: Typeface?): VideoCropper {
        if (tf != null) textTimeSelection.typeface = tf
        return this
    }

    fun setTextTimeTypeface(tf: Typeface?): VideoCropper {
        if (tf != null) textTime.typeface = tf
        return this
    }

    private class MessageHandler internal constructor(view: VideoCropper) : Handler() {
        private val mView: WeakReference<VideoCropper> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.video_loader == null) return
            view.notifyProgressUpdate(true)
            if (view.video_loader.isPlaying) sendEmptyMessageDelayed(0, 10)
        }
    }

    companion object {
        private val TAG = VideoTrimmer::class.java.simpleName
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }
}