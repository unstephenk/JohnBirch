package com.kuehlconsulting.johnbirchsociety.ui.rssfeed

import android.R
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // For ViewModel in Composable
import com.kuehlconsulting.johnbirchsociety.model.RssItem

@OptIn(ExperimentalFoundationApi::class) // Needed for combinedClickable
@Composable
fun RssFeedScreen(
    viewModel: RssFeedViewModel = viewModel(),
    onNavigateToPlayer: (String) -> Unit // Callback to navigate to the player screen
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // Get the context for showing Toast messages

    when (uiState) {
        is RssFeedUiState.Loading -> {
            CircularProgressIndicator() // Show a loading indicator
        }
        is RssFeedUiState.Success -> {
            val rssItems = (uiState as RssFeedUiState.Success).rssItems
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp), // Adds space between items
                modifier = Modifier.padding(horizontal = 8.dp) // Adds horizontal padding to the list
            ) {
                items(rssItems) { item ->
                    RssItemCard(
                        item = item,
                        onLongPress = {
                            Toast.makeText(context, "Long pressed: ${item.title}", Toast.LENGTH_SHORT).show()
                        },
                        onDownloadClick = {
                            clicked -> val url = clicked.enclosureUrl
                                Toast.makeText(context, url ?: "No enclosure URL for this item",
                                    Toast.LENGTH_SHORT
                            ).show()

                            if (!url.isNullOrBlank()) {
                                viewModel.downloadMp3(context, clicked)
                            }
                        },
                        onPlayClick = {
                            // Navigate to the Player Screen with the local file path
                            it.localFilePath?.let { path -> onNavigateToPlayer(path)
                            } ?: Toast.makeText(context, "File not downloaded yet!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        is RssFeedUiState.Error -> {
            val message = (uiState as RssFeedUiState.Error).message
            Text(text = "Error: $message", modifier = Modifier.padding(8.dp)) // Added padding for consistency
        }
    }
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
            .fillMaxWidth() // Make the card fill the width
            .combinedClickable( // Use combinedClickable for long press
                onLongClick = onLongPress,
                onClick = { onPlayClick(item) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium // Use a consistent shape from your theme
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Add padding inside the card
            Text(
                text = item.title ?: "No Title",
                style = MaterialTheme.typography.titleMedium, // Use a typography style
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = item.description ?: "No Description",
                style = MaterialTheme.typography.bodyMedium, // Use a typography style
                modifier = Modifier.padding(bottom = 4.dp)
            )
            when {
                item.isDownloaded -> {
                    Button(onClick = { onPlayClick(item) }) {
                        Text("Play")
                    }
                }

                item.downloadProgress > 0f && item.downloadProgress < 1f -> {
                    LinearProgressIndicator(
                        progress = { item.downloadProgress },
                        modifier = Modifier.fillMaxWidth().padding(top=4.dp),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
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
                }

            }
        }
    }
}
