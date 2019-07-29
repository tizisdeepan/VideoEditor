package com.video.trimmer

import android.content.Context
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
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.video.trimmer.interfaces.OnProgressVideoListener
import com.video.trimmer.interfaces.OnRangeSeekBarListener
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.utils.BackgroundExecutor
import com.video.trimmer.utils.RealPathUtil
import com.video.trimmer.utils.TrimVideoUtils
import com.video.trimmer.utils.UiThreadExecutor
import com.video.trimmer.view.RangeSeekBarView
import com.video.trimmer.view.Thumb
import kotlinx.android.synthetic.main.view_time_line.view.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*


class VideoTrimmer @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    lateinit var mSrc: Uri
    private var mFinalPath: String? = null

    private var mMaxDuration: Int = 0
    private var mListeners: MutableList<OnProgressVideoListener>? = null

    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnVideoListener: OnVideoListener? = null

    private var mDuration = 0
    private var mTimeVideo = 0
    private var mStartPosition = 0
    private var mEndPosition = 0

    private var mOriginSizeFile: Long = 0
    private var mResetSeekBar = true
    private val mMessageHandler = MessageHandler(this)

    var destinationPath: String
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
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true)
        setUpListeners()
        setUpMargins()
    }

    private fun setUpListeners() {
        mListeners = ArrayList()
        mListeners?.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                updateVideoProgress(time)
            }
        })

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

        timeLineBar.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })

        video_loader.setOnPreparedListener { mp -> onVideoPrepared(mp) }

        video_loader.setOnCompletionListener { onVideoCompleted() }
    }

    private fun setUpMargins() {
        val marge = timeLineBar.thumbs[0].widthBitmap
        val lp = timeLineView.layoutParams as RelativeLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        timeLineView.layoutParams = lp
    }

    fun onSaveClicked() {
        if (mStartPosition <= 0 && mEndPosition >= mDuration) {
            mOnTrimVideoListener?.getResult(mSrc)
        } else {
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
            val fname = "t_" + mSrc.path.substring(mSrc.path.lastIndexOf("/") + 1)
            val sdImageMainDirectory = File(root, fname)
            val outputFileUri = Uri.fromFile(sdImageMainDirectory)

            val outPutPath = RealPathUtil.getRealPathFromURI_API19(context, outputFileUri)

            Log.e("SOURCE", file.path)
            Log.e("DESTINATION", outPutPath)

            val ffmpeg = FFmpeg.getInstance(context)
            ffmpeg.loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFinish() {
                    Log.e("FFmpegLoad", "onFinish")
                }

                override fun onSuccess() {
                    Log.e("FFmpegLoad", "onSuccess")
                    val command = arrayOf("-y", "-i", file.path, "-ss", TrimVideoUtils.stringForTime(mStartPosition), "-to", TrimVideoUtils.stringForTime(mEndPosition), "-c", "copy", outPutPath)//{"-y", "-ss", "00:00:03", "-i", file.getPath(), "-t", "00:00:08", "-async", "1", outPutPath};  //-i movie.mp4 -ss 00:00:03 -t 00:00:08 -async 1 cut.mp4
                    try {
                        ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
                            override fun onSuccess(message: String?) {
                                super.onSuccess(message)
                                Log.d(TAG, "onSuccess: " + message!!)
                            }

                            override fun onProgress(message: String?) {
                                super.onProgress(message)
                                mOnTrimVideoListener?.onError(message.toString())
                                Log.d(TAG, "onProgress: " + message!!)
                            }

                            override fun onFailure(message: String?) {
                                super.onFailure(message)
                                mOnTrimVideoListener?.onError(message.toString())
                                Log.d(TAG, "onFailure: " + message!!)
                            }

                            override fun onStart() {
                                super.onStart()
                                Log.d(TAG, "onStart: ")
                            }

                            override fun onFinish() {
                                super.onFinish()
                                mOnTrimVideoListener?.getResult(outputFileUri)
                                Log.d(TAG, "onFinish: ")
                            }
                        })
                    } catch (e: FFmpegCommandAlreadyRunningException) {
                        mOnTrimVideoListener?.onError(e.toString())
                    }
                }

                override fun onFailure() {
                    Log.e("FFmpegLoad", "onFailure")
                    mOnTrimVideoListener?.onError("Failed")
                }

                override fun onStart() {
                }
            })


            //notify that video trimming started
            mOnTrimVideoListener?.onTrimStarted()

//            BackgroundExecutor.execute(
//                    object : BackgroundExecutor.Task("", 0L, "") {
//                        override fun execute() {
//                            try {
//                                TrimVideoUtils.startTrim(file, destinationPath, mStartPosition.toLong(), mEndPosition.toLong(), mOnTrimVideoListener!!)
//                            } catch (e: Throwable) {
//                                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
//                            }
//                        }
//                    }
//            )
        }
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
        // Adjust the size of the video
        // so it fits on the screen
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
        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2
            mEndPosition = mDuration / 2 + mMaxDuration / 2

            timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration).toFloat())
            timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration).toFloat())

        } else {
            mStartPosition = 0
            mEndPosition = mDuration
        }

        video_loader.seekTo(mStartPosition)

        mTimeVideo = mDuration
        timeLineBar.initMaxWidth()
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
            for (item in mListeners!!) {
                item.updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
            }
        } else {
            mListeners!![1].updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
        }
    }

    private fun updateVideoProgress(time: Int) {
        if (video_loader == null) {
            return
        }

        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            video_loader.pause()
            icon_video_play.visibility = View.VISIBLE
            mResetSeekBar = true
            return
        }
        setTimeVideo(time)
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    fun setVideoInformationVisibility(visible: Boolean) {
        timeText.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener
    }

    /**
     * Listener for some [VideoView] events
     *
     * @param onVideoListener interface for events
     */
    fun setOnVideoListener(onVideoListener: OnVideoListener) {
        mOnVideoListener = onVideoListener
    }

    /**
     * Cancel all current operations
     */
    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than maxDuration
     *
     * @param maxDuration the maximum duration of the trimmed video in seconds
     */
    fun setMaxDuration(maxDuration: Int) {
        mMaxDuration = maxDuration * 1000
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    fun setVideoURI(videoURI: Uri) {
        mSrc = videoURI
        video_loader.setVideoURI(mSrc)
        video_loader.requestFocus()
        timeLineView.setVideo(mSrc)
    }

    private class MessageHandler internal constructor(view: VideoTrimmer) : Handler() {

        private val mView: WeakReference<VideoTrimmer> = WeakReference(view)

        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.video_loader == null) return

            view.notifyProgressUpdate(true)
            if (view.video_loader.isPlaying) sendEmptyMessageDelayed(0, 10)
        }
    }

    companion object {
        private val TAG = VideoTrimmer::class.java.simpleName
        private val MIN_TIME_FRAME = 1000
        private val SHOW_PROGRESS = 2
    }
}
