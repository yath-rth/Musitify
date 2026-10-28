package com.yatharth.musitify.data.repository

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class YouTubeResult(
    val id: String,
    val url: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long?,
    val thumbnailUrl: String
)

@Singleton
class YouTubeSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun module() = run {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        Python.getInstance().getModule("downloader")
    }

    suspend fun search(query: String, limit: Int = 8): List<YouTubeResult> =
        withContext(Dispatchers.IO) {
            val raw = module().callAttr("search_youtube", query, limit).toString()
            Json.parseToJsonElement(raw).jsonArray.map { el ->
                val o = el.jsonObject
                fun str(key: String) = o[key]?.jsonPrimitive?.contentOrNull.orEmpty()
                YouTubeResult(
                    id = str("id"),
                    url = str("url"),
                    title = str("title"),
                    artist = str("artist"),
                    durationSeconds = o["duration"]?.jsonPrimitive?.doubleOrNull?.toLong(),
                    thumbnailUrl = str("thumbnail")
                )
            }
        }

    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        module().callAttr("get_stream_url", videoUrl).toString()
    }
}
