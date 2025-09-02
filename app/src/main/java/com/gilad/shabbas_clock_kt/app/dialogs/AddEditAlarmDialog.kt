package com.gilad.shabbas_clock_kt.app.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.gilad.shabbas_clock_kt.R
import com.gilad.shabbas_clock_kt.app.models.Alarm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AddEditAlarmDialog(
    private val activity: AppCompatActivity,
    private val alarm: Alarm?,
    private val onSave: (Alarm) -> Unit
) : Dialog(activity) {

    private lateinit var timeButton: Button
    private lateinit var dayChipGroup: ChipGroup
    private lateinit var timeUntilText: TextView
    private lateinit var ringtoneText: TextView
    private lateinit var changeRingtoneButton: Button
    private lateinit var durationText: TextView
    private lateinit var durationSlider: Slider
    private lateinit var vibrateSwitch: SwitchMaterial
    private lateinit var volumeText: TextView
    private lateinit var volumeSlider: Slider
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private val durationOptions = listOf(5, 10, 15, 20, 30)
    private var selectedDateTime: LocalDateTime = LocalDateTime.now().plusMinutes(1)
    private var selectedRingtoneUri: String = "default"
    private var selectedRingtoneName: String = "צלצול ברירת מחדל"
    private val dayChips = mutableListOf<Chip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_add_edit_alarm)

        // הגדרת רוחב קבוע לדיאלוג
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        initializeViews()
        setupInitialValues()
        setupListeners()

        // אם זה דיאלוג חדש, פתח מיד את בוחר השעה
        if (alarm == null) {
            showTimePicker()
        }
    }

    private fun initializeViews() {
        timeButton = findViewById(R.id.timeButton)
        dayChipGroup = findViewById(R.id.dayChipGroup)
        timeUntilText = findViewById(R.id.timeUntilText)
        ringtoneText = findViewById(R.id.ringtoneText)
        changeRingtoneButton = findViewById(R.id.changeRingtoneButton)
        durationText = findViewById(R.id.durationText)
        durationSlider = findViewById(R.id.durationSlider)
        vibrateSwitch = findViewById(R.id.vibrateSwitch)
        volumeText = findViewById(R.id.volumeText)
        volumeSlider = findViewById(R.id.volumeSlider)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupInitialValues() {
        // הגדרת סליידר משך צלצול עם תוויות
        durationSlider.valueFrom = 0f
        durationSlider.valueTo = 4f
        durationSlider.stepSize = 1f
        durationSlider.setLabelFormatter { value ->
            "${durationOptions[value.toInt()]} שניות"
        }

        // הגדרת סליידר עוצמת קול
        volumeSlider.valueFrom = 0f
        volumeSlider.valueTo = 100f
        volumeSlider.stepSize = 10f

        if (alarm != null) {
            // מצב עריכה
            saveButton.text = "עדכן"
            val alarmTime = alarm.getLocalDateTime()
            selectedDateTime = alarmTime

            // משך צלצול
            val durationIndex = durationOptions.indexOf(alarm.durationSeconds)
            if (durationIndex != -1) {
                durationSlider.value = durationIndex.toFloat()
            }

            volumeSlider.value = alarm.volume.toFloat()
            vibrateSwitch.isChecked = alarm.vibrate

            if (alarm.ringtoneFile != "default") {
                selectedRingtoneUri = alarm.ringtoneFile
                selectedRingtoneName = alarm.ringtoneFile.substringAfterLast("/")
            }
        } else {
            // מצב הוספה
            saveButton.text = "הוסף"
            val now = LocalDateTime.now()
            selectedDateTime = now.plusMinutes(1)
            durationSlider.value = 1f // 10 שניות
            volumeSlider.value = 70f
            vibrateSwitch.isChecked = true
        }

        updateTimeButton()
        updateDayChips()
        updateTimeUntilText()
        updateDurationText()
        updateVolumeText()
        updateRingtoneText()
    }

    private fun setupListeners() {
        timeButton.setOnClickListener {
            showTimePicker()
        }

        durationSlider.addOnChangeListener { _, value, _ ->
            updateDurationText()
        }

        volumeSlider.addOnChangeListener { _, value, _ ->
            updateVolumeText()
        }

        changeRingtoneButton.setOnClickListener {
            openRingtonePicker()
        }

        saveButton.setOnClickListener {
            saveAlarm()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showTimePicker() {
        val currentHour = selectedDateTime.hour
        val currentMinute = selectedDateTime.minute

        TimePickerDialog(
            context,
            R.style.TimePickerTheme,
            { _, hourOfDay, minute ->
                updateSelectedTime(hourOfDay, minute)
            },
            currentHour,
            currentMinute,
            true
        ).show()
    }

    private fun updateSelectedTime(hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var newDateTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        // אם השעה שנבחרה כבר עברה היום, העבר למחר
        if (newDateTime.isBefore(now) || newDateTime.isEqual(now)) {
            newDateTime = newDateTime.plusDays(1)
        }

        selectedDateTime = newDateTime
        updateTimeButton()
        updateDayChips()
        updateTimeUntilText()
    }

    private fun updateTimeButton() {
        timeButton.text = String.format("%02d:%02d", selectedDateTime.hour, selectedDateTime.minute)
    }

    private fun updateDayChips() {
        dayChipGroup.removeAllViews()
        dayChips.clear()

        val now = LocalDateTime.now()
        val daysBetween = java.time.Duration.between(now, selectedDateTime).toDays().toInt()

        for (i in 0..3) {
            val chip = Chip(context)
            val targetDate = if (i < daysBetween) {
                now.plusDays(i.toLong())
            } else {
                selectedDateTime.plusDays((i - daysBetween).toLong())
            }

            val dayText = when {
                i == daysBetween -> when(i) {
                    0 -> "היום"
                    1 -> "מחר"
                    else -> getDayLetter(targetDate)
                }
                i < daysBetween -> getDayLetter(now.plusDays(i.toLong()))
                else -> getDayLetter(targetDate)
            }

            chip.text = dayText
            chip.isCheckable = true
            chip.isChecked = (i == daysBetween)
            chip.setOnClickListener {
                val daysToAdd = i.toLong()
                selectedDateTime = now.plusDays(daysToAdd)
                    .withHour(selectedDateTime.hour)
                    .withMinute(selectedDateTime.minute)
                    .withSecond(0)
                    .withNano(0)

                // אם השעה כבר עברה, תוסיף יום
                if (selectedDateTime.isBefore(now)) {
                    selectedDateTime = selectedDateTime.plusDays(1)
                }

                updateDayChips()
                updateTimeUntilText()
            }

            dayChips.add(chip)
            dayChipGroup.addView(chip)
        }
    }

    private fun getDayLetter(date: LocalDateTime): String {
        return when(date.dayOfWeek.value) {
            1 -> "ב'" // שני
            2 -> "ג'" // שלישי
            3 -> "ד'" // רביעי
            4 -> "ה'" // חמישי
            5 -> "ו'" // שישי
            6 -> "ש'" // שבת
            7 -> "א'" // ראשון
            else -> ""
        }
    }

    private fun updateTimeUntilText() {
        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(now, selectedDateTime)

        if (duration.isNegative) {
            timeUntilText.text = "הזמן שנבחר כבר עבר"
            saveButton.isEnabled = false
            return
        }

        saveButton.isEnabled = true
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        val text = when {
            days > 0 -> "יצלצל בעוד $days ימים, $hours שעות ו-$minutes דקות"
            hours > 0 -> "יצלצל בעוד $hours שעות ו-$minutes דקות"
            else -> "יצלצל בעוד $minutes דקות"
        }
        timeUntilText.text = text
    }

    private fun updateDurationText() {
        val duration = durationOptions[durationSlider.value.toInt()]
        durationText.text = "משך צלצול: $duration שניות"
    }

    private fun updateVolumeText() {
        volumeText.text = "עוצמת קול: ${volumeSlider.value.toInt()}%"
    }

    private fun updateRingtoneText() {
        ringtoneText.text = selectedRingtoneName
    }

    private fun openRingtonePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }

        // צריך להשתמש ב-Activity כדי לקבל תוצאה
        Toast.makeText(context, "בחירת צלצול תהיה זמינה בגרסה הבאה", Toast.LENGTH_SHORT).show()
    }

    private fun saveAlarm() {
        val dateTimeString = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val duration = durationOptions[durationSlider.value.toInt()]
        val volume = volumeSlider.value.toInt()

        val newAlarm = if (alarm != null) {
            alarm.copy(
                dateTime = dateTimeString,
                durationSeconds = duration,
                volume = volume,
                vibrate = vibrateSwitch.isChecked,
                ringtoneFile = selectedRingtoneUri
            )
        } else {
            Alarm(
                id = 0,
                dateTime = dateTimeString,
                isActive = true,
                durationSeconds = duration,
                volume = volume,
                vibrate = vibrateSwitch.isChecked,
                ringtoneFile = selectedRingtoneUri
            )
        }

        onSave(newAlarm)
        dismiss()
    }
}