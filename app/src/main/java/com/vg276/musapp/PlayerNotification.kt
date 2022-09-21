package com.vg276.musapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.exoplayer2.Player

interface PlayerNotificationListener {
    fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean)
    fun onNotificationCancelled(notificationId: Int)
}

interface PlayerNotificationInfo {
    fun onContentArtist(): String
    fun onContentTitle(): String
    fun onTrackAdded(): Boolean
    fun onReceivedAction(action: String)
}

class PlayerNotification(private val context: Context,
                         private val player: Player,
                         private val listener: PlayerNotificationListener,
                         private val info: PlayerNotificationInfo)
{
    companion object
    {
        private const val MSG_POST_NOTIFICATION = 0

        private const val NOTIFICATION_ID = 101276
        private const val NOTIFICATION_CHANNEL_ID = "musapp_playback_channel"
        private const val MEDIA_SESSION_TAG = "musapp_playback_session"

        const val ACTION_DISMISS = "com.vg276.musapp.ACTION_DISMISS"
        const val ACTION_NEXT = "com.vg276.musapp.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.vg276.musapp.ACTION_PREVIOUS"
        const val ACTION_PLAY = "com.vg276.musapp.ACTION_PLAY"
        const val ACTION_PAUSE = "com.vg276.musapp.ACTION_PAUSE"
        const val ACTION_ADD = "com.vg276.musapp.ACTION_ADD"
        const val ACTION_ADDED = "com.vg276.musapp.ACTION_ADDED"

        private val intentFilter = IntentFilter().apply {
            addAction(ACTION_DISMISS)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_ADD)
            addAction(ACTION_ADDED)
        }
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val mediaSession = MediaSessionCompat(context, MEDIA_SESSION_TAG)
    private val mainHandler = Handler(Looper.getMainLooper(), this::handleMessage)

    private val dismissPendingIntent = createActionBroadcast(context, 100, ACTION_DISMISS)
    private val previousPendingIntent = createActionBroadcast(context, 101, ACTION_PREVIOUS)
    private val nextPendingIntent = createActionBroadcast(context, 102, ACTION_NEXT)
    private val pausePendingIntent = createActionBroadcast(context, 103, ACTION_PAUSE)
    private val playPendingIntent = createActionBroadcast(context, 104, ACTION_PLAY)
    private val addPendingIntent = createActionBroadcast(context, 105, ACTION_ADD)
    private val addedPendingIntent = createActionBroadcast(context, 106, ACTION_ADDED)

    init {
        notificationManager.cancelAll()
    }

    fun release()
    {
        context.unregisterReceiver(actionsReceiver)
        mainHandler.removeMessages(MSG_POST_NOTIFICATION)

        mediaSession.release()
        player.removeListener(playerListener)

        notificationManager.cancel(NOTIFICATION_ID)
        listener.onNotificationCancelled(NOTIFICATION_ID)
    }

    fun createNotification()
    {
        // media session
        createMediaSession()

        // channel notification
        createNotificationChannel()

        // receiver actions
        context.registerReceiver(actionsReceiver, intentFilter)

        // player listener
        player.addListener(playerListener)
    }

    fun postNotification()
    {
        if (!mainHandler.hasMessages(MSG_POST_NOTIFICATION)) {
            mainHandler.sendEmptyMessage(MSG_POST_NOTIFICATION)
        }
    }

    private fun createMediaSession()
    {
        mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
        mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
        mediaSession.isActive = true
    }

    private fun updateSession()
    {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration)
                .build()
        )

        val state = when(isPauseAction()) {
            true -> PlaybackStateCompat.STATE_PLAYING
            false -> PlaybackStateCompat.STATE_PAUSED
        }

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, player.contentPosition, 1f,  SystemClock.elapsedRealtime())
                .build()
        )
    }

    private fun createNotification(ongoing: Boolean): Notification
    {
        // update meta data
        updateSession()

        // builder notification
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        // actions
        builder.addAction(R.drawable.noti_previous, "Previous", previousPendingIntent)

        if (isPauseAction())
            builder.addAction(R.drawable.noti_pause, "Pause", pausePendingIntent)
        else
            builder.addAction(R.drawable.noti_play, "Play", playPendingIntent)

        builder.addAction(R.drawable.noti_next, "Next", nextPendingIntent)

        if (info.onTrackAdded())
            builder.addAction(R.drawable.noti_added, "Added", addedPendingIntent)
        else
            builder.addAction(R.drawable.noti_add, "Add", addPendingIntent)

        // style notification
        val style = androidx.media.app.NotificationCompat.MediaStyle()
        style.setMediaSession(mediaSession.sessionToken)
        style.setShowActionsInCompactView(0,1,2)
        style.setShowCancelButton(!ongoing)
        style.setCancelButtonIntent(dismissPendingIntent)

        // notification
        builder
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDeleteIntent(dismissPendingIntent)
            .setContentTitle(info.onContentArtist())
            .setContentText(info.onContentTitle())
            .setContentIntent(currentContent())
            .setSmallIcon(R.drawable.noti_icon)
            .setStyle(style)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)

        return builder.build()
    }

    private fun createNotificationChannel()
    {
        val priority = NotificationManager.IMPORTANCE_HIGH
        val name = context.getString(R.string.app_notification_name)
        val description = context.getString(R.string.app_notification_description)
        val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, priority)

        notificationChannel.description = description
        notificationChannel.setShowBadge(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun createActionBroadcast(context: Context,
                                      instanceId: Int, action: String): PendingIntent
    {
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }

        val intent = Intent(action).setPackage(context.packageName)

        return PendingIntent.getBroadcast(
            context,
            instanceId,
            intent,
            pendingFlags
        )
    }

    private fun currentContent(): PendingIntent
    {
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(INTENT_TYPE, INTENT_TYPE_PLAYER)
        }

        return PendingIntent.getActivity(
            context,
            PENDING_INTENT_CONTENT,
            intent,
            pendingFlags
        )
    }

    private fun isOngoing(): Boolean {
        val playbackState = player.playbackState
        return ((playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
                && player.playWhenReady)
    }

    private fun isPauseAction(): Boolean {
        return  player.playbackState != Player.STATE_ENDED &&
                player.playbackState != Player.STATE_IDLE &&
                player.playWhenReady
    }

    private fun updateNotification()
    {
        val ongoing = isOngoing()
        val notification = createNotification(ongoing)

        notificationManager.notify(NOTIFICATION_ID, notification)
        listener.onNotificationPosted(NOTIFICATION_ID, notification, ongoing)
    }

    private fun handleMessage(msg: Message): Boolean
    {
        return if (msg.what == MSG_POST_NOTIFICATION) {
            updateNotification()
            true
        } else {
            false
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                postNotification()
            }
        }
    }

    private val actionsReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.action?.let {
                info.onReceivedAction(it)

                if (it == ACTION_DISMISS)
                    release()
            }
        }
    }
}