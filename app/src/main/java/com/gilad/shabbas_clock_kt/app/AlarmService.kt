// ========================================
// קובץ: AlarmService.kt - תיקון מלא ללא GlobalScope
// ========================================
package com.gilad.shabbas_clock_kt.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import com.gilad.shabbas_clock_kt.R

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var stopAlarmJob: Job? = null

    // יצירת scope משלנו לשירות
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

        // שימוש ב-ServiceCompat לתמיכה בגרסאות שונות
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

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
                setBypassDnd(true)
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
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(mainPendingIntent, true)
            .addAction(R.drawable.ic_stop, "עצור", stopPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun playAlarm(durationSeconds: Int, volume: Int, vibrate: Boolean, ringtoneFile: String) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val previousRingerMode = audioManager.ringerMode
        val previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        try {
            // הגדר למצב רגיל כדי שהצלצול יעבוד
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }

            // הגדרת עוצמת קול
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVolume = (maxVolume * volume / 100)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

            // הפעלת צלצול
            playRingtone(ringtoneFile)

            // הפעלת רטט רציף
            if (vibrate) {
                startContinuousVibration()
            }

            // השתמש ב-serviceScope במקום GlobalScope
            stopAlarmJob = serviceScope.launch {
                delay(durationSeconds * 1000L)
                stopAlarm()

                // החזר מצב קודם
                withContext(Dispatchers.Main) {
                    audioManager.ringerMode = previousRingerMode
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playRingtone(ringtoneFile: String) {
        try {
            val ringtoneUri = when {
                ringtoneFile == "default" -> {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                ringtoneFile.startsWith("content://") -> {
                    Uri.parse(ringtoneFile)
                }
                else -> {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            }

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
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(this, defaultUri)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startContinuousVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        stopAlarmJob?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        // ביטול ה-scope כשהשירות נהרס
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}