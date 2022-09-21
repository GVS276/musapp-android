package com.vg276.musapp.db.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vg276.musapp.db.timestamp
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class AlbumModel: Serializable {
    val id = UUID.randomUUID().toString()
    var albumId: String = ""
    var title: String = ""
    var description: String = ""
    var thumb: String = ""
    var countTracks: Int = 0
    var createTime: Int = 0
    var updateTime: Int = 0
    var year: Int = 0
    var ownerId: Int = 0
    var accessKey: String = ""
    var isExplicit: Boolean = false
}

class ArtistModel: Serializable {
    var name: String = ""
    var domain: String = ""
    var id: String = ""
    var featured: Boolean = false
}

class AudioModel: Serializable
{
    var audioId: String = ""
    var audioOwnerId: String = ""
    var artist: String = ""
    var title: String = ""
    var streamUrl: String = ""
    var duration: Int = 0
    var isDownloaded: Boolean = false
    var isExplicit: Boolean = false
    var thumb: String = ""
    var albumId: String = ""
    var albumTitle: String = ""
    var albumOwnerId: String = ""
    var albumAccessKey: String = ""
    var artists: ArrayList<ArtistModel> = ArrayList()
    var timestamp: Long = timestamp()
    var isAddedToLibrary: Boolean = false
}

fun AudioModel.toJsonFromArtists(): String
{
    val type = object : TypeToken<ArrayList<ArtistModel>>() {}.type
    return Gson().toJson(artists, type)
}

fun fromJsonToArtists(json: String): ArrayList<ArtistModel>
{
    val type = object : TypeToken<ArrayList<ArtistModel>>() {}.type
    return Gson().fromJson(json, type)
}