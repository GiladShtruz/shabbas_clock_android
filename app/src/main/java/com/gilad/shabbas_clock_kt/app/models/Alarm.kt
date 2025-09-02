package com.gilad.shabbas_clock_kt.app.models

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Alarm(
    val id: Int,
    val dateTime: String, // format: "yyyy-MM-dd HH:mm"
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

    fun getDateString(): String {
        val date = getLocalDateTime()
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    fun getTimeUntilAlarm(): String {
        val now = LocalDateTime.now()
        val alarmTime = getLocalDateTime()

        if (alarmTime.isBefore(now)) {
            return "עבר"
        }

        val duration = Duration.between(now, alarmTime)
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        return when {
            days > 0 -> "בעוד $days ימים, $hours שעות ו-$minutes דקות"
            hours > 0 -> "בעוד $hours שעות ו-$minutes דקות"
            else -> "בעוד $minutes דקות"
        }
    }
}