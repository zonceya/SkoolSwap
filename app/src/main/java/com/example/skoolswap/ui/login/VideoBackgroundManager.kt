package com.example.skoolswap.ui.login

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import timber.log.Timber

class VideoBackgroundManager(
    private val videoView: VideoView,
    private val videoPath: String
) {

    private var isPrepared = false
    private var mediaPlayer: MediaPlayer? = null

    fun setupVideo() {
        try {
            val uri = Uri.parse(videoPath)
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mp ->
                isPrepared = true
                mediaPlayer = mp
                mp.isLooping = true
                videoView.start()
            }

            videoView.setOnErrorListener { _, what, extra ->
                Timber.e("Video error: $what, $extra")
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up video background")
        }
    }

    fun startVideo() {
        if (isPrepared && !videoView.isPlaying) {
            videoView.start()
        }
    }

    fun pauseVideo() {
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    fun resumeVideo() {
        if (isPrepared && !videoView.isPlaying) {
            videoView.start()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }
}