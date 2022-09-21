package com.vg276.musapp

enum class PlayerCommand(val value: Int)
{
    Error(0),
    Idle(1),
    Buffering(2),
    Ready(3),
    Playing(4),
    Paused(5),
    Finished(6)
}

enum class AudioItemClick
{
    Item,
    Menu
}

enum class AudioAdapterType
{
    AudioFromAlbum,
    OtherAudio
}

const val KEY_TOKEN = "token"
const val KEY_SECRET = "secret"
const val KEY_USER_ID = "userId"
const val KEY_REPEAT = "repeat"
const val KEY_RANDOM = "random"

const val PENDING_INTENT_CONTENT = 1000
const val INTENT_TYPE = "intentType"
const val INTENT_TYPE_PLAYER = 1

const val CLIENT_ID = 2274003
const val CLIENT_SECRET = "hHbZxrka2uZ6jB1inYsH"
const val USER_AGENT = "VKAndroidApp/5.52-4543 (Android 5.1.1; SDK 22; x86_64; unknown Android SDK built for x86_64; en; 320x240)"