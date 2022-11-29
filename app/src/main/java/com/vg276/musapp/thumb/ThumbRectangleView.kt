package com.vg276.musapp.thumb

import android.content.Context
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.vg276.musapp.R

class ThumbRectangleView(context: Context, attrs: AttributeSet): AppCompatImageView(context, attrs) {
    init {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        setBackgroundResource(R.drawable.shape_rectangle)
        scaleType = ScaleType.CENTER_CROP
    }
}