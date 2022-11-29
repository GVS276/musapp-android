package com.vg276.musapp

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.util.MimeTypes
import com.vg276.musapp.db.DBController
import com.vg276.musapp.db.IDBDelegate
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.utils.isRandomAudio
import com.vg276.musapp.utils.isRepeatAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

interface PlayerPlaybackListener {
    fun onPlaybackProgress(ms: Long)
}

class PlayerService: LifecycleService()
{
    inner class MainServiceBinder: Binder() {
        val service get() = this@PlayerService
    }

    companion object
    {
        private const val PLAYED_MODEL = "playedModel"
        private const val PLAYED_LIST = "playedList"

        fun bindIntent(context: Context): Intent = Intent(context, PlayerService::class.java)

        fun newIntent(context: Context, model: AudioModel, list: ArrayList<AudioModel>): Intent
        {
            return Intent(context, PlayerService::class.java).apply {
                putExtra(PLAYED_MODEL, model)
                putExtra(PLAYED_LIST, list)
            }
        }
    }

    private val db = DBController.shared

    private lateinit var player: ExoPlayer
    private val playerList: MutableList<AudioModel> = ArrayList()

    private var playerNotification: PlayerNotification? = null
    private var playerPlaybackTimer: Timer? = null
    private var playerPlaybackListener: PlayerPlaybackListener? = null

    private val _playerCommand = MutableLiveData<PlayerCommand>()
    val playerCommand: LiveData<PlayerCommand> get() = _playerCommand

    private val _playedModel = MutableLiveData<AudioModel>()
    val playedModel: LiveData<AudioModel> get() = _playedModel

