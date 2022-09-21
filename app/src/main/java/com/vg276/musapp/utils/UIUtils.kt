package com.vg276.musapp.utils

import android.content.Context
import android.util.Log
import com.vg276.musapp.KEY_RANDOM
import com.vg276.musapp.KEY_REPEAT

fun printLog(msg: String, error: Boolean = false)
{
    if (error)
        Log.e("MusApp", msg)
    else
        Log.i("MusApp", msg)
}

fun isRepeatAudio(context: Context): Boolean
{
    val settings = SettingsPreferences(context)
    return settings.getBoolean(KEY_REPEAT, false)
}

fun isRandomAudio(context: Context): Boolean
{
    val settings = SettingsPreferences(context)
    return settings.getBoolean(KEY_RANDOM, false)
}