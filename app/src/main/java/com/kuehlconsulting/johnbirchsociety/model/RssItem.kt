package com.kuehlconsulting.johnbirchsociety.model

data class RssItem(
    var title: String? = null,
    var description: String? = null,
    var link: String? = null
    // Add other fields as needed, e.g., pubDate, author
)
