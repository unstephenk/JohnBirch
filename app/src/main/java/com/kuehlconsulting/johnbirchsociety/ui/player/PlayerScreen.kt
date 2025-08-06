package com.kuehlconsulting.johnbirchsociety.ui.player

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kuehlconsulting.johnbirchsociety.audio.AudioPlayerService
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val context = LocalContext.current

                Text("Playing from service...", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    val pauseIntent = Intent(context, AudioPlayerService::class.java).apply {
                        action = "com.kuehlconsulting.johnbirchsociety.ACTION_PAUSE"
                    }
                    ContextCompat.startForegroundService(context, pauseIntent)

                }) {
                    Text("Pause")
                }
            }
        }
    }
}