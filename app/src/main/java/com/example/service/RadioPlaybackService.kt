package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.MainActivity
import com.example.models.RadioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RadioPlaybackService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val CHANNEL_ID = "radio_playback_channel"
    private val NOTIFICATION_ID = 8829

    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var currentRadioId: String? = null
    private lateinit var repository: com.example.data.DataRepository

    companion object {
        const val ACTION_START = "com.example.action.START"
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
    }

    private fun playStream(url: String) {
        val player = exoPlayer ?: return
        PlaybackStateManager.isRadioBuffering.value = true
        PlaybackStateManager.isRadioActuallyPlaying.value = false

        try {
            player.stop()
            player.clearMediaItems()
            val cleanUrl = url.trim()
            if (cleanUrl.isNotBlank()) {
                val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(cleanUrl))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = PlaybackStateManager.isPlaying.value
            } else {
                PlaybackStateManager.isRadioBuffering.value = false
            }
        } catch (e: Exception) {
            android.util.Log.e("RadioPlaybackService", "Error playing stream: $url", e)
            PlaybackStateManager.isRadioBuffering.value = false
            PlaybackStateManager.isRadioActuallyPlaying.value = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = com.example.data.DataRepository(applicationContext)
        createNotificationChannel()

        try {
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36 (MedinPlayHub/1.0)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(8000)

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(applicationContext, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

            exoPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL)
                .build().apply {
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            when (state) {
                                androidx.media3.common.Player.STATE_BUFFERING -> {
                                    PlaybackStateManager.isRadioBuffering.value = true
                                    PlaybackStateManager.isRadioActuallyPlaying.value = false
                                }
                                androidx.media3.common.Player.STATE_READY -> {
                                    PlaybackStateManager.isRadioBuffering.value = false
                                    PlaybackStateManager.isRadioActuallyPlaying.value = isPlaying
                                }
                                androidx.media3.common.Player.STATE_ENDED,
                                androidx.media3.common.Player.STATE_IDLE -> {
                                    PlaybackStateManager.isRadioBuffering.value = false
                                    PlaybackStateManager.isRadioActuallyPlaying.value = false
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            PlaybackStateManager.isRadioActuallyPlaying.value = isPlaying
                            if (isPlaying) {
                                PlaybackStateManager.isRadioBuffering.value = false
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            android.util.Log.e("RadioPlaybackService", "ExoPlayer error: ${error.message}", error)
                            PlaybackStateManager.isRadioBuffering.value = false
                            PlaybackStateManager.isRadioActuallyPlaying.value = false
                        }
                    })
                }
        } catch (e: Exception) {
            android.util.Log.e("RadioPlaybackService", "Failed to create ExoPlayer", e)
        }

        // Sync with PlaybackStateManager
        PlaybackStateManager.activeRadio.onEach { radio ->
            if (radio != null) {
                showNotification(radio, PlaybackStateManager.isPlaying.value)
                if (radio.id != currentRadioId) {
                    currentRadioId = radio.id
                    playStream(radio.url)
                }
            } else {
                currentRadioId = null
                try {
                    exoPlayer?.stop()
                    exoPlayer?.clearMediaItems()
                } catch (e: Exception) {}
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }.launchIn(serviceScope)

        PlaybackStateManager.isPlaying.onEach { isPlaying ->
            val radio = PlaybackStateManager.activeRadio.value
            if (radio != null) {
                showNotification(radio, isPlaying)
                try {
                    if (isPlaying) {
                        val player = exoPlayer
                        if (player != null && player.playbackState != androidx.media3.common.Player.STATE_IDLE && player.playbackState != androidx.media3.common.Player.STATE_ENDED) {
                            player.playWhenReady = true
                            player.play()
                        } else {
                            playStream(radio.url)
                        }
                    } else {
                        exoPlayer?.playWhenReady = false
                        exoPlayer?.pause()
                        PlaybackStateManager.isRadioActuallyPlaying.value = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RadioPlaybackService", "Error setting playWhenReady", e)
                }
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val radio = PlaybackStateManager.activeRadio.value
        if (radio != null) {
            showNotification(radio, PlaybackStateManager.isPlaying.value)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }

        when (intent?.action) {
            ACTION_PLAY -> {
                PlaybackStateManager.isPlaying.value = true
                PlaybackStateManager.activeRadio.value?.let { repository.saveLastActiveRadio(it) }
            }
            ACTION_PAUSE -> {
                PlaybackStateManager.isPlaying.value = false
            }
            ACTION_STOP -> {
                PlaybackStateManager.activeRadio.value = null
                PlaybackStateManager.isPlaying.value = false
                repository.saveLastActiveRadio(null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
            ACTION_NEXT -> {
                val radios = if (PlaybackStateManager.radioList.value.isNotEmpty()) {
                    PlaybackStateManager.radioList.value
                } else {
                    repository.loadRadios()
                }
                val active = PlaybackStateManager.activeRadio.value
                if (radios.isNotEmpty() && active != null) {
                    val currentIndex = radios.indexOfFirst { it.id == active.id }
                    val targetIndex = if (currentIndex >= 0) {
                        (currentIndex + 1) % radios.size
                    } else {
                        0
                    }
                    val nextRadio = radios[targetIndex]
                    PlaybackStateManager.activeRadio.value = nextRadio
                    PlaybackStateManager.isPlaying.value = true
                    repository.saveLastActiveRadio(nextRadio)
                }
            }
            ACTION_PREV -> {
                val radios = if (PlaybackStateManager.radioList.value.isNotEmpty()) {
                    PlaybackStateManager.radioList.value
                } else {
                    repository.loadRadios()
                }
                val active = PlaybackStateManager.activeRadio.value
                if (radios.isNotEmpty() && active != null) {
                    val currentIndex = radios.indexOfFirst { it.id == active.id }
                    val targetIndex = if (currentIndex >= 0) {
                        (currentIndex - 1 + radios.size) % radios.size
                    } else {
                        0
                    }
                    val prevRadio = radios[targetIndex]
                    PlaybackStateManager.activeRadio.value = prevRadio
                    PlaybackStateManager.isPlaying.value = true
                    repository.saveLastActiveRadio(prevRadio)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (e: Exception) {}
        exoPlayer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproductor de Radio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción para la radio activa"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(radio: RadioItem, isPlaying: Boolean) {
        val playPauseAction = if (isPlaying) {
            val intent = Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_PAUSE }
            val pendingIntent = PendingIntent.getService(
                this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pausa", pendingIntent
            )
        } else {
            val intent = Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_PLAY }
            val pendingIntent = PendingIntent.getService(
                this, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Reproducir", pendingIntent
            )
        }

        val stopIntent = Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopAction = NotificationCompat.Action(
            R.drawable.ic_stop, "Detener", stopPendingIntent
        )

        val nextIntent = Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this, 4, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Siguiente", nextPendingIntent
        )

        val prevIntent = Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(
            this, 5, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Anterior", prevPendingIntent
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigation_target", "radio")
            putExtra("focus_active_radio", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MediaPlay Hub")
            .setContentText("Sintonizando: ${radio.name}")
            .setSubText("Género: ${radio.genre}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("RadioPlaybackService", "Error starting foreground notification", e)
        }
    }
}
