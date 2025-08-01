package com.kuehlconsulting.johnbirchsociety.ui.rssfeed

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuehlconsulting.johnbirchsociety.model.RssItem

/**
 * Full screen for the RSS feed. Displays loading/error state from [status]
 * and the combined list rows from [feedUi].
 */
@Composable
fun RssFeedScreen(
    viewModel: RssFeedViewModel = viewModel(),
    onNavigateToPlayer: (String) -> Unit
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycleFix()
    val rows by viewModel.feedUi.collectAsStateWithLifecycleFix()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (status) {
            is RssFeedUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }

            is RssFeedUiState.Error -> {
                val message = (status as RssFeedUiState.Error).message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: $message",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Retry") }
                }
            }

            is RssFeedUiState.Ready -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    items(
                        items = rows,
                        key = { it.item.enclosureUrl ?: it.item.title ?: it.hashCode() }
                    ) { row ->
                        RssItemCard(
                            item = row.item.copy(
                                isDownloaded = row.isDownloaded,
                                localFilePath = row.localRef,
                                downloadProgress = row.progress
                            ),
                            onLongPress = {
                                Toast
                                    .makeText(context, row.item.title ?: "Item", Toast.LENGTH_SHORT)
                                    .show()
                            },
                            onDownloadClick = { clicked ->
                                // Optional: quick toast of the URL for debugging
                                clicked.enclosureUrl?.let {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                                viewModel.downloadMp3(clicked)
                            },
                            onPlayClick = {
                                val ref = row.localRef
                                if (ref.isNullOrBlank()) {
                                    Toast.makeText(
                                        context,
                                        "File not downloaded yet!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onNavigateToPlayer(ref)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A small helper so this file compiles whether or not you use lifecycle-runtime-compose.
 * If you already use lifecycle-runtime-compose, replace calls with collectAsStateWithLifecycle.
 */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleFix(): androidx.compose.runtime.State<T> {
    // If you have lifecycle-runtime-compose, prefer:
    // return this.collectAsStateWithLifecycle()
    return this.collectAsState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RssItemCard(
    item: RssItem,
    onLongPress: () -> Unit,
    onDownloadClick: (RssItem) -> Unit,
    onPlayClick: (RssItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongPress,
                onClick = { if (item.isDownloaded) onPlayClick(item) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title ?: "No Title",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = item.description ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            when {
                item.isDownloaded -> {
                    Button(onClick = { onPlayClick(item) }) { Text("Play") }
                    if (!item.localFilePath.isNullOrBlank()) {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                item.downloadProgress < 0f -> {
                    // Indeterminate when total size unknown
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    Text(
                        text = "Downloading…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                item.downloadProgress in 0f..0.999f -> {
                    LinearProgressIndicator(
                        progress = item.downloadProgress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    Text(
                        text = "${(item.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                else -> {
                    Button(
                        onClick = { onDownloadClick(item) },
                        enabled = item.enclosureUrl != null
                    ) { Text("Download") }

                    if (item.enclosureUrl == null) {
                        Text(
                            text = "No audio URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
