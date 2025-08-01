package com.kuehlconsulting.johnbirchsociety.ui.rssfeed

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kuehlconsulting.johnbirchsociety.data.DownloadRepository
import com.kuehlconsulting.johnbirchsociety.data.Mp3Downloader
import com.kuehlconsulting.johnbirchsociety.data.RssFeedService
import com.kuehlconsulting.johnbirchsociety.model.RssItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * UI state composed from RSS + Room(downloads)
 */
data class FeedItemUi(
    val item: RssItem,
    val isDownloaded: Boolean,
    val localRef: String?,
    val progress: Float
)

sealed class RssFeedUiState {
    data object Loading : RssFeedUiState()
    data class Error(val message: String) : RssFeedUiState()
    data object Ready : RssFeedUiState() // content comes from feedUi flow
}

class RssFeedViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val repo = DownloadRepository(appContext)
    private val rssFeedService = RssFeedService()

    // Raw RSS items kept in memory
    private val _rssItems = MutableStateFlow<List<RssItem>>(emptyList())

    // High-level screen status
    private val _status = MutableStateFlow<RssFeedUiState>(RssFeedUiState.Loading)
    val status: StateFlow<RssFeedUiState> = _status.asStateFlow()

    // Combined UI list = RSS items + downloads table
    val feedUi: StateFlow<List<FeedItemUi>> =
        combine(_rssItems, repo.observeAll()) { rss, downloads ->
            val byUrl = downloads.associateBy { it.enclosureUrl }
            rss.map { r ->
                val d = r.enclosureUrl?.let { byUrl[it] }
                FeedItemUi(
                    item = r,
                    isDownloaded = d?.isDownloaded == true,
                    localRef = d?.localRef,
                    progress = when {
                        d?.isDownloaded == true -> 1f
                        else -> r.downloadProgress
                    }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _status.value = RssFeedUiState.Loading
            try {
                val xml = rssFeedService.getRssFeedXml("https://rss.infowars.com/Alex.rss")
                if (xml.isNullOrBlank()) {
                    _status.value = RssFeedUiState.Error("Empty RSS response")
                    return@launch
                }
                val items = parseRssFeed(xml)
                _rssItems.value = items
                _status.value = RssFeedUiState.Ready
            } catch (e: Exception) {
                Log.e("RssFeedViewModel", "Failed to fetch RSS", e)
                _status.value = RssFeedUiState.Error("Error: ${e.localizedMessage ?: "unknown"}")
            }
        }
    }

    /**
     * Start a download and reflect progress in-memory; on completion write to DB.
     * Items are matched by enclosureUrl (stable key), never by full object equality.
     */
    fun downloadMp3(item: RssItem) {
        val url = item.enclosureUrl ?: return
        val downloader = Mp3Downloader(appContext)

        // Nudge UI so a progress bar shows immediately.
        _rssItems.update { list ->
            list.map { if (it.enclosureUrl == url) it.copy(downloadProgress = 0.01f) else it }
        }

        viewModelScope.launch {
            downloader.downloadMp3(
                rssItem = item,
                onProgress = { progress ->
                    _rssItems.update { list ->
                        list.map {
                            if (it.enclosureUrl == url) it.copy(downloadProgress = progress)
                            else it
                        }
                    }
                },
                onCompletion = { localRef ->
                    if (localRef != null) {
                        // Persist to DB; UI will reflect via feedUi combine()
                        viewModelScope.launch {
                            runCatching {
                                repo.upsertCompleted(
                                    enclosureUrl = url,
                                    localRef = localRef,
                                    bytesExpected = item.declaredLength
                                )
                            }.onFailure { e ->
                                Log.e("RssFeedViewModel", "DB upsert failed", e)
                            }
                        }
                    } else {
                        // Reset progress on failure
                        _rssItems.update { list ->
                            list.map {
                                if (it.enclosureUrl == url) it.copy(downloadProgress = 0f)
                                else it
                            }
                        }
                    }
                }
            )
        }
    }

    // ---- RSS parsing ----

    private fun parseRssFeed(xml: String): List<RssItem> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        val items = mutableListOf<RssItem>()
        var event = parser.eventType
        var current: RssItem? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> current = RssItem()
                    "title" -> current?.title = parser.nextText()
                    "description" -> current?.description = parser.nextText()
                    "enclosure" -> {
                        val enclosureUrl = parser.getAttributeValue(null, "url")
                        val lengthAttr = parser.getAttributeValue(null, "length")
                        if (!enclosureUrl.isNullOrEmpty()) current?.enclosureUrl = enclosureUrl
                        current?.declaredLength = lengthAttr?.toLongOrNull()
                    }
                    // Optional: pubDate/guid if you want them later
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" && current != null) {
                    items += current
                    current = null
                }
            }
            event = parser.next()
        }
        return items
    }
}
