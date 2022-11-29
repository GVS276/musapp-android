package com.vg276.musapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.ColorUtils
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

fun getColorFromBitmap(bitmap: Bitmap): Int
{
    var r = 0
    var g = 0
    var b = 0
    var n = 0
    val w = bitmap.width
    val h = bitmap.height

    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    for (i in pixels.indices)
    {
        val p = pixels[i]
        r += Color.red(p)
        g += Color.green(p)
        b += Color.blue(p)
        n += 1
    }

    var color = Color.rgb(r / n, g / n, b / n)
    val luminance = ColorUtils.calculateLuminance(color)

    if (luminance < 0.01 || luminance > 0.5)
    {
        color = Color.rgb(100, 100, 100)
    }

    return color
}