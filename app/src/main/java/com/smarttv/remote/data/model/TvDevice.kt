package com.smarttv.remote.data.model

data class TvDevice(
    val id: String,
    val name: String,
    val model: String = "Google TV",
    val host: String,
    val port: Int = 6466,
    val pairingPort: Int = 6467,
    val macAddress: String? = null,
    val isGoogleTv: Boolean = true,
    val isPaired: Boolean = false,
    val isAvailable: Boolean = true
)
