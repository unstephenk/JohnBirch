package com.kuehlconsulting.johnbirchsociety.audio

import android.app.Service import android.content.Intent import android.os.IBinder import android.app.NotificationManager import android.app.PendingIntent import android.content.Context import android.graphics.Bitmap import androidx.core.app.NotificationCompat import androidx.media3.common.MediaItem import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer import androidx.media3.session.MediaSession import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class AudioPlayerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "johnbirch_audio"
        const val KEY_URI = "KEY_URI"
        const val KEY_ENCLOSURE_URL = "KEY_ENCLOSURE_URL"
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(DescriptionAdapter())
            .setChannelImportance(NotificationManager.IMPORTANCE_LOW)
            .build()
            .apply {
                setUseFastForwardAction(true)
                setUseRewindAction(true)
                setUseNextAction(false)
                setUsePreviousAction(false)
                setUsePlayPauseActions(true)
                setPlayer(player)
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uriString = intent?.getStringExtra(KEY_URI)

        if (!uriString.isNullOrEmpty()) {
            val mediaItem = MediaItem.fromUri(uriString)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.setPlayer(null)
        mediaSession?.release()
        player?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return "Now Playing"
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? = null

        override fun getCurrentContentText(player: Player): CharSequence? {
            return "Streaming audio"
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? = null
    }

}