    /*private val _playbackTime = MutableLiveData<Long>()
    val playbackTime: LiveData<Long> get() = _playbackTime*/

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(applicationContext).build()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, true)
        player.setHandleAudioBecomingNoisy(true)
        player.addListener(PlayerEventListener())

        db.addDelegate(libraryDelegates)
        initNotification()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return MainServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        handleIntent(intent)
        return START_NOT_STICKY // чтобы не перезапускался после убийства
    }

    override fun onDestroy()
    {
        removePlaybackListener()
        releasePlayerTimer()

        db.removeDelegate(libraryDelegates)
        playerNotification?.release()
        player.release()

        super.onDestroy()
    }

    private fun initNotification()
    {
        playerNotification = PlayerNotification(
            applicationContext, player, notificationListener, notificationInfo
        )
        playerNotification?.createNotification()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                it.getSerializableExtra(PLAYED_MODEL, AudioModel::class.java)?.let { model ->
                    startStream(model)
                }

                it.getParcelableArrayListExtra(PLAYED_LIST, AudioModel::class.java)?.let { list ->
                    setPlayerList(list)
                }
            } else {
                (it.getSerializableExtra(PLAYED_MODEL) as? AudioModel)?.let { model ->
                    startStream(model)
                }

                (it.getSerializableExtra(PLAYED_LIST) as? ArrayList<AudioModel>)?.let { list ->
                    setPlayerList(list)
                }
            }
        }
    }

    private fun startStream(model: AudioModel)
    {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(model.streamUrl))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        player.setMediaItem(mediaItem, 0)
        player.prepare()
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.playWhenReady = true

        _playedModel.postValue(model)
    }

    private fun setPlayerList(list: ArrayList<AudioModel>)
    {
        if (!playerList.containsAll(list))
        {
            playerList.clear()
            playerList.addAll(list)
        }
    }

    fun play()
    {
        player.play()
    }

    fun pause()
    {
        player.pause()
    }

    fun playOrPause()
    {
        if (player.isPlaying)
            pause()
        else
            play()
    }

    fun seek(ms: Long)
    {
        player.seekTo(ms)
    }

    fun setPlaybackListener(listener: PlayerPlaybackListener)
    {
        playerPlaybackListener = listener
    }

    fun removePlaybackListener()
    {
        playerPlaybackListener = null
    }

    fun trackRandom()
    {
        val randomIndex = (0 until playerList.size).random()
        val audio = playerList[randomIndex]
        startStream(audio)
    }

    fun trackNext()
    {
        val index = playerList.indexOfFirst { it.audioId == playedModel.value?.audioId }
        if (index != -1)
        {
            val next = if (index + 1 > playerList.size - 1) 0 else index + 1
            val audio = playerList[next]
            startStream(audio)
        }
    }

    fun trackPrevious()
    {
        val index = playerList.indexOfFirst { it.audioId == playedModel.value?.audioId }
        if (index != -1)
        {
            val previous = if (index - 1 < 0) playerList.size - 1 else index - 1
            val audio = playerList[previous]
            startStream(audio)
        }
    }


    private fun trackAddedToLibrary(audioId: String, value: Boolean)
    {
        // update played model
        if (playedModel.value?.audioId == audioId)
            playedModel.value?.isAddedToLibrary = value

        // update model from list
        val index = playerList.indexOfFirst { model -> model.audioId == audioId }
        if (index != -1)
            playerList[index].isAddedToLibrary = value

        // update notification
        playerNotification?.postNotification()
    }

    private fun addTrackToLibrary(value: Boolean)
    {
        playedModel.value?.let {
            if (value)
                db.addAudio(it)
            else
                db.deleteAudio(it.audioId)
        }
    }

    private fun startPlayerTimer()
    {
        if (playerPlaybackTimer != null)
            return

        playerPlaybackTimer = Timer()
        playerPlaybackTimer?.scheduleAtFixedRate(timerTask {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    val ms = player.currentPosition
                    playerPlaybackListener?.onPlaybackProgress(ms)
                    //_playbackTime.postValue(ms)
                }
            }
        }, 100, 100)
    }

    private fun releasePlayerTimer() {
        playerPlaybackTimer?.cancel()
        playerPlaybackTimer = null
    }

    private inner class PlayerEventListener: Player.Listener
    {
        override fun onPlayerError(error: PlaybackException) {
            _playerCommand.postValue(PlayerCommand.Error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when(playbackState)
            {
                Player.STATE_IDLE -> _playerCommand.postValue(PlayerCommand.Idle)
                Player.STATE_BUFFERING -> _playerCommand.postValue(PlayerCommand.Buffering)
                Player.STATE_READY -> _playerCommand.postValue(PlayerCommand.Ready)
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            when {
                isPlaying -> _playerCommand.postValue(PlayerCommand.Playing)

                player.playbackState == Player.STATE_ENDED -> {
                    when {
                        isRepeatAudio(applicationContext) -> {
                            seek(0)
                            play()
                        }

                        isRandomAudio(applicationContext) -> {
                            trackRandom()
                        }

                        else -> {
                            trackNext()
                        }
                    }
                }

                else -> _playerCommand.postValue(PlayerCommand.Paused)
            }

            if (isPlaying)
                startPlayerTimer()
            else
                releasePlayerTimer()
        }
    }

    private val notificationListener = object : PlayerNotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing) {
                startForeground(notificationId, notification)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(false)
                }
            }
        }

        override fun onNotificationCancelled(notificationId: Int) {
            stopSelf()
        }
    }

    private val notificationInfo = object : PlayerNotificationInfo {
        override fun onContentArtist(): String {
            return playedModel.value?.artist ?: "Artist"
        }

        override fun onContentTitle(): String {
            return playedModel.value?.title ?: "Title"
        }

        override fun onContentAlbum(): String {
            return playedModel.value?.albumTitle ?: "Album"
        }

        override fun onTrackAdded(): Boolean {
            return playedModel.value?.isAddedToLibrary ?: false
        }

        override fun onReceivedAction(action: String) {
            when(action)
            {
                PlayerNotification.ACTION_PREVIOUS -> trackPrevious()
                PlayerNotification.ACTION_NEXT -> trackNext()
                PlayerNotification.ACTION_PLAY -> play()
                PlayerNotification.ACTION_PAUSE -> pause()
                PlayerNotification.ACTION_ADD -> addTrackToLibrary(true)
                PlayerNotification.ACTION_ADDED -> addTrackToLibrary(false)
                else -> {}
            }
        }
    }

    private val libraryDelegates = object : IDBDelegate
    {
        override fun onAudioList(requestIdentifier: Long, list: ArrayList<AudioModel>?) {}
        override fun onAudioAdded(requestIdentifier: Long, model: AudioModel?) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    model?.let {
                        trackAddedToLibrary(it.audioId, true)
                    }
                }
            }
        }
        override fun onAudioDeleted(requestIdentifier: Long, audioId: String) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    trackAddedToLibrary(audioId, false)
                }
            }
        }
    }
}