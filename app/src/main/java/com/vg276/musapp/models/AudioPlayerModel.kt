package com.vg276.musapp.models

import androidx.lifecycle.*

data class AudioPlaying(val audioId: String,
                        val isPlaying: Boolean)

class AudioPlayerModel: ViewModel()
{
    var playedId: String = ""

    private val _playing = MutableLiveData<AudioPlaying>()
    val playing: LiveData<AudioPlaying> get() = _playing

    fun setPlaying(value: Boolean)
    {
        _playing.postValue(AudioPlaying(playedId, value))
    }
}