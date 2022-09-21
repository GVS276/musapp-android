package com.vg276.musapp.db

import com.vg276.musapp.db.model.AudioModel
import java.util.concurrent.locks.ReentrantLock

interface IDBDelegate {
    fun onAudioList(requestIdentifier: Long, list: ArrayList<AudioModel>?)
    fun onAudioAdded(requestIdentifier: Long, model: AudioModel?)
    fun onAudioDeleted(requestIdentifier: Long, audioId: String)
}

open class DBBaseNotifier {
    private val lock = ReentrantLock(true)

    fun lock() {
        lock.lock()
    }

    fun unlock() {
        lock.unlock()
    }
}

object DBDelegate: DBBaseNotifier()
{
    private val delegates = LinkedHashSet<IDBDelegate>()

    fun addDelegate(delegate: IDBDelegate) {
        lock()
        delegates.add(delegate)
        unlock()
    }

    fun removeDelegate(delegate: IDBDelegate) {
        lock()
        delegates.remove(delegate)
        unlock()
    }

    fun onAudioListResult(requestIdentifier: Long, list: ArrayList<AudioModel>?)
    {
        lock()

        try {
            for (delegate in delegates) {
                delegate.onAudioList(requestIdentifier, list)
            }
        }
        finally {
            unlock()
        }
    }

    fun onAudioAddedResult(requestIdentifier: Long, model: AudioModel?)
    {
        lock()

        try {
            for (delegate in delegates) {
                delegate.onAudioAdded(requestIdentifier, model)
            }
        }
        finally {
            unlock()
        }
    }

    fun onAudioDeletedResult(requestIdentifier: Long, audioId: String)
    {
        lock()

        try {
            for (delegate in delegates) {
                delegate.onAudioDeleted(requestIdentifier, audioId)
            }
        }
        finally {
            unlock()
        }
    }
}