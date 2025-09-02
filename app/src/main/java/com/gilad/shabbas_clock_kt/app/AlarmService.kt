package com.gilad.shabbas_clock_kt.app

import com.gilad.shabbas_clock_kt.R
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var job: Job? = null

    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        val duration = intent?.getIntExtra("duration", 10) ?: 10
        val volume = intent?.getIntExtra("volume", 70) ?: 70
        val vibrate = intent?.getBooleanExtra("vibrate", true) ?: true
        val ringtone = intent?.getStringExtra("ringtone") ?: "default"

        startForeground(NOTIFICATION_ID, createNotification())

        playAlarm(duration, volume, vibrate, ringtone)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "התראות שעון מעורר",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "התראות עבור שעון מעורר לשבת"
                enableLights(true)
                enableVibration(true)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, StopAlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("שעון מעורר")
            .setContentText("השעון מצלצל")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(mainPendingIntent, true)
            .addAction(R.drawable.ic_stop, "עצור", stopPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun playAlarm(durationSeconds: Int, volume: Int, vibrate: Boolean, ringtoneFile: String) {
        // הגדרת עוצמת קול
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVolume = (maxVolume * volume / 100)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

        // הפעלת צלצול
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // הפעלת רטט
        if (vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 1000, 1000) // רטט למשך שנייה, הפסקה של שנייה
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }

        // עצירה אוטומטית אחרי הזמן שנקבע
        job = GlobalScope.launch {
            delay(durationSeconds * 1000L)
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        job?.cancel()

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}