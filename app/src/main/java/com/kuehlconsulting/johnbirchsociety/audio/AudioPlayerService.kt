package com.kuehlconsulting.johnbirchsociety.audio

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.IBinder
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

class AudioPlayerService : MediaSessionService() {
    companion object {
        const val ACTION_PLAY = "com.kuehlconsulting.johnbirchsociety.PLAY"
        const val KEY_URI = "uri"
        const val KEY_ENCLOSURE_URL = "enclosureUrl"
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

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
        if (intent?.action == ACTION_PLAY) {
            val enclosureUrl = intent.getStringExtra(KEY_ENCLOSURE_URL)
            val uriString = intent.getStringExtra(KEY_URI)

            if (!uriString.isNullOrBlank() && !enclosureUrl.isNullOrBlank()) {
                playMedia(enclosureUrl, uriString.toUri())
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

}


