package com.kuehlconsulting.johnbirchsociety.ui.player

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.kuehlconsulting.johnbirchsociety.audio.AudioPlayerService
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    ref: String,                // may be "content://..." or a file path
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uri = remember(ref) {
        if (ref.startsWith("content://")) Uri.parse(ref)
        else Uri.fromFile(File(ref))
    }

    LaunchedEffect(uri) {
        val intent = Intent(context, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
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
            AndroidView(
                factory = { PlayerView(it).apply { this.player = player } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}