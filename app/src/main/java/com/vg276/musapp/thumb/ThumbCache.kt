package com.vg276.musapp.thumb

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import com.vg276.musapp.base.BaseTask
import com.vg276.musapp.utils.printLog
import com.vg276.musapp.utils.thenNull
import java.io.IOException
import java.net.URL

object ThumbCache
{
    private val lruCache = LruCache<String, Bitmap>(1024 * 1024 * 50) // 50 MB limit

    fun getImage(albumId: String): Bitmap?
    {
        return lruCache[albumId]
    }

    private fun setImage(albumId: String, data: Bitmap?)
    {
        if (data == null)
            lruCache.remove(albumId)
        else
            lruCache.put(albumId, data)
    }

    fun load(urlString: String,
             albumId: String,
             callback: (albumId: String, bitmap: Bitmap?) -> Unit)
    {
        if (albumId.isEmpty())
        {
            callback(albumId, null)
            return
        }

        getImage(albumId)?.let {
            printLog("Thumb: cached from RAM")
            callback(albumId, it)
        }.thenNull {
            if (urlString.isEmpty())
            {
                callback(albumId, null)
                return@thenNull
            }

            printLog("Thumb: received from URL")
            ThumbLoader(urlString, albumId, callback).execute()
        }
    }

    private class ThumbLoader(private val urlString: String,
                              private val albumId: String,
                              private val callback: (albumId: String, bitmap: Bitmap?) -> Unit): BaseTask()
    {
        override fun run() {
            try {
                val stream = URL(urlString).openStream()
                val bmp = BitmapFactory.decodeStream(stream)

                setImage(albumId, bmp)
                callback(albumId, bmp)
            } catch (e: IOException) {
                e.localizedMessage?.let {
                    printLog(it)
                }
            }
        }
    }
}