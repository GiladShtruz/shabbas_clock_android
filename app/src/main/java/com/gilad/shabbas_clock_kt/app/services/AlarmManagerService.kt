package com.gilad.shabbas_clock_kt.app.services


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gilad.shabbas_clock_kt.app.AlarmReceiver
import com.gilad.shabbas_clock_kt.app.models.Alarm
import java.time.ZoneId

class AlarmManagerService(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun setAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("duration", alarm.durationSeconds)
            putExtra("volume", alarm.volume)
            putExtra("vibrate", alarm.vibrate)
            putExtra("ringtone", alarm.ringtoneFile)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmTimeMillis = alarm.getLocalDateTime()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTimeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                alarmTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}