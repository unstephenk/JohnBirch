package com.kuehlconsulting.johnbirchsociety.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kuehlconsulting.johnbirchsociety.MainActivity
import com.kuehlconsulting.johnbirchsociety.data.db.getDownloadDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaStyleNotificationHelper
import com.kuehlconsulting.johnbirchsociety.R

class AudioPlayerService : MediaSessionService() {
    companion object {
        const val ACTION_PLAY = "com.kuehlconsulting.johnbirchsociety.PLAY"
        const val KEY_URI = "uri"
        const val KEY_ENCLOSURE_URL = "enclosureUrl"
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "media_playback_channel"

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        player = ExoPlayer.Builder(this).build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        player?.release()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (intent?.action == ACTION_PLAY) {
            val uriString = intent.getStringExtra(KEY_URI)
            val enclosureUrl = intent.getStringExtra(KEY_ENCLOSURE_URL)

            if (!uriString.isNullOrBlank() && !enclosureUrl.isNullOrBlank()) {
                val uri = Uri.parse(uriString)
                playMedia(enclosureUrl, uri)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }




    private fun playMedia(enclosureUrl: String, contentUri: Uri) {
        val dao = getDownloadDao(this)

        CoroutineScope(Dispatchers.IO).launch {
            val download = dao.getByEnclosureUrl(enclosureUrl) ?: return@launch

            val mediaItem = MediaItem.fromUri(contentUri)
            player?.setMediaItem(mediaItem)
            player?.prepare()

            PlayerHolder.player = player!!
            player?.seekTo(download.lastPlayedAt ?: 0L)
            player?.play()

            player?.addListener(PlayerProgressListener(enclosureUrl, dao))
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(): Notification {
        val session = mediaSession ?: throw IllegalStateException("MediaSession not initialized")

        val style = MediaStyleNotificationHelper.MediaStyle(session)

        return NotificationCompat.Builder(this, "media_playback_channel") // you can change this channel ID
            .setContentTitle("Now Playing")
            .setContentText("Audio is playing")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // make sure this icon exists
            .setStyle(style)
            .setOngoing(true)
            .setChannelId(CHANNEL_ID)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "media_playback_channel", // must match .setChannelId
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls media playback"
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }


}


