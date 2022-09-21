package com.vg276.musapp.base

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vg276.musapp.*
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.models.AudioPlayerModel
import com.vg276.musapp.utils.*
import kotlin.math.abs
import kotlin.math.min

abstract class BaseActivity: AppCompatActivity()
{
    val audioPlayer: AudioPlayerModel by viewModels()
    var playerService: PlayerService? = null

    private lateinit var player: ConstraintLayout
    private lateinit var playerContent: ConstraintLayout
    private lateinit var playerSubtract: AppCompatImageView
    private lateinit var playerThumb: AppCompatImageView
    private lateinit var playerArtist: AppCompatTextView
    private lateinit var playerTitle: AppCompatTextView
    private lateinit var playerTime: AppCompatTextView
    private lateinit var playerTotalTime: AppCompatTextView
    private lateinit var playerSeekBar: SeekBar
    private lateinit var playerButtonPlayOrPause: AppCompatImageButton
    private lateinit var playerButtonPrevious: AppCompatImageButton
    private lateinit var playerButtonNext: AppCompatImageButton
    private lateinit var playerButtonRandom: AppCompatImageButton
    private lateinit var playerButtonRepeat: AppCompatImageButton

    private lateinit var playerPeek: ConstraintLayout
    private lateinit var playerPeekArtist: AppCompatTextView
    private lateinit var playerPeekTitle: AppCompatTextView
    private lateinit var playerPeekButtonPlayOrPause: AppCompatImageButton

