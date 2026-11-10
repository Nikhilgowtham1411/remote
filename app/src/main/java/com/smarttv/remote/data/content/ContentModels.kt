package com.smarttv.remote.data.content

enum class Platform(val label: String, val tvPackage: String) {
    JIOHOTSTAR("JioHotstar", "in.startv.hotstar.dplus.tv"),
    NETFLIX("Netflix", "com.netflix.ninja")
}

data class ContentItem(
    val id: Int,
    val title: String,
    val originalTitle: String,
    val overview: String,
    val posterUrl: String?,
    val year: String,
    val rating: Double,
    val isSeries: Boolean,
    val platform: Platform
)

data class ContentRow(
    val title: String,
    val items: List<ContentItem>
)

sealed class ContentState {
    object Idle : ContentState()
    object Loading : ContentState()
    /** No TMDB key saved yet. */
    object NeedsKey : ContentState()
    data class Error(val message: String) : ContentState()
    data class Loaded(val rows: List<ContentRow>) : ContentState()
}
