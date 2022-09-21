package com.vg276.musapp.db.dao

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.vg276.musapp.db.SQLDataBase
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.db.model.fromJsonToArtists
import com.vg276.musapp.db.model.toJsonFromArtists
import com.vg276.musapp.utils.toInt

class DBAudioDao(private val db: SQLDataBase)
{
    fun insertAudio(audio: AudioModel)
    {
        val writableDb = db.writableDatabase
        val values = ContentValues()

        values.put(SQLDataBase.AUDIO_ID, audio.audioId)
        values.put(SQLDataBase.AUDIO_OWNER_ID, audio.audioOwnerId)
        values.put(SQLDataBase.ARTIST, audio.artist)
        values.put(SQLDataBase.TITLE, audio.title)
        values.put(SQLDataBase.STREAM_URL, audio.streamUrl)
        values.put(SQLDataBase.DURATION, audio.duration)
        values.put(SQLDataBase.IS_DOWNLOADED, audio.isDownloaded.toInt())
        values.put(SQLDataBase.IS_EXPLICIT, audio.isExplicit.toInt())
        values.put(SQLDataBase.THUMB, audio.thumb)
        values.put(SQLDataBase.ALBUM_ID, audio.albumId)
        values.put(SQLDataBase.ALBUM_TITLE, audio.albumTitle)
        values.put(SQLDataBase.ALBUM_OWNER_ID, audio.albumOwnerId)
        values.put(SQLDataBase.ALBUM_ACCESS_KEY, audio.albumAccessKey)
        values.put(SQLDataBase.ARTISTS, audio.toJsonFromArtists())
        values.put(SQLDataBase.TIMESTAMP, audio.timestamp)

        writableDb.insertWithOnConflict(SQLDataBase.TABLE_AUDIO_NAME, null,
            values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @SuppressLint("Recycle")
    fun getAllAudio(): ArrayList<AudioModel>
    {
        val list = ArrayList<AudioModel>()
        val readableDb = db.readableDatabase
        val cursor = readableDb.query(SQLDataBase.TABLE_AUDIO_NAME, null,
            null, null, null, null, SQLDataBase.TIMESTAMP+" DESC")

        if ((null != cursor) && (cursor.moveToFirst()))
        {
            do {
                val model = getAudioFromCursor(cursor)
                list.add(model)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return list
    }

    @SuppressLint("Recycle")
    fun getAudioById(audioId: String): AudioModel?
    {
        var model: AudioModel? = null
        val readableDb = db.readableDatabase

        val selection: String = SQLDataBase.AUDIO_ID + " = ? "
        val selectionArgs = arrayOf(audioId)

        val cursor = readableDb.query(SQLDataBase.TABLE_AUDIO_NAME, null,
            selection, selectionArgs, null, null, null)

        if ((null != cursor) && (cursor.moveToFirst()))
        {
            model = getAudioFromCursor(cursor)
            cursor.close()
        }

        return model
    }

    fun deleteAudioById(audioId: String)
    {
        val writableDb = db.writableDatabase

        val selection: String = SQLDataBase.AUDIO_ID + " = ? "
        val selectionArgs = arrayOf(audioId)

        writableDb.delete(SQLDataBase.TABLE_AUDIO_NAME, selection, selectionArgs)
    }

    @SuppressLint("Range")
    private fun getAudioFromCursor(cursor: Cursor): AudioModel
    {
        val model = AudioModel()

        var str = cursor.getString(cursor.getColumnIndex(SQLDataBase.AUDIO_ID))
        model.audioId = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.AUDIO_OWNER_ID))
        model.audioOwnerId = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.ARTIST))
        model.artist = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.TITLE))
        model.title = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.STREAM_URL))
        model.streamUrl = str

        var intValue = cursor.getInt(cursor.getColumnIndex(SQLDataBase.DURATION))
        model.duration = intValue

        intValue = cursor.getInt(cursor.getColumnIndex(SQLDataBase.IS_DOWNLOADED))
        model.isDownloaded = intValue == 1

        intValue = cursor.getInt(cursor.getColumnIndex(SQLDataBase.IS_EXPLICIT))
        model.isExplicit = intValue == 1

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.THUMB))
        model.thumb = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.ALBUM_ID))
        model.albumId = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.ALBUM_TITLE))
        model.albumTitle = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.ALBUM_OWNER_ID))
        model.albumOwnerId = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.ALBUM_ACCESS_KEY))
        model.albumAccessKey = str

        str = cursor.getString(cursor.getColumnIndex(SQLDataBase.ARTISTS))
        model.artists = fromJsonToArtists(str)

        val longValue = cursor.getLong(cursor.getColumnIndex(SQLDataBase.TIMESTAMP))
        model.timestamp = longValue

        model.isAddedToLibrary = true
        return model
    }
}