package com.gilad.shabbas_clock_kt.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gilad.shabbas_clock_kt.R
import com.gilad.shabbas_clock_kt.app.models.Alarm
import androidx.core.content.ContextCompat


class AlarmAdapter(
    private val listener: OnAlarmClickListener
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    private var isEditMode = false
    private val selectedAlarms = mutableSetOf<Int>()
    private var allAlarmsCount = 0

    interface OnAlarmClickListener {
        fun onAlarmClick(alarm: Alarm)
        fun onAlarmLongClick(alarm: Alarm): Boolean
        fun onAlarmToggle(alarm: Alarm, isChecked: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCurrentListChanged(previousList: MutableList<Alarm>, currentList: MutableList<Alarm>) {
        super.onCurrentListChanged(previousList, currentList)
        allAlarmsCount = currentList.size
    }

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        if (!editMode) {
            selectedAlarms.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedAlarmIds(): List<Int> {
        return selectedAlarms.toList()
    }

    fun clearSelection() {
        selectedAlarms.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        currentList.forEach { alarm ->
            selectedAlarms.add(alarm.id)
        }
        notifyDataSetChanged()
    }

    fun isAllSelected(): Boolean {
        return selectedAlarms.size == allAlarmsCount && allAlarmsCount > 0
    }

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val dayNameText: TextView = itemView.findViewById(R.id.dayNameText)
        private val timeUntilText: TextView = itemView.findViewById(R.id.timeUntilText)
        private val toggleSwitch: SwitchCompat = itemView.findViewById(R.id.toggleSwitch)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        private val Int.dp: Int
            get() = (this * itemView.context.resources.displayMetrics.density).toInt()
        fun bind(alarm: Alarm) {
            timeText.text = alarm.getTimeString()

            // קבע opacity התחלתי בהתאם למצב השעון
            cardView.alpha = if (alarm.isActive) 1f else 0.6f

            // עדכון תצוגת ימים ושעות
            if (alarm.isActive) {
                val dayName = alarm.getDayName()
                dayNameText.visibility = if (dayName.isNotEmpty()) {
                    dayNameText.text = dayName
                    View.VISIBLE
                } else View.GONE

                timeUntilText.text = alarm.getTimeUntilAlarm()
                timeUntilText.visibility = View.VISIBLE
            } else {
                dayNameText.visibility = View.GONE
                timeUntilText.visibility = View.GONE
            }

            if (isEditMode) {
                toggleSwitch.visibility = View.GONE
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = selectedAlarms.contains(alarm.id)

                // הדגשת כרטיס מסומן
                val isSelected = selectedAlarms.contains(alarm.id)
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context,
                        if (isSelected) R.color.primary_light else R.color.white
                    )
                )
                cardView.cardElevation = if (isSelected) 8.dp.toFloat() else 4.dp.toFloat()

                cardView.setOnClickListener {
                    // במצב עריכה, לחיצה על כרטיס צריכה לשנות את מצב הבחירה (toggle)
                    val currentlySelected = selectedAlarms.contains(alarm.id)
                    if (currentlySelected) {
                        // אם הפריט כבר נבחר, הסר אותו מהרשימה
                        selectedAlarms.remove(alarm.id)
                        checkBox.isChecked = false
                        cardView.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.white)
                        )
                        cardView.cardElevation = 4.dp.toFloat()
                    } else {
                        // אם הפריט לא נבחר, הוסף אותו לרשימה
                        selectedAlarms.add(alarm.id)
                        checkBox.isChecked = true
                        cardView.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.primary_light)
                        )
                        cardView.cardElevation = 8.dp.toFloat()
                    }
                }

                cardView.setOnLongClickListener(null)
            }

            else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                cardView.cardElevation = 4.dp.toFloat()

                toggleSwitch.visibility = View.VISIBLE
                checkBox.visibility = View.GONE

                // בטיחות - לא להפעיל listener קודם
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = alarm.isActive

                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != alarm.isActive) {
                        // אנימציה של ה-CardView מיידית עם שינוי ה-Switch
                        val targetAlpha = if (isChecked) 1f else 0.6f
                        cardView.animate()
                            .alpha(targetAlpha)
                            .setDuration(200)
                            .start()
//                        if (alarm.isActive) {
//                            // Fade in של הטקסטים
//                            dayNameText.alpha = 0f
//                            dayNameText.visibility = View.VISIBLE
//                            dayNameText.animate()
//                                .alpha(1f)
//                                .setDuration(200)
//                                .setStartDelay(100) // התחל אחרי 100ms
//                                .start()
//
//                            timeUntilText.alpha = 0f
//                            timeUntilText.visibility = View.VISIBLE
//                            timeUntilText.animate()
//                                .alpha(1f)
//                                .setDuration(200)
//                                .setStartDelay(150) // התחל אחרי 150ms
//                                .start()
//                        }
//                        else {
//                            // Fade out של הטקסטים
//                            dayNameText.animate()
//                                .alpha(0f)
//                                .setDuration(200)
//                                .withEndAction {
//                                    dayNameText.visibility = View.GONE
//                                }
//                                .start()
//
//                            timeUntilText.animate()
//                                .alpha(0f)
//                                .setDuration(200)
//                                .withEndAction {
//                                    timeUntilText.visibility = View.GONE
//                                }
//                                .start()
//                        }

                        // עדכון ה-Alarms ברקע
                        listener.onAlarmToggle(alarm, isChecked)
                    }
                }

                cardView.setOnClickListener {
                    listener.onAlarmClick(alarm)
                }

                cardView.setOnLongClickListener {
                    listener.onAlarmLongClick(alarm)
                }
            }
        }


    }


    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}
