package com.kuehlconsulting.johnbirchsociety.model

data class RssItem(
    var title: String? = null,
    var description: String? = null,
    var enclosureUrl: String? = null, // To store the MP3 URL
    var localFilePath: String? = null, // To store the path of the downloaded file
    var isDownloaded: Boolean = false,
    var downloadProgress: Float = 0f,
    var declaredLength: Long? = null,
    var lastPlayedAt: Long? = null
)
