// ========================================
// קובץ: AddEditAlarmBottomSheet.kt - מלא
// ========================================
package com.gilad.shabbas_clock_kt.app.dialogs

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.gilad.shabbas_clock_kt.R
import com.gilad.shabbas_clock_kt.app.models.Alarm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AddEditAlarmBottomSheet(
    private val alarm: Alarm?,
    private val onSave: (Alarm) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var timeButton: Button
    private lateinit var dayChipGroup: ChipGroup
    private lateinit var timeUntilText: TextView
    private lateinit var durationText: TextView
    private lateinit var durationSlider: Slider
    private lateinit var volumeText: TextView
    private lateinit var volumeSlider: Slider
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private val durationOptions = listOf(5, 10, 15, 20, 30)
    private var selectedDateTime: LocalDateTime = LocalDateTime.now().plusMinutes(1)
    private val dayChips = mutableListOf<Chip>()

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateTimeUntilText()
            updateDayChips()
            refreshHandler.postDelayed(this, 30000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_edit_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupInitialValues()
        setupListeners()

        // אם זה שעון חדש, פתח מיד את בוחר השעה
        if (alarm == null) {
            Handler(Looper.getMainLooper()).postDelayed({
                showTimePicker()
            }, 300)
        }
    }

    private fun initializeViews(view: View) {
        timeButton = view.findViewById(R.id.timeButton)
        dayChipGroup = view.findViewById(R.id.dayChipGroup)
        timeUntilText = view.findViewById(R.id.timeUntilText)
        durationText = view.findViewById(R.id.durationText)
        durationSlider = view.findViewById(R.id.durationSlider)
        volumeText = view.findViewById(R.id.volumeText)
        volumeSlider = view.findViewById(R.id.volumeSlider)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
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
        } else {
            // מצב הוספה
            saveButton.text = "הוסף"
            val now = LocalDateTime.now()
            selectedDateTime = now.plusMinutes(1)
            durationSlider.value = 1f // 10 שניות
            volumeSlider.value = 70f
        }

        updateTimeButton()
        updateDayChips()
        updateTimeUntilText()
        updateDurationText()
        updateVolumeText()

        // הפעל רענון אוטומטי
        startAutoRefresh()
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

        saveButton.setOnClickListener {
            saveAlarm()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun startAutoRefresh() {
        refreshHandler.post(refreshRunnable)
    }

    override fun onStop() {
        super.onStop()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun showTimePicker() {
        val currentHour = selectedDateTime.hour
        val currentMinute = selectedDateTime.minute

        TimePickerDialog(
            requireContext(),
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
        val selectedHour = selectedDateTime.hour
        val selectedMinute = selectedDateTime.minute

        var targetDateTime = now.withHour(selectedHour).withMinute(selectedMinute).withSecond(0).withNano(0)
        val isTimePassed = targetDateTime.isBefore(now) || targetDateTime.isEqual(now)

        if (isTimePassed) {
            targetDateTime = targetDateTime.plusDays(1)
            // אל תעדכן את selectedDateTime כאן! רק אם המשתמש בוחר
        }

        for (i in 0..3) {
            val chip = Chip(requireContext())
            chip.isCheckable = true
            chip.isClickable = true
            // אל תשנה את isEnabled - תמיד true

            val chipDateTime = if (isTimePassed) {
                now.plusDays((i + 1).toLong())
                    .withHour(selectedHour)
                    .withMinute(selectedMinute)
                    .withSecond(0)
                    .withNano(0)
            } else {
                now.plusDays(i.toLong())
                    .withHour(selectedHour)
                    .withMinute(selectedMinute)
                    .withSecond(0)
                    .withNano(0)
            }

            val chipText = when {
                !isTimePassed && i == 0 -> "היום"
                isTimePassed && i == 0 -> "מחר"
                else -> getDayName(chipDateTime)
            }

            chip.text = chipText
            chip.tag = chipDateTime

            // בדוק אם זה היום הנבחר
            val isSameDay = chipDateTime.toLocalDate() == selectedDateTime.toLocalDate()
            chip.isChecked = isSameDay

            // Listener פשוט וישיר
            chip.setOnClickListener {
                val selected = it.tag as LocalDateTime
                selectedDateTime = selected
                // עדכן רק את הצ'יפים והטקסט
                for (otherChip in dayChips) {
                    val otherDate = otherChip.tag as LocalDateTime
                    otherChip.isChecked = otherDate.toLocalDate() == selected.toLocalDate()
                }
                updateTimeUntilText()
            }

            dayChips.add(chip)
            dayChipGroup.addView(chip)
        }
    }
    private fun getDayName(date: LocalDateTime): String {
        return when(date.dayOfWeek.value) {
            1 -> "ב'"
            2 -> "ג'"
            3 -> "ד'"
            4 -> "ה'"
            5 -> "ו'"
            6 -> "ש'"
            7 -> "א'"
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

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days ימים")
        if (hours > 0) parts.add("$hours שעות")
        if (minutes > 0) parts.add("$minutes דקות")

        val text = when {
            parts.isEmpty() -> "מוגדר לעוד פחות מדקה"
            else -> "מוגדר לעוד " + parts.joinToString(" ו-")
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

    private fun saveAlarm() {
        val dateTimeString = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val duration = durationOptions[durationSlider.value.toInt()]
        val volume = volumeSlider.value.toInt()

        val newAlarm = if (alarm != null) {
            alarm.copy(
                dateTime = dateTimeString,
                durationSeconds = duration,
                volume = volume,
                vibrate = true, // תמיד עם רטט
                ringtoneFile = "default" // תמיד צלצול ברירת מחדל
            )
        } else {
            Alarm(
                id = 0, // יוחלף ב-MainActivity
                dateTime = dateTimeString,
                isActive = true,
                durationSeconds = duration,
                volume = volume,
                vibrate = true, // תמיד עם רטט
                ringtoneFile = "default" // תמיד צלצול ברירת מחדל
            )
        }

        onSave(newAlarm)
        dismiss()
    }
}