package com.smarttv.remote.data.model

enum class ConnectionStatus {
    DISCONNECTED,
    CHECKING_WIFI,
    DISCOVERING,
    CONNECTING,
    PAIRING_REQUIRED,
    PAIRING_IN_PROGRESS,
    CONNECTED,
    ERROR
}
