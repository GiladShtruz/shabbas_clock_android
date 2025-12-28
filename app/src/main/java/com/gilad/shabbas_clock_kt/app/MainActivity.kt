// ========================================
// קובץ: MainActivity.kt - מלא
// ========================================
package com.gilad.shabbas_clock_kt.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.Rect
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.gilad.shabbas_clock_kt.R
import com.gilad.shabbas_clock_kt.app.adapters.AlarmAdapter
import com.gilad.shabbas_clock_kt.app.dialogs.AddEditAlarmBottomSheet
import com.gilad.shabbas_clock_kt.app.models.Alarm
import com.gilad.shabbas_clock_kt.app.repository.AlarmRepository
import com.gilad.shabbas_clock_kt.app.services.AlarmManagerService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity(), AlarmAdapter.OnAlarmClickListener {

    private lateinit var repository: AlarmRepository
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var alarmManager: AlarmManagerService
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var fabAddAlarm: ExtendedFloatingActionButton
    private lateinit var editButton: ImageView
    private lateinit var infoButton: ImageView
    private lateinit var closeEditButton: ImageView
    private lateinit var selectAllButton: ImageView

    private var isEditMode = false
    private var allSelected = false
    private val handler = Handler(Looper.getMainLooper())

    // החלף את updateRunnable (שורות 43-54) בקוד הזה:
    private val updateRunnable = object : Runnable {
        override fun run() {
            // עדכון הרשימה ובדיקת שעונים פעילים
            updateAlarmsList()
            checkAndDisableActiveAlarms()

            // כדי לוודא שהאדפטר מתעדכן מיד
            alarmAdapter.notifyDataSetChanged()

            // תזמון הרענון הבא בדיוק במעבר הדקה
            scheduleNextMinuteUpdate()
        }
    }

    // הוסף פונקציה חדשה אחרי updateRunnable:
    private fun scheduleNextMinuteUpdate() {
        // חשב בדיוק כמה זמן נשאר עד הדקה הבאה
        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60000) + 1) * 60000
        val delay = nextMinute - now

        // תזמן את העדכון הבא
        handler.postDelayed(updateRunnable, delay)
    }
    private var audioPickerCallback: ((String?, String?) -> Unit)? = null

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(it)
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // לא כל ה-URIs תומכים בהרשאות קבועות
            }
            audioPickerCallback?.invoke(it.toString(), fileName)
        }
        audioPickerCallback = null
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 100
        private const val PREFS_NAME = "AlarmPrefs"
        private const val PREF_DEFAULT_DURATION = "default_duration"
        private const val PREF_DEFAULT_VOLUME = "default_volume"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

        initializeViews()
        initializeServices()
        setupClickListeners()
        checkPermissions()
        checkDoNotDisturbPermission()
        updateAlarmsList()

        onBackPressedDispatcher.addCallback(this) {
            if (isEditMode) {
                exitEditMode()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        splashScreen.setKeepOnScreenCondition {
            false // תחליף ל־true עד שתסיים לטעון נתונים
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.alarmsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        fabAddAlarm = findViewById(R.id.fabAddAlarm)
        editButton = findViewById(R.id.editButton)
        infoButton = findViewById(R.id.infoButton)
        closeEditButton = findViewById(R.id.closeEditButton)
        selectAllButton = findViewById(R.id.selectAllButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        alarmAdapter = AlarmAdapter(this)
        recyclerView.adapter = alarmAdapter

        // הוספת מרווח בתחתית הרשימה כדי שהפריט האחרון לא יוסתר על ידי ה-FAB
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                val itemCount = state.itemCount
                if (position == itemCount - 1) {
                    outRect.bottom = (120 * resources.displayMetrics.density).toInt()
                }
            }
        })
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

        closeEditButton.setOnClickListener {
            exitEditMode()
        }

        selectAllButton.setOnClickListener {
            toggleSelectAll()
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

    private fun checkDoNotDisturbPermission() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            AlertDialog.Builder(this)
                .setTitle("הרשאה נדרשת")
                .setMessage("כדי שהשעון יצלצל במצב 'נא לא להפריע', נדרשת הרשאה מיוחדת")
                .setPositiveButton("אשר") { _, _ ->
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("לא עכשיו", null)
                .show()
        }
    }

    private fun exitEditMode() {
        isEditMode = false
        alarmAdapter.setEditMode(false)
        updateEditModeUI()
        alarmAdapter.clearSelection()
        allSelected = false
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        alarmAdapter.setEditMode(isEditMode)
        updateEditModeUI()

        if (!isEditMode) {
            alarmAdapter.clearSelection()
            allSelected = false
        }
    }

    private fun updateEditModeUI() {
        if (isEditMode) {
            editButton.visibility = View.GONE
            closeEditButton.visibility = View.VISIBLE
            selectAllButton.visibility = View.VISIBLE

            fabAddAlarm.text = "מחק"
            fabAddAlarm.setIconResource(R.drawable.ic_delete)

            updateSelectAllIcon()
        } else {
            editButton.visibility = View.VISIBLE
            closeEditButton.visibility = View.GONE
            selectAllButton.visibility = View.GONE

            fabAddAlarm.text = "הוסף שעון"
            fabAddAlarm.setIconResource(R.drawable.ic_add)
        }
    }

    private fun toggleSelectAll() {
        if (allSelected) {
            alarmAdapter.clearSelection()
            allSelected = false
        } else {
            alarmAdapter.selectAll()
            allSelected = true
        }
        updateSelectAllIcon()
    }

    private fun updateSelectAllIcon() {
        if (alarmAdapter.isAllSelected()) {
            selectAllButton.setImageResource(R.drawable.ic_deselect_all)
            selectAllButton.contentDescription = "בטל בחירת הכל"
            allSelected = true
        } else {
            selectAllButton.setImageResource(R.drawable.ic_select_all)
            selectAllButton.contentDescription = "בחר הכל"
            allSelected = false
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

    private fun updateAlarmsListCorrectly() {
        handler.postDelayed({
            val alarms = repository.getAllAlarms()
            runOnUiThread {
                alarmAdapter.submitList(alarms) {
                    if (alarms.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyStateText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }, 100)
    }

    private fun checkAndDisableActiveAlarms() {
        val now = LocalDateTime.now()
        val alarms = repository.getAllAlarms()
        var updated = false

        alarms.forEach { alarm ->
            if (alarm.isActive) {
                val alarmTime = alarm.getLocalDateTime()
                // בדיקה מלאה של תאריך ושעה - לא רק שעה ודקה
                if (alarmTime.isBefore(now) ||
                    (alarmTime.toLocalDate() == now.toLocalDate() &&
                     alarmTime.hour == now.hour &&
                     alarmTime.minute == now.minute)) {
                    val updatedAlarm = alarm.copy(isActive = false)
                    repository.updateAlarm(updatedAlarm)
                    updated = true
                }
            }
        }

        if (updated) {
            updateAlarmsList()
        }
    }

    private fun deleteSelectedAlarms() {
        val selectedIds = alarmAdapter.getSelectedAlarmIds()
        if (selectedIds.isNotEmpty()) {
//            AlertDialog.Builder(this)
//                .setTitle("מחיקת שעונים")
//                .setMessage("האם אתה בטוח שברצונך למחוק ${selectedIds.size} שעונים?")
//                .setPositiveButton("מחק") { _, _ ->
                    selectedIds.forEach { id ->
                        val alarm = repository.getAllAlarms().find { it.id == id }
                        alarm?.let {
                            alarmManager.cancelAlarm(it)
                        }
                    }
                    repository.deleteAlarms(selectedIds)
                    updateAlarmsList()
                    toggleEditMode()
                }
//                .setNegativeButton("ביטול", null)
//                .show()
         else {
            Toast.makeText(this, "לא נבחרו שעונים למחיקה", Toast.LENGTH_SHORT).show()
            toggleEditMode()
        }
    }

    private fun showAddEditDialog(alarm: Alarm?) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultDuration = prefs.getInt(PREF_DEFAULT_DURATION, 10)
        val defaultVolume = prefs.getInt(PREF_DEFAULT_VOLUME, 70)

        val bottomSheet = AddEditAlarmBottomSheet(alarm, defaultDuration, defaultVolume) { updatedAlarm ->
            if (alarm == null) {
                Log.d("tott","null")
                // שעון חדש
                prefs.edit()
                    .putInt(PREF_DEFAULT_DURATION, updatedAlarm.durationSeconds)
                    .putInt(PREF_DEFAULT_VOLUME, updatedAlarm.volume)
                    .apply()

                val newAlarm = updatedAlarm.copy(id = repository.getNextAlarmId())
                repository.addAlarm(newAlarm)
                if (newAlarm.isActive) {
                    alarmManager.setAlarm(newAlarm)
                }
            } else {
                Log.d("tott","else")

                alarmManager.cancelAlarm(alarm)


                val finalAlarm = updatedAlarm.copy(
                    id = alarm.id,  // וודא שה-ID נשמר
                    isActive = true
                )

                repository.updateAlarm(finalAlarm)

                // אם השעון היה פעיל, עדכן אותו במערכת
//                if (finalAlarm.isActive) {
                    alarmManager.setAlarm(finalAlarm)
//                }

            }
//            alarmAdapter.notifyDataSetChanged()
            updateAlarmsListCorrectly()
        }
        bottomSheet.show(supportFragmentManager, "AddEditAlarmBottomSheet")
    }
    private fun showInfoDialog() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }



    private fun getFileName(uri: Uri): String {
        var name = "צלצול מותאם אישית"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun openAudioPicker(callback: (String?, String?) -> Unit) {
        audioPickerCallback = callback
        try {
            pickAudioLauncher.launch("audio/*")
        } catch (e: Exception) {
            Toast.makeText(this, "לא ניתן לפתוח בוחר קבצים", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAlarmClick(alarm: Alarm) {
        if (!isEditMode) {
            showAddEditDialog(alarm)
        }
    }

    override fun onAlarmLongClick(alarm: Alarm): Boolean {
        if (!isEditMode) {
            toggleEditMode()
            return true
        }
        return false
    }

    override fun onAlarmToggle(alarm: Alarm, isChecked: Boolean) {
        if (alarm.isActive == isChecked) return

        // חישוב הזמן הבא
        val now = LocalDateTime.now()
        var alarmTime = now
            .withHour(alarm.getLocalDateTime().hour)
            .withMinute(alarm.getLocalDateTime().minute)
            .withSecond(0)
            .withNano(0)

        if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
            alarmTime = alarmTime.plusDays(1)
        }

        val dateTimeString = alarmTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val updatedAlarm = alarm.copy(
            dateTime = dateTimeString,
            isActive = isChecked
        )

        // עדכון Repository וה-AlarmManager
        repository.updateAlarm(updatedAlarm)
        alarmManager.cancelAlarm(alarm)
        if (isChecked) alarmManager.setAlarm(updatedAlarm)

        // עדכון הרשימה בצורה נקייה
        val alarms = repository.getAllAlarms()
        Handler(Looper.getMainLooper()).postDelayed( {
            alarmAdapter.submitList(alarms) {
                alarmAdapter.notifyDataSetChanged()
            }
        },200)
    }

    override fun onResume() {
        super.onResume()

        // ביטול כל הרענונים הקודמים
        handler.removeCallbacks(updateRunnable)

        // עדכון מיידי
        updateAlarmsList()
        checkAndDisableActiveAlarms()

        // תזמון הרענון הבא בדיוק במעבר הדקה
        scheduleNextMinuteUpdate()
        alarmAdapter.notifyDataSetChanged()
    }

    // וודא ש-onPause נשאר כמו שהוא:
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
}