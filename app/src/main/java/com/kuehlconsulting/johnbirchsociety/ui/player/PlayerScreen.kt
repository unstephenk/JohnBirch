// app/src/main/java/com/kuehlconsulting/johnbirchsociety/ui/player/PlayerScreen.kt
package com.kuehlconsulting.johnbirchsociety.ui.player

import android.content.ComponentName
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerControlView
import com.kuehlconsulting.johnbirchsociety.audio.AudioPlayerService
import java.io.File
import java.util.concurrent.Executor
import androidx.core.net.toUri

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    ref: String,                 // local file path or a content:// / file:// / http(s):// string
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val token = remember { SessionToken(context, ComponentName(context, AudioPlayerService::class.java)) }

    // Hold both the controller and the future reference to release/cancel safely.
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var future by remember { mutableStateOf<com.google.common.util.concurrent.ListenableFuture<MediaController>?>(null) }

    // Build controller once per token/ref combo
    LaunchedEffect(token, ref) {
        // Build async; we won't use Guava extensions.
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            {
                try {
                    val c = f.get() // get() runs on mainExecutor because we scheduled on it
                    controller = c

                    val uri = toPlayableUri(ref)
                    val item = MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(uri.lastPathSegment ?: "Episode")
                                .build()
                        ).build()

                    c.setMediaItem(item)
                    c.prepare()
                    c.play()
                } catch (_: Exception) {
                    // Swallow for now; you can log if you like
                }
            },
            mainExecutor
        )
    }

    // Ensure we release even without awaitDispose
    DisposableEffect(token) {
        onDispose {
            try { controller?.release() } catch (_: Exception) {}
            try { future?.let { MediaController.releaseFuture(it) } } catch (_: Exception) {}
            controller = null
            future = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerControlView(ctx).apply {
                player = controller
                setOnFullScreenModeChangedListener { isFs ->
                    if (!isFs) onClose()
                }
            }
        },
        update = { view -> view.player = controller }
    )
}

private fun toPlayableUri(ref: String): Uri {
    return when {
        ref.startsWith("content://") || ref.startsWith("file://") || ref.startsWith("http://") || ref.startsWith("https://") ->
            ref.toUri()
        else -> Uri.fromFile(File(ref))
    }
}
