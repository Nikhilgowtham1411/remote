package com.smarttv.remote.data.model

data class SkipDurationConfig(
    val backwardSeconds: Int = 10,
    val forwardSeconds: Int = 10,
    val customDuration: Int = 45
)
