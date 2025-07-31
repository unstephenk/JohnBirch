package com.kuehlconsulting.johnbirchsociety.ui.rssfeed

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel // For ViewModel in Composable

@Composable
fun RssFeedScreen(
    viewModel: RssFeedViewModel = viewModel() // Get the ViewModel instance
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is RssFeedUiState.Loading -> {
            CircularProgressIndicator() // Show a loading indicator
        }
        is RssFeedUiState.Success -> {
            val rssItems = (uiState as RssFeedUiState.Success).rssItems
            LazyColumn {
                items(rssItems) { item ->
                    Text(text = item.title ?: "No Title")
                    Text(text = item.description ?: "No Description")
                    Text(text = item.link ?: "No Link")
                    // Add more UI elements to display other fields if needed
                }
            }
        }
        is RssFeedUiState.Error -> {
            val message = (uiState as RssFeedUiState.Error).message
            Text(text = "Error: $message") // Display an error message
        }
    }
}
