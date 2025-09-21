package com.gilad.shabbas_clock_kt.app.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Alarm(
    val id: Int,
    val dateTime: String,
    var isActive: Boolean,
    val durationSeconds: Int,
    val volume: Int,
    val vibrate: Boolean,
    val ringtoneFile: String = "default"
) {
    fun getLocalDateTime(): LocalDateTime {
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    fun getTimeString(): String {
        val time = getLocalDateTime()
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    fun getDayName(): String {
        if (!isActive) return ""

        val now = LocalDateTime.now()
        val alarmTime = getLocalDateTime()

        if (alarmTime.isBefore(now)) {
            return ""
        }

        // חשב שעות עד הצלצול
        val hoursUntil = java.time.Duration.between(now, alarmTime).toHours()

        // אם השעון יצלצל ב-24 שעות הקרובות, אל תציג יום
        if (hoursUntil <= 24) {
            return ""
        }

        // אחרת, החזר את שם היום המלא
        val dayOfWeek = alarmTime.dayOfWeek.value
        return when (dayOfWeek) {
            1 -> "יום שני"
            2 -> "יום שלישי"
            3 -> "יום רביעי"
            4 -> "יום חמישי"
            5 -> "יום שישי"
            6 -> "שבת"
            7 -> "יום ראשון"
            else -> ""
        }
    }

    fun getTimeUntilAlarm(): String {
        val now = LocalDateTime.now()
        val alarmTime = getLocalDateTime()

        if (alarmTime.isBefore(now)) {
            return ""
        }

        val duration = java.time.Duration.between(now, alarmTime)
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60 + 1


        val parts = mutableListOf<String>()

        if (days > 0) parts.add("$days ימים")
        if (hours > 0) parts.add("$hours שעות")
        if (minutes > 1) parts.add("$minutes דקות")

        return when {
            parts.isEmpty() -> "יצלצל בעוד פחות מדקה"
            else -> "יצלצל בעוד " + parts.joinToString(" ו-")
        }
    }
}