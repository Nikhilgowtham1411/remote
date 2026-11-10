package com.smarttv.remote.data.model

enum class RemoteCommand(val keyCode: Int, val label: String) {
    // D-PAD & Navigation
    DPAD_UP(19, "Up"),
    DPAD_DOWN(20, "Down"),
    DPAD_LEFT(21, "Left"),
    DPAD_RIGHT(22, "Right"),
    DPAD_CENTER(23, "OK"),
    BACK(4, "Back"),
    HOME(3, "Home"),
    RECENT_APPS(187, "Recent Apps"),

    // Power & Audio
    POWER(26, "Power"),
    MUTE(164, "Mute"),
    VOLUME_UP(24, "Volume Up"),
    VOLUME_DOWN(25, "Volume Down"),

    // Channel
    CHANNEL_UP(166, "Channel Up"),
    CHANNEL_DOWN(167, "Channel Down"),

    // Media Playback
    MEDIA_PLAY_PAUSE(85, "Play/Pause"),
    MEDIA_PLAY(126, "Play"),
    MEDIA_PAUSE(127, "Pause"),
    MEDIA_PREVIOUS(88, "Previous"),
    MEDIA_NEXT(87, "Next"),
    MEDIA_STOP(86, "Stop"),
    MEDIA_REWIND(89, "Rewind"),
    MEDIA_FAST_FORWARD(90, "Fast Forward")
}
