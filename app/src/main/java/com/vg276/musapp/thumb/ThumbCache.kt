package com.vg276.musapp.thumb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import com.vg276.musapp.THUMB_PATH
import com.vg276.musapp.base.BaseTask
import com.vg276.musapp.utils.printLog
import com.vg276.musapp.utils.thenNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

object ThumbCache
{
    private val lruCache = LruCache<String, Bitmap>(1024 * 1024 * 50) // 50 MB limit

    /*
     * Получаем изображение из памяти: RAM или DISK (ROM)
     */
    fun getImage(context: Context, albumId: String): Bitmap?
    {
        // Если ид пустой, то ответ 0
        if (albumId.isEmpty())
        {
            printLog("Thumb: id empty")
            return null
        }

        // через блок лет проверяем изображение в ОЗУ и выводим его
        lruCache[albumId]?.let {
            printLog("Thumb: cached from RAM")
            return it
        }

        // если в озу нет кэша, то проверяем его на диске и выводим его
        var file = context.cacheDir
        file = File("${file.absoluteFile}$THUMB_PATH${albumId}.jpg")

        if (file.exists()) {
            printLog("Thumb: cached from Disk")

            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            lruCache.put(albumId, bmp)

            return bmp
        }

        // если на диске нет кэша, то ответ 0
        // это необходимо, чтобы мы могли получить изображение из сети

        printLog("Thumb: not cache")
        return null
    }

    /*
     * Записываем изображение в озу (кэшируем в память)
     */
    private fun setImage(albumId: String, data: Bitmap?)
    {
        if (data == null)
            lruCache.remove(albumId)
        else
            lruCache.put(albumId, data)
    }

    /*
     * Сохраняем изображение на диск (кэшируем на диск)
     */
    private fun saveThumb(context: Context, albumId: String): FileOutputStream
    {
        var file = context.cacheDir
        file = File("${file.absoluteFile}$THUMB_PATH")

        if (!file.exists() && file.mkdirs()) {
            printLog("saveThumb: dir ${file.absoluteFile} created")
        } else{
            printLog("saveThumb: dir ${file.absoluteFile} not created")
        }

        file = File("${file.absoluteFile}/${albumId}.jpg")
        if(!file.exists()) {
            file.createNewFile()
        }

        return FileOutputStream(file)
    }

    /*
     * Загружаем кэш из память или из сети
     */
    fun load(context: Context,
             urlString: String,
             albumId: String,
             callback: (albumId: String, bitmap: Bitmap?) -> Unit)
    {
        if (albumId.isEmpty())
        {
            callback(albumId, null)
            return
        }

        getImage(context, albumId)?.let {
            callback(albumId, it)
        }.thenNull {
            if (urlString.isEmpty())
            {
                callback(albumId, null)
                return@thenNull
            }

            printLog("Thumb: received from URL")
            ThumbLoader(context, urlString, albumId, callback).execute()
        }
    }

    private class ThumbLoader(private val context: Context,
                              private val urlString: String,
                              private val albumId: String,
                              private val callback: (albumId: String, bitmap: Bitmap?) -> Unit): BaseTask()
    {
        override fun run() {
            try {
                // получаем изображение из сети
                val stream = URL(urlString).openStream()
                val bmp = BitmapFactory.decodeStream(stream)

                // сохраняем на диск
                bmp?.let {
                    val out = saveThumb(context, albumId)
                    it.compress(Bitmap.CompressFormat.JPEG, 100, out)

                    out.flush()
                    out.close()
                }

                // кэшируем в озу и выводим изображение через обратный вызов
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