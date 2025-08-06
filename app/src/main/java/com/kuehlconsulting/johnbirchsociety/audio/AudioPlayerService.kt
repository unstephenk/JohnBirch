package com.kuehlconsulting.johnbirchsociety.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.kuehlconsulting.johnbirchsociety.R
import androidx.core.net.toUri

class AudioPlayerService : Service() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentEnclosureUrl: String? = null
    private val NOTIFICATION_CHANNEL_ID = "audio_playback_channel"
    private val NOTIFICATION_ID = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(), PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uriString = intent?.getStringExtra(KEY_URI)
        val enclosureUrl = intent?.getStringExtra(KEY_ENCLOSURE_URL)
        currentEnclosureUrl = enclosureUrl

        if (uriString != null && enclosureUrl != null) {
            val mediaItem = MediaItem.fromUri(uriString.toUri())
            val savedPosition = getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
                .getLong("pos_$enclosureUrl", 0L)

            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                seekTo(savedPosition)
                play()
            }

            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        } else if (intent?.action == ACTION_PAUSE) {
            player?.pause()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        val position = player?.currentPosition ?: 0L
        val prefs = getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
        val episodeKey = "pos_${currentEnclosureUrl}"
        prefs.edit().putLong(episodeKey, position).apply()

        mediaSession?.release()
        player?.release()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText("Playing audio")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val KEY_URI = "KEY_URI"
        const val KEY_ENCLOSURE_URL = "KEY_ENCLOSURE_URL"
        const val ACTION_PAUSE = "com.kuehlconsulting.johnbirchsociety.ACTION_PAUSE"
        const val ACTION_PLAY = "com.kuehlconsulting.johnbirchsociety.ACTION_PLAY"

        fun start(context: Context, uri: String, enclosureUrl: String) {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                putExtra(KEY_URI, uri)
                putExtra(KEY_ENCLOSURE_URL, enclosureUrl)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

}
