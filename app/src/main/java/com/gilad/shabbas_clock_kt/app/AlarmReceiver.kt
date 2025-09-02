package com.gilad.shabbas_clock_kt.app


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gilad.shabbas_clock_kt.app.repository.AlarmRepository

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val duration = intent.getIntExtra("duration", 10)
        val volume = intent.getIntExtra("volume", 70)
        val vibrate = intent.getBooleanExtra("vibrate", true)
        val ringtone = intent.getStringExtra("ringtone") ?: "default"

        // כיבוי השעון ב-JSON
        if (alarmId != -1) {
            val repository = AlarmRepository(context)
            val alarms = repository.getAllAlarms()
            val alarm = alarms.find { it.id == alarmId }
            alarm?.let {
                repository.updateAlarm(it.copy(isActive = false))
            }
        }

        // הפעלת השירות
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("duration", duration)
            putExtra("volume", volume)
            putExtra("vibrate", vibrate)
            putExtra("ringtone", ringtone)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}