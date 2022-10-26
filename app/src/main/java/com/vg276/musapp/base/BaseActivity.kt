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
import androidx.core.view.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vg276.musapp.*
import com.vg276.musapp.databinding.ActivityMainBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.models.AudioPlayerModel
import com.vg276.musapp.utils.*
import kotlin.math.abs
import kotlin.math.min

abstract class BaseActivity: AppCompatActivity()
{
    val audioPlayer: AudioPlayerModel by viewModels()
    var playerService: PlayerService? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    protected fun initPlayer()
    {
        // init
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // settings
        binding.includePlayerSheet.includeContentPlayer.contentPlayer.alpha = 0f
        binding.includePlayerSheet.includePeekPlayer.peekPlayer.alpha = 1f

        binding.includePlayerSheet.includeContentPlayer.random.setImageResource(when(isRandomAudio(baseContext)) {
            true -> R.drawable.random_on
            false -> R.drawable.random
        })

        binding.includePlayerSheet.includeContentPlayer.repeat.setImageResource(when(isRepeatAudio(baseContext)) {
            true -> R.drawable.repeat_on
            false -> R.drawable.repeat
        })

        // sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.includePlayerSheet.player)
        bottomSheetBehavior.apply {
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            if (binding.includePlayerSheet.includePeekPlayer.peekPlayer.visibility != View.GONE)
                            {
                                binding.includePlayerSheet.includePeekPlayer.peekPlayer.visibility = View.GONE
                                startProgress()
                            }
                        }
                        else -> {
                            if (binding.includePlayerSheet.includePeekPlayer.peekPlayer.visibility != View.VISIBLE)
                            {
                                binding.includePlayerSheet.includePeekPlayer.peekPlayer.visibility = View.VISIBLE
                                removeProgress()
                            }
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.includePlayerSheet.includePeekPlayer.peekPlayer.alpha = 1 - slideOffset
                    binding.includePlayerSheet.includeContentPlayer.contentPlayer.alpha = min(1f, if (slideOffset >= 0.1) slideOffset else 0f)
                }
            })
        }

        // overlay
        binding.playerOverlay.applyWindowInsets { v, insets ->
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
            binding.includePlayerSheet.includeContentPlayer.contentPlayer.updatePadding(bottom = bars.bottom)
            binding.includePlayerSheet.includeContentPlayer.subtract.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val sb = bars.top + 10
                val nb = bars.bottom
                topMargin = if (nb >= sb) nb else sb
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

                    binding.includePlayerSheet.includePeekPlayer.peekPlayer.visibility = View.GONE
                    binding.includePlayerSheet.includePeekPlayer.peekPlayer.alpha = 0f
                    binding.includePlayerSheet.includeContentPlayer.contentPlayer.alpha = 1f

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

                    binding.includePlayerSheet.includePeekPlayer.peekPlayer.visibility = View.VISIBLE
                    binding.includePlayerSheet.includePeekPlayer.peekPlayer.alpha = 1f
                    binding.includePlayerSheet.includeContentPlayer.contentPlayer.alpha = 0f

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
        binding.includePlayerSheet.includePeekPlayer.peekPlayOrPause.setImageResource(res)
        binding.includePlayerSheet.includeContentPlayer.playOrPause.setImageResource(res)
    }

    private fun setAudioProgress(ms: Long)
    {
        val correct = abs(ms / 1000L)
        binding.includePlayerSheet.includeContentPlayer.audioSeekBar.progress = correct.toInt()
    }

    private fun repeatAudio(value: Boolean)
    {
        val settings = SettingsPreferences(baseContext)
        settings.put(KEY_REPEAT, value)

        binding.includePlayerSheet.includeContentPlayer.repeat.setImageResource(when(value) {
            true -> R.drawable.repeat_on
            false -> R.drawable.repeat
        })
    }

    private fun randomAudio(value: Boolean)
    {
        val settings = SettingsPreferences(baseContext)
        settings.put(KEY_RANDOM, value)

        binding.includePlayerSheet.includeContentPlayer.random.setImageResource(when(value) {
            true -> R.drawable.random_on
            false -> R.drawable.random
        })
    }

    private fun setPlayed(model: AudioModel)
    {
        if (binding.playerOverlay.visibility == View.GONE)
        {
            binding.playerOverlay.visibility = View.VISIBLE
            binding.includePlayerSheet.player.visibility = View.VISIBLE
        }

        // ui
        binding.includePlayerSheet.includeContentPlayer.artist.text = model.artist
        binding.includePlayerSheet.includeContentPlayer.title.text = model.title

        binding.includePlayerSheet.includePeekPlayer.peekArtist.text = model.artist
        binding.includePlayerSheet.includePeekPlayer.peekTitle.text = model.title

        binding.includePlayerSheet.includeContentPlayer.audioSeekBar.progress = 0
        binding.includePlayerSheet.includeContentPlayer.audioSeekBar.max = model.duration
        binding.includePlayerSheet.includeContentPlayer.totalDuration.text = model.duration.toTime()

        if (model.isExplicit)
        {
            binding.includePlayerSheet.includeContentPlayer.title.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.action_explicit,
                0,
                0,
                0)

            binding.includePlayerSheet.includePeekPlayer.peekTitle.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.action_explicit,
                0,
                0,
                0)
        }
        else
        {
            binding.includePlayerSheet.includeContentPlayer.title.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0)

            binding.includePlayerSheet.includePeekPlayer.peekTitle.setCompoundDrawablesWithIntrinsicBounds(
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
        binding.includePlayerSheet.includePeekPlayer.peekPlayer.setOnClickListener {
            showPlayerSheet()
        }

        binding.includePlayerSheet.includePeekPlayer.peekPlayOrPause.setOnClickListener {
            playerService?.playOrPause()
        }

        binding.includePlayerSheet.includeContentPlayer.playOrPause.setOnClickListener {
            playerService?.playOrPause()
        }

        binding.includePlayerSheet.includeContentPlayer.previous.setOnClickListener {
            playerService?.trackPrevious()
        }

        binding.includePlayerSheet.includeContentPlayer.next.setOnClickListener {
            playerService?.trackNext()
        }

        binding.includePlayerSheet.includeContentPlayer.random.setOnClickListener {
            val value = isRandomAudio(baseContext)
            randomAudio(!value)
        }

        binding.includePlayerSheet.includeContentPlayer.repeat.setOnClickListener {
            val value = isRepeatAudio(baseContext)
            repeatAudio(!value)
        }

        // listener
        binding.includePlayerSheet.includeContentPlayer.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                p0?.let {
                    binding.includePlayerSheet.includeContentPlayer.currentDuration.text = it.progress.toTime()
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