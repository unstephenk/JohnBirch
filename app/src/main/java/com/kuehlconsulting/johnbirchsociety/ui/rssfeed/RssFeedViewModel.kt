package com.kuehlconsulting.johnbirchsociety.ui.rssfeed

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuehlconsulting.johnbirchsociety.data.Mp3Downloader
import com.kuehlconsulting.johnbirchsociety.data.RssFeedService
import com.kuehlconsulting.johnbirchsociety.model.RssItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class RssFeedViewModel(
    private val rssFeedService: RssFeedService = RssFeedService()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RssFeedUiState>(RssFeedUiState.Loading)
    val uiState: StateFlow<RssFeedUiState> = _uiState

    init {
        fetchRssFeed()
    }

    fun fetchRssFeed() {
        viewModelScope.launch {
            _uiState.value = RssFeedUiState.Loading
            try {
                val xml = rssFeedService.getRssFeedXml("https://rss.infowars.com/Alex.rss")
                if (xml != null) {
                    val rssItems = parseRssFeed(xml)
                    _uiState.value = RssFeedUiState.Success(rssItems)
                } else {
                    _uiState.value = RssFeedUiState.Error("Failed to fetch RSS feed.")
                }
            } catch (e: Exception) {
                _uiState.value = RssFeedUiState.Error("Error: ${e.localizedMessage}")
                Log.e("RssFeedViewModel", "Failed to fetch RSS", e)
            }
        }
    }

    private fun parseRssFeed(xml: String): List<RssItem> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        val rssItems = mutableListOf<RssItem>()
        var currentItem: RssItem? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> currentItem = RssItem()
                        "title" -> currentItem?.title = parser.nextText()
                        "description" -> currentItem?.description = parser.nextText()
                        "enclosure" -> {
                            val enclosureUrl = parser.getAttributeValue(null, "url")
                            if (!enclosureUrl.isNullOrEmpty()) {
                                currentItem?.enclosureUrl = enclosureUrl
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && currentItem != null) {
                        rssItems.add(currentItem)
                        currentItem = null
                    }
                }
            }
            eventType = parser.next()
        }
        return rssItems
    }

    fun downloadMp3(context: Context, item: RssItem) {
        // Prevent re-downloading if already downloaded or currently downloading
        if (item.isDownloaded || _uiState.value !is RssFeedUiState.Success) return

        val downloader = Mp3Downloader(context)
        viewModelScope.launch {
            // Update UI state to show download in progress
            _uiState.update { currentState ->
                if (currentState is RssFeedUiState.Success) {
                    currentState.copy(
                        rssItems = currentState.rssItems.map { rssItem ->
                            if (rssItem == item) rssItem.copy(downloadProgress = 0.01f) // Start progress slightly above 0
                            else rssItem
                        }
                    )
                } else currentState
            }

            downloader.downloadMp3(
                rssItem = item,
                onProgress = { progress ->
                    _uiState.update { currentState ->
                        if (currentState is RssFeedUiState.Success) {
                            currentState.copy(
                                rssItems = currentState.rssItems.map { rssItem ->
                                    if (rssItem == item) rssItem.copy(downloadProgress = progress)
                                    else rssItem
                                }
                            )
                        } else currentState
                    }
                },
                onCompletion = { filePath ->
                    _uiState.update { currentState ->
                        if (currentState is RssFeedUiState.Success) {
                            currentState.copy(
                                rssItems = currentState.rssItems.map { rssItem ->
                                    if (rssItem == item) {
                                        if (filePath != null) {
                                            rssItem.copy(
                                                isDownloaded = true,
                                                localFilePath = filePath,
                                                downloadProgress = 1f
                                            )
                                        } else {
                                            // Handle download failure
                                            rssItem.copy(downloadProgress = 0f) // Reset progress
                                        }
                                    } else rssItem
                                }
                            )
                        } else currentState
                    }
                }
            )
        }
    }
}

// Define the UI State for your screen
sealed class RssFeedUiState {
    data object Loading : RssFeedUiState()
    data class Success(val rssItems: List<RssItem>) : RssFeedUiState()
    data class Error(val message: String) : RssFeedUiState()
}
