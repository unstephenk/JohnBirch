// app/src/main/java/.../ui/player/PlayerScreen.kt
package com.kuehlconsulting.johnbirchsociety.ui.player

import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerControlView
import com.kuehlconsulting.johnbirchsociety.audio.AudioPlayerService
import kotlinx.coroutines.guava.await
import java.io.File

// --------------- Public API you can call from MainActivity ---------------

@Composable
fun PlayerScreen(
    ref: YourRefType,              // <-- keep your existing call site
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaUri = remember(ref) { resolveMediaUri(ref) }
    val metadata  = remember(ref) { resolveMetadata(ref) }
    PlayerScreenInternal(mediaUri = mediaUri, metadata = metadata, onClose = onClose, modifier = modifier)
}

// --------------- Adapter helpers (adjust to your model) ---------------

/**
 * Map your 'ref' to a playable local Uri.
 * Examples shown for either a file path or a content Uri on your model.
 */
private fun resolveMediaUri(ref: YourRefType): Uri {
    // Example possibilities — replace with your actual fields:
    // return ref.contentUri                       // if already a Uri
    // return Uri.parse(ref.localUriString)        // if stored as string
    // return Uri.fromFile(File(ref.localPath))    // if stored as file path

    // TEMP default (replace this with your real mapping):
    val path = ref.localPath                       // <-- e.g., "/storage/emulated/0/Podcasts/episode123.mp3"
    require(!path.isNullOrBlank()) { "Ref has no local path/uri" }
    return Uri.fromFile(File(path))
}

/**
 * Optional: enrich the notification/lockscreen UI.
 */
private fun resolveMetadata(ref: YourRefType): MediaMetadata {
    return MediaMetadata.Builder()
        .setTitle(ref.title ?: "Episode")
        .setArtist(ref.showName ?: "Podcast")
        .setArtworkUri(ref.artworkUri) // if you have one; can be null
        .build()
}

// --------------- Internal player UI (don’t change) ---------------

@Composable
private fun PlayerScreenInternal(
    mediaUri: Uri,
    metadata: MediaMetadata? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val token = remember {
        SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
    }

    var controller by remember { mutableStateOf<MediaController?>(null) }

    LaunchedEffect(token, mediaUri) {
        val future = MediaController.Builder(context, token).buildAsync()
        controller = future.await()

        controller?.apply {
            val item = MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaMetadata(metadata ?: MediaMetadata.EMPTY)
                .build()

            setMediaItem(item)
            prepare()
            play()
        }

        awaitDispose { MediaController.releaseFuture(future) }
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
