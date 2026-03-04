// ui/login/VideoPlayerView.kt
package com.example.skoolswap.ui.login

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.skoolswap.R

class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var isPrepared = false
    private var videoUri: Uri? = null

    init {
        // Inflate the player view
        val view = inflate(context, R.layout.view_video_player, this)
        playerView = view.findViewById(R.id.player_view)
    }

    fun setVideoUri(uri: Uri) {
        this.videoUri = uri
        if (isPrepared) {
            initializePlayer(uri)
        }
    }

    fun onResume() {
        if (isPrepared && videoUri != null) {
            initializePlayer(videoUri!!)
        }
    }

    fun onPause() {
        releasePlayer()
    }

    fun prepare() {
        isPrepared = true
        videoUri?.let { initializePlayer(it) }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(uri: Uri) {
        try {
            releasePlayer()

            // Create a data source factory for reading data
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)

            // Create a source for the local video
            val mediaItem = MediaItem.fromUri(uri)
            val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            player = ExoPlayer.Builder(context).build().apply {
                setMediaSource(source)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL // Loop the video
                volume = 0f // Mute the video

                // Add listener for debugging/state changes
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        // Log error if needed
                        error.printStackTrace()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        when (playbackState) {
                            Player.STATE_READY -> {
                                // Video is ready to play
                                playerView.visibility = View.VISIBLE
                            }
                            Player.STATE_BUFFERING -> {
                                // Buffering
                            }
                        }
                    }
                })
            }

            playerView.player = player
            playerView.useController = false // Hide controls

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releasePlayer() {
        player?.run {
            playWhenReady = false
            stop()
            release()
        }
        player = null
        playerView.player = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releasePlayer()
    }
}