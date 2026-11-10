package com.smarttv.remote.data.model

data class MediaState(
    val title: String = "Now Playing",
    val subtitle: String = "Controls whatever is playing on your TV",
    val posterUrl: String = "",
    val currentPositionSeconds: Long = 0,
    val totalDurationSeconds: Long = 0,
    val isPlaying: Boolean = true
) {
    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    val progressFraction: Float
        get() = if (totalDurationSeconds > 0) currentPositionSeconds.toFloat() / totalDurationSeconds else 0f
}
