package com.kuehlconsulting.johnbirchsociety.audio

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

        mediaSession = MediaSession.Builder(this, player!!).build()

    }

    override fun onDestroy() {
        player?.release()
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Here you can implement logic to decide whether to accept or reject controller connections.
        // For simplicity, let's accept all connections.
        return mediaSession
    }

}