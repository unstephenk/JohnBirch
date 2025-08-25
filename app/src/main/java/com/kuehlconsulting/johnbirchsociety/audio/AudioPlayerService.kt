package com.kuehlconsulting.johnbirchsociety.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import android.os.Handler
import android.os.Looper

@UnstableApi
class AudioPlayerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "johnbirch_audio"
        const val KEY_URI = "KEY_URI"
        const val KEY_ENCLOSURE_URL = "KEY_ENCLOSURE_URL"
        const val ACTION_PAUSE = "com.kuehlconsulting.johnbirchsociety.ACTION_PAUSE"
        const val ACTION_PLAY = "com.kuehlconsulting.johnbirchsociety.ACTION_PLAY"
        const val ACTION_PROGRESS_UPDATE = "com.kuehlconsulting.johnbirchsociety.ACTION_PROGRESS_UPDATE"
        const val EXTRA_CURRENT_POSITION = "EXTRA_CURRENT_POSITION"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val EXTRA_IS_PLAYING = "EXTRA_IS_PLAYING"
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var isTrackingProgress = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            player?.let { exoPlayer ->
                if (exoPlayer.isPlaying) {
                    val currentPosition = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    
                    val intent = Intent(ACTION_PROGRESS_UPDATE).apply {
                        putExtra(EXTRA_CURRENT_POSITION, currentPosition)
                        putExtra(EXTRA_DURATION, duration)
                        putExtra(EXTRA_IS_PLAYING, exoPlayer.isPlaying)
                    }
                    sendBroadcast(intent)
                }
            }
            progressHandler.postDelayed(this, 1000) // Update every second
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for Android 8+
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "John Birch Audio",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying && !isTrackingProgress) {
                        startProgressTracking()
                    } else if (!isPlaying && isTrackingProgress) {
                        stopProgressTracking()
                    }
                }
            })
        }
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
        when (intent?.action) {
            ACTION_PAUSE -> {
                player?.pause()
            }
            ACTION_PLAY -> {
                player?.play()
            }
            else -> {
                val uriString = intent?.getStringExtra(KEY_URI)

                if (!uriString.isNullOrEmpty()) {
                    val mediaItem = MediaItem.fromUri(uriString)
                    player?.setMediaItem(mediaItem)
                    player?.prepare()
                    player?.play()
                }
            }
        }

        notificationManager?.setPlayer(player)

        // Start foreground with a stub notification until media takes over
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Starting playback")
            .setContentText("Buffering...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun startProgressTracking() {
        if (!isTrackingProgress) {
            isTrackingProgress = true
            progressHandler.post(progressRunnable)
        }
    }

    private fun stopProgressTracking() {
        isTrackingProgress = false
        progressHandler.removeCallbacks(progressRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressTracking()
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