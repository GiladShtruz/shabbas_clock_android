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

class AlarmAdapter(
    private val listener: OnAlarmClickListener
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    private var isEditMode = false
    private val selectedAlarms = mutableSetOf<Int>()

    interface OnAlarmClickListener {
        fun onAlarmClick(alarm: Alarm)
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

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val durationText: TextView = itemView.findViewById(R.id.durationText)
        private val timeUntilText: TextView = itemView.findViewById(R.id.timeUntilText)
        private val toggleSwitch: SwitchCompat = itemView.findViewById(R.id.toggleSwitch)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        fun bind(alarm: Alarm) {
            timeText.text = alarm.getTimeString()
            dateText.text = alarm.getDateString()
            durationText.text = "משך צלצול: ${alarm.durationSeconds} שניות"

            if (alarm.isActive) {
                timeUntilText.text = alarm.getTimeUntilAlarm()
                timeUntilText.visibility = View.VISIBLE
                cardView.alpha = 1.0f
            } else {
                timeUntilText.visibility = View.GONE
                cardView.alpha = 0.3f
            }

            if (isEditMode) {
                toggleSwitch.visibility = View.GONE
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = selectedAlarms.contains(alarm.id)

                cardView.setOnClickListener {
                    if (selectedAlarms.contains(alarm.id)) {
                        selectedAlarms.remove(alarm.id)
                        checkBox.isChecked = false
                    } else {
                        selectedAlarms.add(alarm.id)
                        checkBox.isChecked = true
                    }
                }
            } else {
                toggleSwitch.visibility = View.VISIBLE
                checkBox.visibility = View.GONE
                toggleSwitch.isChecked = alarm.isActive

                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    listener.onAlarmToggle(alarm, isChecked)
                }

                cardView.setOnClickListener {
                    listener.onAlarmClick(alarm)
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