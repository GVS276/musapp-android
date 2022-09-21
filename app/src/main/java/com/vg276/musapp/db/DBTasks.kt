package com.vg276.musapp.db

import com.vg276.musapp.base.BaseTask
import com.vg276.musapp.db.model.AudioModel
import java.util.*

fun timestamp(): Long
{
    val now = Calendar.getInstance()
    return now.timeInMillis
}

class AllAudioTask: BaseTask()
{
    override fun run() {
        val list = SQLDataBase.getInstance().getAudioDao().getAllAudio()
        DBDelegate.onAudioListResult(requestIdentifier, list)
    }
}

class AddAudioTask(private var model: AudioModel): BaseTask()
{
    override fun run() {
        model.timestamp = timestamp()
        model.isAddedToLibrary = true
        SQLDataBase.getInstance().getAudioDao().insertAudio(model)
        DBDelegate.onAudioAddedResult(requestIdentifier, model)
    }
}

class DeleteAudioTask(private var audioId: String): BaseTask()
{
    override fun run() {
        SQLDataBase.getInstance().getAudioDao().deleteAudioById(audioId)
        DBDelegate.onAudioDeletedResult(requestIdentifier, audioId)
    }
}

class GetAudioByIdTask(private var audioId: String,
                       private val callback: (model: AudioModel?) -> Unit): BaseTask()
{
    override fun run() {
        val model = SQLDataBase.getInstance().getAudioDao().getAudioById(audioId)
        callback(model)
    }
}