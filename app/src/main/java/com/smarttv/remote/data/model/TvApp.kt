package com.smarttv.remote.data.model

enum class AppCategory {
    ALL,
    STREAMING,
    INSTALLED
}

data class TvApp(
    val id: String,
    val name: String,
    val packageName: String,
    val deepLinkUri: String,
    val primaryColor: Long,
    val category: AppCategory = AppCategory.STREAMING,
    val isInstalled: Boolean = true
) {
    companion object {
        val DEFAULT_APPS = listOf(
            TvApp("netflix", "Netflix", "com.netflix.ninja", "https://www.netflix.com/title", 0xFFE50914, AppCategory.STREAMING),
            TvApp("youtube", "YouTube", "com.google.android.youtube.tv", "vnd.youtube://", 0xFFFF0000, AppCategory.STREAMING),
            TvApp("jiohotstar", "JioHotstar", "in.startv.hotstar", "hotstar://", 0xFF8A2BE2, AppCategory.STREAMING),
            TvApp("primevideo", "Prime Video", "com.amazon.amazonvideo.livingroom", "https://app.primevideo.com", 0xFF00A8E1, AppCategory.STREAMING),
            TvApp("sonyliv", "Sony LIV", "com.sonyliv", "sonyliv://", 0xFF2A2D3A, AppCategory.STREAMING),
            TvApp("zee5", "ZEE5", "com.graymatrix.did", "zee5://", 0xFF6C2BD9, AppCategory.STREAMING),
            TvApp("aha", "aha", "aha.video.app", "aha://", 0xFFFF4500, AppCategory.STREAMING),
            TvApp("mxplayer", "MX Player", "com.mxtech.videoplayer.ad", "mxplayer://", 0xFF007AFF, AppCategory.STREAMING),
            TvApp("jiocinema", "JioCinema", "com.jio.media.ondemand", "jiocinema://", 0xFFE91E63, AppCategory.STREAMING),
            TvApp("disneyhotstar", "Disney+ Hotstar", "in.startv.hotstar", "hotstar://", 0xFF1434CB, AppCategory.STREAMING),
            TvApp("spotify", "Spotify", "com.spotify.tv.android", "spotify://", 0xFF1DB954, AppCategory.INSTALLED),
            TvApp("vlc", "VLC", "org.videolan.vlc", "vlc://", 0xFFFF7700, AppCategory.INSTALLED)
        )
    }
}
