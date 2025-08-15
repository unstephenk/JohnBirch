package com.kuehlconsulting.johnbirchsociety.audio

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AudioPlayerService : MediaSessionService() {

    companion object {
        const val KEY_URI = "KEY_URI"
        const val KEY_ENCLOSURE_URL = "KEY_ENCLOSURE_URL"
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(15000)
            .setSeekForwardIncrementMs(15000)
            .build()

        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val uri = intent?.getStringExtra(KEY_URI)
        uri?.let {
            val mediaItem = MediaItem.fromUri(it)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }

        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        player?.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
