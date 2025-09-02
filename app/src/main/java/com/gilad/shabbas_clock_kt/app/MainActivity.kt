package com.gilad.shabbas_clock_kt.app

import com.gilad.shabbas_clock_kt.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gilad.shabbas_clock_kt.app.adapters.AlarmAdapter
import com.gilad.shabbas_clock_kt.app.dialogs.AddEditAlarmDialog
import com.gilad.shabbas_clock_kt.app.models.Alarm
import com.gilad.shabbas_clock_kt.app.repository.AlarmRepository
import com.gilad.shabbas_clock_kt.app.services.AlarmManagerService

class MainActivity : AppCompatActivity(), AlarmAdapter.OnAlarmClickListener {

    private lateinit var repository: AlarmRepository
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var alarmManager: AlarmManagerService
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var fabAddAlarm: FloatingActionButton
    private lateinit var editButton: ImageView
    private lateinit var infoButton: ImageView

    private var isEditMode = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateAlarmsList()
            handler.postDelayed(this, 60000) // עדכון כל דקה
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeServices()
        setupClickListeners()
        checkPermissions()
        updateAlarmsList()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.alarmsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        fabAddAlarm = findViewById(R.id.fabAddAlarm)
        editButton = findViewById(R.id.editButton)
        infoButton = findViewById(R.id.infoButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        alarmAdapter = AlarmAdapter(this)
        recyclerView.adapter = alarmAdapter
    }

    private fun initializeServices() {
        repository = AlarmRepository(this)
        alarmManager = AlarmManagerService(this)
    }

    private fun setupClickListeners() {
        fabAddAlarm.setOnClickListener {
            if (isEditMode) {
                deleteSelectedAlarms()
            } else {
                showAddEditDialog(null)
            }
        }

        editButton.setOnClickListener {
            toggleEditMode()
        }

        infoButton.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun updateAlarmsList() {
        val alarms = repository.getAllAlarms()
        alarmAdapter.submitList(alarms)

        if (alarms.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        alarmAdapter.setEditMode(isEditMode)

        if (isEditMode) {
            fabAddAlarm.setImageResource(R.drawable.ic_delete)
            editButton.setColorFilter(ContextCompat.getColor(this, R.color.accent))
        } else {
            fabAddAlarm.setImageResource(R.drawable.ic_add)
            editButton.setColorFilter(ContextCompat.getColor(this, R.color.white))
            alarmAdapter.clearSelection()
        }
    }

    private fun deleteSelectedAlarms() {
        val selectedIds = alarmAdapter.getSelectedAlarmIds()
        if (selectedIds.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("מחיקת שעונים")
                .setMessage("האם למחוק ${selectedIds.size} שעונים?")
                .setPositiveButton("מחק") { _, _ ->
                    selectedIds.forEach { id ->
                        val alarm = repository.getAllAlarms().find { it.id == id }
                        alarm?.let { alarmManager.cancelAlarm(it) }
                    }
                    repository.deleteAlarms(selectedIds)
                    updateAlarmsList()
                    toggleEditMode()
                }
                .setNegativeButton("ביטול", null)
                .show()
        }
    }

//    private fun showAddEditDialog(alarm: Alarm?) {
//        val dialog = AddEditAlarmDialog(this, alarm) { updatedAlarm ->
//            if (alarm == null) {
//                val newAlarm = updatedAlarm.copy(id = repository.getNextAlarmId())
//                repository.addAlarm(newAlarm)
//                if (newAlarm.isActive) {
//                    alarmManager.setAlarm(newAlarm)
//                }
//            } else {
//                repository.updateAlarm(updatedAlarm)
//                alarmManager.cancelAlarm(alarm)
//                if (updatedAlarm.isActive) {
//                    alarmManager.setAlarm(updatedAlarm)
//                }
//            }
//            updateAlarmsList()
//        }
//        dialog.show()
//    }
private fun showAddEditDialog(alarm: Alarm?) {
    val dialog = AddEditAlarmDialog(this, alarm) { updatedAlarm ->  // שים לב: this במקום context
        if (alarm == null) {
            val newAlarm = updatedAlarm.copy(id = repository.getNextAlarmId())
            repository.addAlarm(newAlarm)
            if (newAlarm.isActive) {
                alarmManager.setAlarm(newAlarm)
            }
        } else {
            repository.updateAlarm(updatedAlarm)
            alarmManager.cancelAlarm(alarm)
            if (updatedAlarm.isActive) {
                alarmManager.setAlarm(updatedAlarm)
            }
        }
        updateAlarmsList()
    }
    dialog.show()
}
    private fun showInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("אודות")
            .setMessage("מעורר לשבת\nגרסה 1.0\n\nאפליקציה מיוחדת לשומרי שבת")
            .setPositiveButton("סגור", null)
            .show()
    }

    override fun onAlarmClick(alarm: Alarm) {
        if (!isEditMode) {
            showAddEditDialog(alarm)
        }
    }

    override fun onAlarmToggle(alarm: Alarm, isChecked: Boolean) {
        val updatedAlarm = alarm.copy(isActive = isChecked)
        repository.updateAlarm(updatedAlarm)

        if (isChecked) {
            alarmManager.setAlarm(updatedAlarm)
        } else {
            alarmManager.cancelAlarm(updatedAlarm)
        }

        updateAlarmsList()
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
}