package com.smarttv.remote.data.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Loads what is streaming in India on JioHotstar and Netflix from The Movie Database (TMDB).
 * TMDB is a free service; it needs a personal API key (Settings > Home content).
 */
class TmdbClient(private val apiKey: String) {

    companion object {
        private const val BASE = "https://api.themoviedb.org/3"
        private const val IMAGE = "https://image.tmdb.org/t/p/w342"
        private const val REGION = "IN"
    }

    class AuthException : IOException("TMDB did not accept the key.")

    private fun isBearer() = apiKey.startsWith("eyJ")

    private fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val query = buildString {
            params.forEach { (k, v) -> append('&').append(k).append('=').append(URLEncoder.encode(v, "UTF-8")) }
            if (!isBearer()) append("&api_key=").append(URLEncoder.encode(apiKey, "UTF-8"))
        }.removePrefix("&")
        val conn = URL("$BASE$path?$query").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            if (isBearer()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
            val code = conn.responseCode
            if (code == 401) throw AuthException()
            if (code !in 200..299) throw IOException("TMDB error $code")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun providerIds(kind: String, vararg names: String): String {
        val list = get("/watch/providers/$kind", mapOf("watch_region" to REGION)).optJSONArray("results") ?: return ""
        val ids = mutableListOf<Int>()
        for (i in 0 until list.length()) {
            val p = list.getJSONObject(i)
            val name = p.optString("provider_name").lowercase()
            if (names.any { name.contains(it) }) ids += p.getInt("provider_id")
        }
        return ids.distinct().joinToString("|")
    }

    private fun discover(
        kind: String,
        providers: String,
        language: String?,
        platform: Platform
    ): List<ContentItem> {
        if (providers.isBlank()) return emptyList()
        val params = mutableMapOf(
            "watch_region" to REGION,
            "with_watch_providers" to providers,
            "with_watch_monetization_types" to "flatrate|free|ads",
            "sort_by" to "popularity.desc",
            "language" to "en-US"
        )
        if (language != null) params["with_original_language"] = language
        val results = get("/discover/$kind", params).optJSONArray("results") ?: return emptyList()
        val isSeries = kind == "tv"
        val items = mutableListOf<ContentItem>()
        for (i in 0 until results.length()) {
            val o = results.getJSONObject(i)
            val title = o.optString(if (isSeries) "name" else "title")
            if (title.isBlank()) continue
            val poster = o.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
            items += ContentItem(
                id = o.getInt("id"),
                title = title,
                originalTitle = o.optString(if (isSeries) "original_name" else "original_title"),
                overview = o.optString("overview"),
                posterUrl = poster?.let { IMAGE + it },
                year = o.optString(if (isSeries) "first_air_date" else "release_date").take(4),
                rating = o.optDouble("vote_average", 0.0),
                isSeries = isSeries,
                platform = platform
            )
        }
        return items.filter { it.posterUrl != null }.take(20)
    }

    suspend fun loadHome(): List<ContentRow> = withContext(Dispatchers.IO) {
        val hotTv = providerIds("tv", "hotstar")
        val hotMovie = providerIds("movie", "hotstar")
        val nfTv = providerIds("tv", "netflix")
        val nfMovie = providerIds("movie", "netflix")

        coroutineScope {
            val rows = listOf(
                "Telugu series on JioHotstar" to async { discover("tv", hotTv, "te", Platform.JIOHOTSTAR) },
                "Telugu movies on JioHotstar" to async { discover("movie", hotMovie, "te", Platform.JIOHOTSTAR) },
                "Popular on JioHotstar" to async { discover("tv", hotTv, null, Platform.JIOHOTSTAR) },
                "Telugu on Netflix" to async { discover("movie", nfMovie, "te", Platform.NETFLIX) },
                "Netflix series" to async { discover("tv", nfTv, null, Platform.NETFLIX) },
                "Netflix movies" to async { discover("movie", nfMovie, null, Platform.NETFLIX) }
            )
            rows.map { (title, items) -> ContentRow(title, items.await()) }.filter { it.items.isNotEmpty() }
        }
    }
}
