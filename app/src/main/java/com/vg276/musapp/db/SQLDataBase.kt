package com.vg276.musapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vg276.musapp.MainApplication
import com.vg276.musapp.db.dao.DBAudioDao
import com.vg276.musapp.utils.printLog

class SQLDataBase(context: Context): SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION)
{
    private var dbAudioDao: DBAudioDao? = null

    fun getAudioDao(): DBAudioDao
    {
        if (dbAudioDao == null)
            dbAudioDao = DBAudioDao(this)
        return dbAudioDao!!
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_AUDIO)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        printLog("DB: onUpgrade: oldVersion = $oldVersion newVersion = $newVersion")
    }

    companion object
    {
        fun getInstance(): SQLDataBase
        {
            val context = MainApplication.getInstance()
            return SQLDataBase(context)
        }

        // config DB
        const val DB_VERSION = 1
        const val DB_NAME = "musapp_database.db"

        // tables
        const val TABLE_AUDIO_NAME = "audio"

        // audio table
        const val AUDIO_ID: String = "audioId"                  // String
        const val AUDIO_OWNER_ID: String = "audioOwnerId"       // String
        const val ARTIST: String = "artist"                     // String
        const val TITLE: String = "title"                       // String
        const val STREAM_URL: String = "streamUrl"              // String
        const val DURATION: String = "duration"                 // Integer
        const val IS_DOWNLOADED: String = "isDownloaded"        // Integer
        const val IS_EXPLICIT: String = "isExplicit"            // Integer
        const val THUMB: String = "thumb"                       // String
        const val ALBUM_ID: String = "albumId"                  // String
        const val ALBUM_TITLE: String = "albumTitle"            // String
        const val ALBUM_OWNER_ID: String = "albumOwnerId"       // String
        const val ALBUM_ACCESS_KEY: String = "albumAccessKey"   // String
        const val ARTISTS: String = "artists"                   // String
        const val TIMESTAMP: String = "timestamp"               // Long

        const val SQL_CREATE_AUDIO: String = "CREATE TABLE IF NOT EXISTS " +
                TABLE_AUDIO_NAME + " (" +
                AUDIO_ID + " TEXT PRIMARY KEY," +
                AUDIO_OWNER_ID + " TEXT," +
                ARTIST + " TEXT," +
                TITLE + " TEXT," +
                STREAM_URL + " TEXT," +
                DURATION + " INTEGER," +
                IS_DOWNLOADED + " INTEGER," +
                IS_EXPLICIT + " INTEGER," +
                THUMB + " TEXT," +
                ALBUM_ID + " TEXT," +
                ALBUM_TITLE + " TEXT," +
                ALBUM_OWNER_ID + " TEXT," +
                ALBUM_ACCESS_KEY + " TEXT," +
                ARTISTS + " TEXT," +
                TIMESTAMP + " INTEGER)"
    }
}