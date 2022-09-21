package com.vg276.musapp.db

import com.vg276.musapp.db.model.AudioModel

class DBController
{
    companion object
    {
        val shared = DBController()
    }

    fun addDelegate(delegate: IDBDelegate)
    {
        DBDelegate.addDelegate(delegate)
    }

    fun removeDelegate(delegate: IDBDelegate)
    {
        DBDelegate.removeDelegate(delegate)
    }

    fun receiveAudioList(): Long
    {
        return AllAudioTask().execute()
    }

    fun addAudio(model: AudioModel): Long {
        model.timestamp = timestamp()
        return AddAudioTask(model).execute()
    }

    fun deleteAudio(audioId: String): Long
    {
        return DeleteAudioTask(audioId).execute()
    }

    fun getAudioById(audioId: String, callback: (model: AudioModel?) -> Unit)
    {
        GetAudioByIdTask(audioId, callback).execute()
    }

    // No main thread
    fun isAddedAudioToLibrary(audioId: String): Boolean
    {
        return SQLDataBase.getInstance().getAudioDao().getAudioById(audioId) != null
    }
}