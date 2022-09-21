package com.vg276.musapp.core

import android.annotation.SuppressLint
import android.provider.Settings.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.vg276.musapp.CLIENT_ID
import com.vg276.musapp.CLIENT_SECRET
import com.vg276.musapp.MainApplication
import com.vg276.musapp.db.DBController
import com.vg276.musapp.db.model.AlbumModel
import com.vg276.musapp.db.model.ArtistModel
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.utils.encoded
import com.vg276.musapp.utils.md5
import com.vg276.musapp.utils.printLog
import com.vg276.musapp.utils.thenNull
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.*
import kotlin.collections.ArrayList

data class RequestParam(
    val key: String,
    val value: Any
)

data class AuthInfo (
    val access_token: String,
    val expires_in: Int,
    val user_id: Int,
    val secret: String,
    val https_required: String
)

data class AuthInfoUpdated(
    val token: String,
    val secret: String
)

data class Response(
    val response: AuthInfoUpdated
)

class VKRequests
{
    companion object
    {
        val shared = VKRequests()
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String
    {
        val id = Secure.getString(MainApplication.getInstance().contentResolver, Secure.ANDROID_ID)
        val uuid = UUID.nameUUIDFromBytes(id.toByteArray())
        return uuid?.toString() ?: ""
    }

    fun doAuth(login: String,
               password: String,
               completionHandler: (info: AuthInfo?, result: RequestResult) -> Unit)
    {
        val parameters = ArrayList<RequestParam>()
        parameters.add(RequestParam("grant_type", "password"))
        parameters.add(RequestParam("scope", "nohttps,audio"))
        parameters.add(RequestParam("client_id", CLIENT_ID))
        parameters.add(RequestParam("client_secret", CLIENT_SECRET))
        parameters.add(RequestParam("validate_token", "true"))
        parameters.add(RequestParam("username", login))
        parameters.add(RequestParam("password", password))

        RequestSessionTask("https://oauth.vk.com/token", parameters) { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        try {
                            val obj = Gson().fromJson(String(it), AuthInfo::class.java)
                            completionHandler(obj, RequestResult.Success)
                        } catch (exception: JsonSyntaxException) {
                            printLog("VKRequests - doAuth: ${exception.localizedMessage}")
                            completionHandler(null, RequestResult.ErrorRequest)
                        }
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    fun refreshToken(token: String,
                     secret: String,
                     completionHandler: (response: Response?, result: RequestResult) -> Unit)
    {
        val method = "/method/auth.refreshToken?access_token=$token&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"

        val hash = "$method$secret".md5()

        RequestSessionTask("https://api.vk.com$method&sig=$hash") { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        try {
                            val obj = Gson().fromJson(String(it), Response::class.java)
                            completionHandler(obj, RequestResult.Success)
                        } catch (exception: JsonSyntaxException) {
                            printLog("VKRequests - refreshToken: ${exception.localizedMessage}")
                            completionHandler(null, RequestResult.ErrorRequest)
                        }
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    fun getAudioList(token: String,
                     secret: String,
                     userId: Int,
                     count: Int,
                     offset: Int,
                     completionHandler: (list: ArrayList<AudioModel>?, result: RequestResult) -> Unit)
    {
        val method = "/method/audio.get?access_token=$token&owner_id=$userId&count=$count&offset=$offset&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"
        val hash = "$method$secret".md5()

        RequestSessionTask("https://api.vk.com$method&sig=$hash") { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        val list = createAudioList(it)
                        completionHandler(list, RequestResult.Success)
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    fun searchAudio(token: String,
                    secret: String,
                    q: String,
                    count: Int,
                    offset: Int,
                    completionHandler: (list: ArrayList<AudioModel>?, result: RequestResult) -> Unit)
    {
        val method = "/method/audio.search?access_token=$token&q=${q.encoded()}&count=$count&offset=$offset&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"
        val methodForHash = "/method/audio.search?access_token=$token&q=$q&count=$count&offset=$offset&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"
        val hash = "$methodForHash$secret".md5()

        RequestSessionTask("https://api.vk.com$method&sig=$hash") { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        val list = createAudioList(it)
                        completionHandler(list, RequestResult.Success)
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    fun receiveAudioArtist(token: String,
                           secret: String,
                           artistId: String,
                           count: Int,
                           offset: Int,
                           completionHandler: (list: ArrayList<AudioModel>?, result: RequestResult) -> Unit)
    {
        val method = "/method/audio.getAudiosByArtist?access_token=$token&artist_id=$artistId&count=$count&offset=$offset&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"
        val hash = "$method$secret".md5()

        RequestSessionTask("https://api.vk.com$method&sig=$hash") { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        val list = createAudioList(it)
                        completionHandler(list, RequestResult.Success)
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    fun receiveAlbumArtist(token: String,
                           secret: String,
                           artistId: String,
                           count: Int,
                           offset: Int,
                           completionHandler: (list: ArrayList<AlbumModel>?, result: RequestResult) -> Unit)
    {
        val method = "/method/audio.getAlbumsByArtist?access_token=$token&artist_id=$artistId&count=$count&offset=$offset&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"
        val hash = "$method$secret".md5()

        RequestSessionTask("https://api.vk.com$method&sig=$hash") { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        val list = createAlbumList(it)
                        completionHandler(list, RequestResult.Success)
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    fun getAudioFromAlbum(token: String,
                          secret: String,
                          ownerId: Int,
                          accessKey: String,
                          albumId: String,
                          completionHandler: (list: ArrayList<AudioModel>?, result: RequestResult) -> Unit)
    {
        val method = "/method/audio.get?access_token=$token&owner_id=$ownerId&album_id=$albumId&access_key=$accessKey&v=5.95&https=1&need_blocks=0&lang=ru&device_id=${getDeviceId()}"
        val hash = "$method$secret".md5()

        RequestSessionTask("https://api.vk.com$method&sig=$hash") { responseCode, data ->
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    data?.let {
                        val list = createAudioList(it)
                        completionHandler(list, RequestResult.Success)
                    }.thenNull {
                        completionHandler(null, RequestResult.ErrorRequest)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    completionHandler(null, RequestResult.ErrorAuth)
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    completionHandler(null, RequestResult.ErrorAvailable)
                }
                else -> {
                    completionHandler(null, RequestResult.ErrorInternet)
                }
            }
        }.execute()
    }

    /*
     * List
     */

    private fun createAudioList(data: ByteArray): ArrayList<AudioModel>
    {
        val list = ArrayList<AudioModel>()
        try {
            val root = JSONObject(String(data))
            val response = root.getJSONObject("response")
            if (response.getInt("count") > 0)
            {
                val items = response.getJSONArray("items")
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)

                    val model = AudioModel().apply {
                        audioId = item.getInt("id").toString()
                        audioOwnerId = item.getInt("owner_id").toString()
                        artist = item.getString("artist")
                        title = item.getString("title")
                        streamUrl = item.getString("url")
                        duration = item.getInt("duration")
                        isExplicit = item.getBoolean("is_explicit")
                    }

                    if (item.has("album"))
                    {
                        val album = item.getJSONObject("album")

                        model.albumId = album.getInt("id").toString()
                        model.albumTitle = album.getString("title")
                        model.albumOwnerId = album.getInt("owner_id").toString()
                        model.albumAccessKey = album.getString("access_key")

                        if (album.has("thumb"))
                        {
                            val thumb = album.getJSONObject("thumb")
                            if (thumb.has("photo_300"))
                            {
                                model.thumb = thumb.getString("photo_300")
                            }
                        }
                    }

                    val artists = ArrayList<ArtistModel>()

                    if (item.has("main_artists"))
                    {
                        val mainArtists = item.getJSONArray("main_artists")
                        for (j in 0 until mainArtists.length()) {
                            val a = mainArtists.getJSONObject(j)

                            val artistModel = ArtistModel().apply {
                                id = a.getString("id")
                                domain = a.getString("domain")
                                name = a.getString("name")
                                featured = false
                            }
                            artists.add(artistModel)
                        }
                    }

                    if (item.has("featured_artists"))
                    {
                        val mainArtists = item.getJSONArray("featured_artists")
                        for (j in 0 until mainArtists.length()) {
                            val a = mainArtists.getJSONObject(j)

                            val artistModel = ArtistModel().apply {
                                id = a.getString("id")
                                domain = a.getString("domain")
                                name = a.getString("name")
                                featured = true
                            }
                            artists.add(artistModel)
                        }
                    }

                    model.artists = artists
                    model.isAddedToLibrary = DBController.shared.isAddedAudioToLibrary(model.audioId)

                    list.add(model)
                }
            }
        } catch (exception: JSONException) {
            list.clear()
            printLog("VKRequests - getAudioList: ${exception.localizedMessage}")
        }
        return list
    }

    private fun createAlbumList(data: ByteArray): ArrayList<AlbumModel>
    {
        val list = ArrayList<AlbumModel>()
        try {
            val root = JSONObject(String(data))
            val response = root.getJSONObject("response")
            if (response.getInt("count") > 0)
            {
                val items = response.getJSONArray("items")
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val model = AlbumModel().apply {
                        albumId = item.getInt("id").toString()
                        title = item.getString("title")
                        description = item.getString("description")
                        countTracks = item.getInt("count")
                        createTime = item.getInt("create_time")
                        updateTime = item.getInt("update_time")
                        year = item.getInt("year")
                        ownerId = item.getInt("owner_id")
                        accessKey = item.getString("access_key")
                        isExplicit = item.getBoolean("is_explicit")
                    }

                    if (item.has("photo"))
                    {
                        val photo = item.getJSONObject("photo")
                        if (photo.has("photo_300"))
                        {
                            model.thumb = photo.getString("photo_300")
                        }
                    }

                    list.add(model)
                }
            }
        } catch (exception: JSONException) {
            list.clear()
            printLog("VKRequests - receiveAlbumArtist: ${exception.localizedMessage}")
        }
        return list
    }
}