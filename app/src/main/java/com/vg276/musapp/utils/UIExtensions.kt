package com.vg276.musapp.utils

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigInteger
import java.security.MessageDigest

fun <T> T?.thenNull(block: () -> Unit) = this ?: block()

fun String.md5(): String
{
    val md = MessageDigest.getInstance("MD5")
    val bigInt = BigInteger(1, md.digest(this.toByteArray(Charsets.UTF_8)))
    return String.format("%032x", bigInt)
}

fun String.encoded(): String
{
    return Uri.encode(this)
}

fun Int.addZero(): String {
    return if (this / 10 == 0) "0$this" else "$this"
}

fun Int.toTime(): String {
    val minutes = this % 3600 / 60
    val seconds = this % 60
    return "${minutes.addZero()}:${seconds.addZero()}"
}

fun Boolean.toInt(): Int
{
    return if (this) 1 else 0
}

fun Window.systemBarsTransparent()
{
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setDecorFitsSystemWindows(false)
    } else {
        setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }
}

fun Window.statusBarLight(value: Boolean)
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        WindowCompat.getInsetsController(this, decorView).apply {
            isAppearanceLightStatusBars = !value
        }
    } else {
        decorView.systemUiVisibility =
            if (value) {
                decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
    }
}

fun View.applyWindowInsets(set: (View, WindowInsetsCompat) -> WindowInsetsCompat)
{
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        set(v, insets)
    }

    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}