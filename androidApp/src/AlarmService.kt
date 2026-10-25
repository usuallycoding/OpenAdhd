package com.usuallycoding.openadhd

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.util.Log
import android.os.Vibrator
import com.usuallycoding.openadhd.domain.AlarmPlan

/**
 * Rings an alarm: the alarm sound on the alarm volume in a loop, vibration, and an ongoing notification with the
 * alarm screen. It stops when the user dismisses it or opens the task, and by itself after [AlarmPlan.RingLimit].
 */
class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var entryId: String = ""
    private var text: String = ""
    private val limit = Runnable { onLimit() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionRing -> try {
                ring(intent)
            } catch (e: Exception) {
                // Whatever went wrong, the user must still be told: post the alarm as a notification that makes its own sound.
                Log.e(Tag, "The alarm could not start ringing; showing its notification instead", e)
                fallBack(intent)
            }
            ActionDismiss -> end()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun ring(intent: Intent) {
        entryId = intent.getStringExtra(ReminderAlarms.ExtraEntryId) ?: return stopSelf()
        text = intent.getStringExtra(ReminderAlarms.ExtraText) ?: return stopSelf()
        AlarmNotifications.ensureChannels(this)
        val notification = AlarmNotifications.build(this, entryId, text, ringing = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(AlarmNotifications.NotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(AlarmNotifications.NotificationId, notification)
        }
        // A ring in progress is replaced, not layered over.
        stopSound()
        startSound()
        handler.removeCallbacks(limit)
        handler.postDelayed(limit, AlarmPlan.RingLimit.inWholeMilliseconds)
        sendBroadcast(Intent(ActionRinging).setPackage(packageName).putExtra(ReminderAlarms.ExtraEntryId, entryId).putExtra(ReminderAlarms.ExtraText, text))
    }

    /**
     * The ring could not start as a service: show the alarm notification that plays its own sound instead. The service
     * was started as a foreground service, which must call startForeground before it ends, so it does that first.
     */
    private fun fallBack(intent: Intent) {
        stopSound()
        val id = intent.getStringExtra(ReminderAlarms.ExtraEntryId) ?: ""
        val label = intent.getStringExtra(ReminderAlarms.ExtraText) ?: ""
        try {
            AlarmNotifications.ensureChannels(this)
            val notification = AlarmNotifications.build(this, id, label, ringing = false)
            try {
                startForeground(AlarmNotifications.NotificationId, notification)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_DETACH)
            } catch (e: Exception) {
                Log.e(Tag, "The fallback alarm could not run as a foreground service", e)
                getSystemService(NotificationManager::class.java).notify(AlarmNotifications.NotificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(Tag, "The fallback alarm notification could not be shown", e)
        }
        stopSelf()
    }

    private fun startSound() {
        try {
            val uri = AlarmNotifications.alarmSound(this)
            if (uri != null) {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(Tag, "The alarm sound could not be played", e)
            player?.release()
            player = null
        }
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 800, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    private fun stopSound() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
    }

    /** Stops the ring and removes the alarm's notification. */
    private fun end() {
        handler.removeCallbacks(limit)
        stopSound()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        sendBroadcast(Intent(ActionEnded).setPackage(packageName))
        stopSelf()
    }

    /** Nobody answered in time: stop the noise and leave the usual notification for the task. */
    private fun onLimit() {
        val id = entryId
        val label = text
        end()
        ReminderNotifications.show(this, id, label)
    }

    override fun onDestroy() {
        handler.removeCallbacks(limit)
        stopSound()
        super.onDestroy()
    }

    companion object {
        private const val Tag = "AlarmService"

        const val ActionRing = "com.usuallycoding.openadhd.alarm.RING"
        const val ActionDismiss = "com.usuallycoding.openadhd.alarm.DISMISS"

        /** Broadcast when a ring begins or is replaced, with the entry and its text, so the alarm screen can follow. */
        const val ActionRinging = "com.usuallycoding.openadhd.alarm.RINGING"

        /** Broadcast when the ring has ended, so the alarm screen can close. */
        const val ActionEnded = "com.usuallycoding.openadhd.alarm.ENDED"
    }
}
