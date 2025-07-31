package com.kuehlconsulting.johnbirchsociety.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class RssFeedService {
    private val client = HttpClient()

    suspend fun getRssFeedXml(url: String): String? {
        return try {
            val response: HttpResponse = client.get(url)
            response.body() // Returns the XML as a String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
