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
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
            .setCallback(object : MediaSession.Callback {
                fun onPlay(session: MediaSession, controller: MediaSession.ControllerInfo) {
                    player?.play()
                }

                fun onPause(session: MediaSession, controller: MediaSession.ControllerInfo) {
                    player?.pause()
                }

                fun onStop(session: MediaSession, controller: MediaSession.ControllerInfo) {
                    player?.stop()
                }

                @androidx.media3.common.util.UnstableApi
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    player?.play()

                    val mediaItemsWithStartPosition = MediaSession.MediaItemsWithStartPosition(
                        player?.currentMediaItem?.let { listOf(it) } ?: emptyList(),
                        player?.currentMediaItemIndex ?: 0,
                        player?.currentPosition ?: 0L
                    )

                    return Futures.immediateFuture(mediaItemsWithStartPosition)
                }

            })
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
        super.onStartCommand(intent, flags, startId)
        val uriString = intent?.getStringExtra(KEY_URI)
        val enclosureUrl = intent?.getStringExtra(KEY_ENCLOSURE_URL)

        if (uriString != null) {
            val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                play() // ✅ without this, audio won't start
            }

            // Build notification AFTER player is set up
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
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

        return NotificationCompat.Builder(this, CHANNEL_ID) // you can change this channel ID
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
            CHANNEL_ID, // must match .setChannelId
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls media playback"
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }


}


