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
    MEDIA_FAST_FORWARD(90, "Fast Forward"),

    // Extra keys
    MENU(82, "Menu"),
    TV_INPUT(178, "Input"),
    SETTINGS(176, "Settings"),
    INFO(165, "Info"),
    GUIDE(172, "Guide"),

    // Number keypad
    NUM_0(7, "0"), NUM_1(8, "1"), NUM_2(9, "2"), NUM_3(10, "3"), NUM_4(11, "4"),
    NUM_5(12, "5"), NUM_6(13, "6"), NUM_7(14, "7"), NUM_8(15, "8"), NUM_9(16, "9")
}
