package com.gilad.shabbas_clock_kt.app.repository


import android.content.Context
import com.gilad.shabbas_clock_kt.app.models.Alarm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDateTime
import kotlin.collections.contains

class AlarmRepository(private val context: Context) {
    private val fileName = "alarms.json"
    private val gson = Gson()

    fun getAllAlarms(): List<Alarm> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            return emptyList()
        }

        return try {
            val json = file.readText()
            val type = object : TypeToken<List<Alarm>>() {}.type
            val alarms: List<Alarm> = gson.fromJson(json, type)

            // מסנן שעונים שעברו ומכבה אותם - רק אם הם פעילים
            val now = LocalDateTime.now()
            val updatedAlarms = alarms.map { alarm ->
                // כבה רק אם השעון פעיל והזמן עבר
                if (alarm.isActive && alarm.getLocalDateTime().isBefore(now)) {
                    alarm.copy(isActive = false)
                } else {
                    alarm // החזר כמו שהוא - שמור על המצב
                }
            }

            // שומר שינויים רק אם היו
            if (updatedAlarms != alarms) {
                saveAlarms(updatedAlarms)
            }

            // מיון לפי שעה יומית
            updatedAlarms.sortedBy { alarm ->
                val time = alarm.getLocalDateTime()
                time.hour * 60 + time.minute
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun saveAlarms(alarms: List<Alarm>) {
        val file = File(context.filesDir, fileName)
        val json = gson.toJson(alarms)
        file.writeText(json)
    }

    fun addAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        alarms.add(alarm)
        saveAlarms(alarms)
    }

    fun updateAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            alarms[index] = alarm
            saveAlarms(alarms)
        }
    }

    fun deleteAlarm(alarmId: Int) {
        val alarms = getAllAlarms().toMutableList()
        alarms.removeAll { it.id == alarmId }
        saveAlarms(alarms)
    }

    fun deleteAlarms(alarmIds: List<Int>) {
        val alarms = getAllAlarms().toMutableList()
        alarms.removeAll { it.id in alarmIds }
        saveAlarms(alarms)
    }

    fun getNextAlarmId(): Int {
        val alarms = getAllAlarms()
        return if (alarms.isEmpty()) 1 else alarms.maxOf { it.id } + 1
    }
}