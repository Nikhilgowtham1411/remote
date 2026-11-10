package com.smarttv.remote.data.model

data class MediaState(
    val title: String = "The Last of Us",
    val subtitle: String = "S1 E3 · Long, Long Time",
    val posterUrl: String = "",
    val currentPositionSeconds: Long = 754, // 12:34
    val totalDurationSeconds: Long = 3370,  // 56:10
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