    private lateinit var playerOverlay: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    protected fun initPlayer(view: View)
    {
        // full
        player = view.findViewById(R.id.player)
        playerContent = player.findViewById(R.id.contentPlayer)
        playerContent.alpha = 0f

        playerSubtract = player.findViewById(R.id.subtract)
        playerThumb = player.findViewById(R.id.thumb)
        playerArtist = player.findViewById(R.id.artist)
        playerTitle = player.findViewById(R.id.title)

        playerTime = player.findViewById(R.id.currentDuration)
        playerTotalTime = player.findViewById(R.id.totalDuration)
        playerSeekBar = player.findViewById(R.id.audioSeekBar)

        playerButtonPlayOrPause = player.findViewById(R.id.playOrPause)
        playerButtonPrevious = player.findViewById(R.id.previous)
        playerButtonNext = player.findViewById(R.id.next)

        playerButtonRandom = player.findViewById(R.id.random)
        playerButtonRandom.setImageResource(when(isRandomAudio(baseContext)) {
            true -> R.drawable.random_on
            false -> R.drawable.random
        })

        playerButtonRepeat = player.findViewById(R.id.repeat)
        playerButtonRepeat.setImageResource(when(isRepeatAudio(baseContext)) {
            true -> R.drawable.repeat_on
            false -> R.drawable.repeat
        })

        // peek
        playerPeek = player.findViewById(R.id.peekPlayer)
        playerPeek.alpha = 1f

        playerPeekArtist = player.findViewById(R.id.peekArtist)
        playerPeekTitle = player.findViewById(R.id.peekTitle)
        playerPeekButtonPlayOrPause = player.findViewById(R.id.peekPlayOrPause)

        // sheet
        bottomSheetBehavior = BottomSheetBehavior.from(player)
        bottomSheetBehavior.apply {
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            if (playerPeek.visibility != View.GONE)
                            {
                                playerPeek.visibility = View.GONE
                                startProgress()
                            }
                        }
                        else -> {
                            if (playerPeek.visibility != View.VISIBLE)
                            {
                                playerPeek.visibility = View.VISIBLE
                                removeProgress()
                            }
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    playerPeek.alpha = 1 - slideOffset
                    playerContent.alpha = min(1f, if (slideOffset >= 0.1) slideOffset else 0f)
                }
            })
        }

        // overlay
        playerOverlay = view.findViewById(R.id.playerOverlay)
        playerOverlay.applyWindowInsets { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // overlay
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom
            }

            // peek player
            if (this::bottomSheetBehavior.isInitialized)
            {
                val h = resources.getDimensionPixelSize(R.dimen.peek_player_size)
                bottomSheetBehavior.peekHeight = h + bars.bottom
            }

            // content player
            if (this::playerContent.isInitialized)
            {
                playerContent.updatePadding(bottom = bars.bottom)
            }

            if (this::playerSubtract.isInitialized)
            {
                playerSubtract.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    val sb = bars.top + 10
                    val nb = bars.bottom
                    topMargin = if (nb >= sb) nb else sb
                }
            }

            insets
        }

        initPlayerListeners()
    }

    fun showPlayerSheet()
    {
        if (this::bottomSheetBehavior.isInitialized)
        {
            bottomSheetBehavior.apply {
                if (state != BottomSheetBehavior.STATE_EXPANDED)
                {
                    state = BottomSheetBehavior.STATE_EXPANDED

                    playerPeek.visibility = View.GONE
                    playerPeek.alpha = 0f
                    playerContent.alpha = 1f

                    startProgress()
                }
            }
        }
    }

    fun hidePlayerSheet()
    {
        if (this::bottomSheetBehavior.isInitialized)
        {
            bottomSheetBehavior.apply {
                if (state == BottomSheetBehavior.STATE_EXPANDED)
                {
                    state = BottomSheetBehavior.STATE_COLLAPSED

                    playerPeek.visibility = View.VISIBLE
                    playerPeek.alpha = 1f
                    playerContent.alpha = 0f

                    removeProgress()
                }
            }
        }
    }

    fun isPlayerSheet(): Boolean
    {
        if (this::bottomSheetBehavior.isInitialized)
        {
            return bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
        }
        return false
    }

    private fun setAudioButtonsRes(res: Int)
    {
        playerPeekButtonPlayOrPause.setImageResource(res)
        playerButtonPlayOrPause.setImageResource(res)
    }

    private fun setAudioProgress(ms: Long)
    {
        if (this::playerSeekBar.isInitialized)
        {
            val correct = abs(ms / 1000L)
            playerSeekBar.progress = correct.toInt()
        }
    }

    private fun repeatAudio(value: Boolean)
    {
        val settings = SettingsPreferences(baseContext)
        settings.put(KEY_REPEAT, value)

        playerButtonRepeat.setImageResource(when(value) {
            true -> R.drawable.repeat_on
            false -> R.drawable.repeat
        })
    }

    private fun randomAudio(value: Boolean)
    {
        val settings = SettingsPreferences(baseContext)
        settings.put(KEY_RANDOM, value)

        playerButtonRandom.setImageResource(when(value) {
            true -> R.drawable.random_on
            false -> R.drawable.random
        })
    }

    private fun setPlayed(model: AudioModel)
    {
        if (!this::playerArtist.isInitialized)
        {
            return
        }

        if (playerOverlay.visibility == View.GONE)
        {
            playerOverlay.visibility = View.VISIBLE
            player.visibility = View.VISIBLE
        }

        // ui
        playerArtist.text = model.artist
        playerTitle.text = model.title

        playerPeekArtist.text = model.artist
        playerPeekTitle.text = model.title

        playerSeekBar.progress = 0
        playerSeekBar.max = model.duration
        playerTotalTime.text = model.duration.toTime()

        if (model.isExplicit)
        {
            playerTitle.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.action_explicit,
                0,
                0,
                0)

            playerPeekTitle.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.action_explicit,
                0,
                0,
                0)
        }
        else
        {
            playerTitle.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0)

            playerPeekTitle.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0)
        }

        // viewModel
        audioPlayer.playedId = model.audioId
        audioPlayer.setPlaying(true)
    }

    private fun initPlayerListeners()
    {
        // clicks
        playerPeek.setOnClickListener {
            showPlayerSheet()
        }

        playerPeekButtonPlayOrPause.setOnClickListener {
            playerService?.playOrPause()
        }

        playerButtonPlayOrPause.setOnClickListener {
            playerService?.playOrPause()
        }

        playerButtonPrevious.setOnClickListener {
            playerService?.trackPrevious()
        }

        playerButtonNext.setOnClickListener {
            playerService?.trackNext()
        }

        playerButtonRandom.setOnClickListener {
            val value = isRandomAudio(baseContext)
            randomAudio(!value)
        }

        playerButtonRepeat.setOnClickListener {
            val value = isRepeatAudio(baseContext)
            repeatAudio(!value)
        }

        // listener
        playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                p0?.let {
                    playerTime.text = it.progress.toTime()
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                playerService?.pause()
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                p0?.let {
                    playerService?.seek(it.progress * 1000L)
                    playerService?.play()
                }
            }
        })
    }

    fun startStream(model: AudioModel, list: ArrayList<AudioModel>)
    {
        PlayerService.newIntent(this, model, list).also { intent ->
            startService(intent)
            if (playerService == null) {
                bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun bindService()
    {
        if (playerService == null) {
            bindService(PlayerService.bindIntent(this), playerServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun playOrPause()
    {
        playerService?.playOrPause()
    }

    fun stopPlayerService()
    {
        if (playerService != null) {
            unbindService(playerServiceConnection)
        }

        stopService(Intent(this, PlayerService::class.java))

        playerService = null
    }

    private fun startProgress()
    {
        playerService?.setPlaybackListener(playbackListener)
    }

    private fun removeProgress()
    {
        playerService?.removePlaybackListener()
    }

    private val playerServiceConnection = object : ServiceConnection
    {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as PlayerService.MainServiceBinder

            playerService = binder.service
            playerService?.playerCommand?.observe(this@BaseActivity) {
                when(it)
                {
                    PlayerCommand.Error -> {

                    }
                    PlayerCommand.Idle -> {

                    }
                    PlayerCommand.Buffering -> {

                    }
                    PlayerCommand.Ready -> {

                    }
                    PlayerCommand.Playing -> {
                        setAudioButtonsRes(R.drawable.pause)
                        audioPlayer.setPlaying(true)
                    }
                    PlayerCommand.Paused -> {
                        setAudioButtonsRes(R.drawable.play)
                        audioPlayer.setPlaying(false)
                    }
                    PlayerCommand.Finished -> {
                        playerService?.trackNext()
                    }
                    else -> {}
                }
            }

            playerService?.playedModel?.observe(this@BaseActivity) {
                setPlayed(it)
            }

            if (isPlayerSheet())
                startProgress()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            playerService = null
        }
    }

    private val playbackListener = object : PlayerPlaybackListener
    {
        override fun onPlaybackProgress(ms: Long) {
            setAudioProgress(ms)
        }
    }
}