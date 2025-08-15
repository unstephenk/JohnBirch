package com.kuehlconsulting.johnbirchsociety.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kuehlconsulting.johnbirchsociety.audio.AudioPlayerService
import java.io.File
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
    val uri = if (ref.startsWith("content://")) ref.toUri()
    else Uri.fromFile(File(ref))

    // Start the audio service (this kicks off the MediaSession)
    LaunchedEffect(uri) {
        val intent = android.content.Intent(context, AudioPlayerService::class.java).apply {
            putExtra(AudioPlayerService.KEY_URI, uri.toString())
            putExtra(AudioPlayerService.KEY_ENCLOSURE_URL, ref)
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
            Text("Playing via system media controls", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
