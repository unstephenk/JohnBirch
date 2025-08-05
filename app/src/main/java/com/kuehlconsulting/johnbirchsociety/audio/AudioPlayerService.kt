package com.kuehlconsulting.johnbirchsociety.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kuehlconsulting.johnbirchsociety.MainActivity
import com.kuehlconsulting.johnbirchsociety.R
import com.kuehlconsulting.johnbirchsociety.data.db.AppDatabase
import com.kuehlconsulting.johnbirchsociety.audio.PlayerHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class AudioPlayerService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
        }
        player?.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    companion object {
        private const val ACTION_PLAY = "com.kuehlconsulting.johnbirchsociety.PLAY_AUDIO"
        private const val KEY_ENCLOSURE_URL = "enclosure_url"
        private const val KEY_URI = "local_uri"

        fun startWith(context: Context, enclosureUrl: String, contentUri: Uri) {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(KEY_ENCLOSURE_URL, enclosureUrl)
                putExtra(KEY_URI, contentUri.toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY) {
            val enclosureUrl = intent.getStringExtra(KEY_ENCLOSURE_URL) ?: return super.onStartCommand(intent, flags, startId)
            val uriString = intent.getStringExtra(KEY_URI) ?: return super.onStartCommand(intent, flags, startId)
            val contentUri = uriString.toUri()
            playMedia(enclosureUrl, contentUri)
        }
        return Service.START_STICKY
    }

    private fun playMedia(enclosureUrl: String, contentUri: Uri) {
        serviceScope.launch {
            val dao = AppDatabase.get(applicationContext).downloadDao()
            val download = dao.get(enclosureUrl) ?: return@launch

            val mediaItem = MediaItem.Builder()
                .setUri(contentUri)
                .setMediaId(enclosureUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("John Birch Episode")
                        .build()
                )
                .build()

            player?.setMediaItem(mediaItem)
            player?.prepare()

            PlayerHolder.player = player!!

            val resumePosition = download.lastPlayedAt ?: 0L
            player?.seekTo(resumePosition)
            player?.play()

            // Observe position periodically and save to DB
            player?.addListener(PlayerProgressListener(enclosureUrl, dao))

            startForeground(
                1,
                NotificationCompat.Builder(this@AudioPlayerService, "media_playback")
                    .setContentTitle("John Birch Audio")
                    .setContentText("Playing episode")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            this@AudioPlayerService,
                            0,
                            Intent(this@AudioPlayerService, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .setOngoing(true)
                    .build()
            )
        }
    }

}

