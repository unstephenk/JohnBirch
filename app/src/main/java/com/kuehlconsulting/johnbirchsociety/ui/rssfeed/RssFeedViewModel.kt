package com.kuehlconsulting.johnbirchsociety.ui.rssfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuehlconsulting.johnbirchsociety.data.RssFeedService
import com.kuehlconsulting.johnbirchsociety.model.RssItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                        "link" -> currentItem?.link = parser.nextText()
                        // Add other relevant fields if needed
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
}

// Define the UI State for your screen
sealed class RssFeedUiState {
    object Loading : RssFeedUiState()
    data class Success(val rssItems: List<RssItem>) : RssFeedUiState()
    data class Error(val message: String) : RssFeedUiState()
}
