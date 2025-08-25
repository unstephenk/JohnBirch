package com.kuehlconsulting.johnbirchsociety.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kuehlconsulting.johnbirchsociety.audio.AudioPlayerService
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign

import java.io.File
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    ref: String,                // may be "content://..." or a file path
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(ref) {
        if (ref.startsWith("content://")) ref.toUri()
        else Uri.fromFile(File(ref))
    }

    // State for progress tracking
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasStartedPlaying by remember { mutableStateOf(false) }

    // Broadcast receiver for progress updates
    val progressReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioPlayerService.ACTION_PROGRESS_UPDATE) {
                    currentPosition = intent.getLongExtra(AudioPlayerService.EXTRA_CURRENT_POSITION, 0L)
                    duration = intent.getLongExtra(AudioPlayerService.EXTRA_DURATION, 0L)
                    isPlaying = intent.getBooleanExtra(AudioPlayerService.EXTRA_IS_PLAYING, false)
                    if (isPlaying && !hasStartedPlaying) {
                        hasStartedPlaying = true
                    }
                }
            }
        }
    }

    // Register broadcast receiver
    LaunchedEffect(Unit) {
        val filter = IntentFilter(AudioPlayerService.ACTION_PROGRESS_UPDATE)
        ContextCompat.registerReceiver(
            context,
            progressReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                context.unregisterReceiver(progressReceiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
        }
    }

    LaunchedEffect(uri) {
        val intent = Intent(context, AudioPlayerService::class.java).apply {
            putExtra(AudioPlayerService.KEY_URI, uri.toString())
            putExtra(AudioPlayerService.KEY_ENCLOSURE_URL, ref) // ref should be the enclosure URL
        }
        ContextCompat.startForegroundService(context, intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Playing from service...", 
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress bar - only show after playback has started
                if (hasStartedPlaying && duration > 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Play/Pause button
                Button(
                    onClick = {
                        val intent = Intent(context, AudioPlayerService::class.java).apply {
                            action = if (isPlaying) AudioPlayerService.ACTION_PAUSE else AudioPlayerService.ACTION_PLAY
                        }
                        ContextCompat.startForegroundService(context, intent)
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}