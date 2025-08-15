package com.kuehlconsulting.johnbirchsociety.audio

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AudioPlayerService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Single app-wide player living in the service
        val p = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH) // podcasts
                    .build(),
                /* handleAudioFocus = */ true
            )
            setHandleAudioBecomingNoisy(true) // pause if headphones unplugged
        }
        player = p

        // MediaSession wires up BT/lockscreen/car/system controls + transport
        session = MediaSession.Builder(this, p).build()

        // This enables the system’s media notification (slide drawer/lock screen)
        // and promotes the service to foreground correctly.
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Optional convenience: start playback if a URL was included
        val url = intent?.getStringExtra("url")
        if (!url.isNullOrBlank()) {
            player?.apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                play()
            }
        }
        // If the system recreates the service, we want it to keep running.
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }
}
