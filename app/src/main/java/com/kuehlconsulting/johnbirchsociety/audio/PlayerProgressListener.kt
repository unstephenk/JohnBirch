package com.kuehlconsulting.johnbirchsociety.audio

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import com.kuehlconsulting.johnbirchsociety.data.db.DownloadDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayerProgressListener(
    private val enclosureUrl: String,
    private val downloadDao: DownloadDao
) : Player.Listener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isTracking = false

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying && !isTracking) {
            startTracking()
        } else if (!isPlaying && isTracking) {
            stopTracking()
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            scope.launch {
                val position = currentPlayer?.currentPosition ?: return@launch
                downloadDao.updateLastPlayedAt(enclosureUrl, position)
            }
            mainHandler.postDelayed(this, 5000)
        }
    }

    private val currentPlayer: Player?
        get() = PlayerHolder.player

    private fun startTracking() {
        isTracking = true
        mainHandler.post(updateRunnable)
    }

    private fun stopTracking() {
        isTracking = false
        mainHandler.removeCallbacks(updateRunnable)
    }
}
