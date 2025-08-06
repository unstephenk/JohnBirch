package com.kuehlconsulting.johnbirchsociety.audio

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kuehlconsulting.johnbirchsociety.R
import androidx.core.content.edit

class AudioPlayerService : Service() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentEnclosureUrl: String? = null

    companion object {
        const val KEY_URI = "KEY_URI"
        const val KEY_ENCLOSURE_URL = "KEY_ENCLOSURE_URL"
        const val ACTION_PAUSE = "com.kuehlconsulting.johnbirchsociety.ACTION_PAUSE"
        const val ACTION_PLAY = "com.kuehlconsulting.johnbirchsociety.ACTION_PLAY"
        const val ACTION_REWIND = "com.kuehlconsulting.johnbirchsociety.ACTION_REWIND"
        const val ACTION_FORWARD = "com.kuehlconsulting.johnbirchsociety.ACTION_FORWARD"
        const val NOTIFICATION_CHANNEL_ID = "audio_playback_channel"
        const val NOTIFICATION_ID = 1

        fun start(context: Context, uri: String, enclosureUrl: String) {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                putExtra(KEY_URI, uri)
                putExtra(KEY_ENCLOSURE_URL, enclosureUrl)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(
                        this,
                        Class.forName("com.kuehlconsulting.johnbirchsociety.MainActivity")
                    ),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uriString = intent?.getStringExtra(KEY_URI)
        val enclosureUrl = intent?.getStringExtra(KEY_ENCLOSURE_URL)
        currentEnclosureUrl = enclosureUrl

        // Handle user actions
        when (intent?.action) {
            ACTION_PAUSE -> player?.pause()
            ACTION_PLAY -> player?.play()
            ACTION_REWIND -> player?.seekBack()
            ACTION_FORWARD -> player?.seekForward()
        }


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

            val notification = buildMediaStyleNotification()
            startForeground(NOTIFICATION_ID, notification)
        } else if (intent?.action == ACTION_PAUSE) {
            player?.pause()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        val position = player?.currentPosition ?: 0L
        currentEnclosureUrl?.let {
            getSharedPreferences("player_prefs", MODE_PRIVATE)
                .edit {
                    putLong("pos_$it", position)
                }
        }
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildMediaStyleNotification(): Notification {
        val session = mediaSession ?: throw IllegalStateException("MediaSession not initialized")

        val rewindIntent = PendingIntent.getService(
            this, 0, Intent(this, AudioPlayerService::class.java).apply { action = ACTION_REWIND },
            PendingIntent.FLAG_IMMUTABLE
        )

        val forwardIntent = PendingIntent.getService(
            this, 1, Intent(this, AudioPlayerService::class.java).apply { action = ACTION_FORWARD },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText("Playing audio")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_previous, "Rewind", rewindIntent)
            .addAction(android.R.drawable.ic_media_next, "Forward", forwardIntent)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1) // Show both actions in compact view
            )
            .setOngoing(true)
            .build()
    }



}