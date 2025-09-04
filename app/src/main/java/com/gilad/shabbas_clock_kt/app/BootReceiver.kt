package com.gilad.shabbas_clock_kt.app

// ========================================
// קובץ: app/src/main/java/com/shabbatalarm/app/BootReceiver.kt
// ========================================


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gilad.shabbas_clock_kt.app.repository.AlarmRepository
import com.gilad.shabbas_clock_kt.app.services.AlarmManagerService
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // טעינת כל השעונים הפעילים מחדש אחרי אתחול
            val repository = AlarmRepository(context)
            val alarmManager = AlarmManagerService(context)
            val now = LocalDateTime.now()

            repository.getAllAlarms()
                .filter { it.isActive && it.getLocalDateTime().isAfter(now) }
                .forEach { alarm ->
                    alarmManager.setAlarm(alarm)
                }
        }
    }
}