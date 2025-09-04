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

            // קבע opacity מיידית בלי אנימציה או בדיקות
            cardView.alpha = if (alarm.isActive) 1.0f else 0.6f

            // עדכון תצוגה לפי מצב השעון
            if (alarm.isActive) {
                val dayName = alarm.getDayName()
                if (dayName.isNotEmpty()) {
                    dayNameText.text = dayName
                    dayNameText.visibility = View.VISIBLE
                } else {
                    dayNameText.visibility = View.GONE
                }

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
                if (selectedAlarms.contains(alarm.id)) {
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.primary_light)
                    )
                    cardView.cardElevation = 8.dp.toFloat()
                } else {
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.white)
                    )
                    cardView.cardElevation = 4.dp.toFloat()
                }

                cardView.setOnClickListener {
                    if (selectedAlarms.contains(alarm.id)) {
                        selectedAlarms.remove(alarm.id)
                        checkBox.isChecked = false
                        cardView.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.white)
                        )
                        cardView.cardElevation = 4.dp.toFloat()
                    } else {
                        selectedAlarms.add(alarm.id)
                        checkBox.isChecked = true
                        cardView.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.primary_light)
                        )
                        cardView.cardElevation = 8.dp.toFloat()
                    }
                }

                cardView.setOnLongClickListener(null)
            } else {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
                cardView.cardElevation = 4.dp.toFloat()

                toggleSwitch.visibility = View.VISIBLE
                checkBox.visibility = View.GONE

                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = alarm.isActive
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != alarm.isActive) {
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
        }    }

    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}
