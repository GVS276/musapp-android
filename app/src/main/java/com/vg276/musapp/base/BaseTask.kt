package com.vg276.musapp.base

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface IBaseTask: Runnable
{
    fun execute(): Long
    override fun run()
}

abstract class BaseTask: IBaseTask
{
    val requestIdentifier: Long = System.nanoTime()

    override fun execute(): Long
    {
        executorService.execute(this)
        return requestIdentifier
    }

    companion object
    {
        val executorService = ThreadPoolExecutor(0,
            1,
            10,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(),
            Executors.defaultThreadFactory()
        )
    }
}