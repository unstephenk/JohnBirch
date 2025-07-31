package com.kuehlconsulting.johnbirchsociety.model

import android.net.Uri

data class RssItem(
    var title: String? = null,
    var description: String? = null,
    var enclosureUrl: String? = null, // To store the MP3 URL
    var localFilePath: String? = null, // To store the path of the downloaded file
    var isDownloaded: Boolean = false,
    var downloadProgress: Float = 0f // 0.0 to 1.0
)
