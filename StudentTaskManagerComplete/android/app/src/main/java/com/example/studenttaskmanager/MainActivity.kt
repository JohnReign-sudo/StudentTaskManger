package com.example.studenttaskmanager

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val tasks = mutableListOf<Task>()
    private var selectedDueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var selectedCalendarDate = dayStart(System.currentTimeMillis())
    private var darkMode = false
    private lateinit var root: LinearLayout
    private lateinit var taskInput: EditText
    private lateinit var subjectInput: EditText
    private lateinit var dueButton: Button
    private lateinit var taskListContainer: LinearLayout
    private lateinit var calendarView: CalendarView
    private lateinit var upcomingContainer: LinearLayout
    private lateinit var darkSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasks.addAll(TaskStore.load(this))
        darkMode = TaskStore.isDark(this)
        requestNotificationPermission()
        buildScreen()
        renderTasks()
    }

    private fun buildScreen() {
        val bg = if (darkMode) Color.rgb(24, 26, 32) else Color.rgb(247, 248, 252)
        val fg = if (darkMode) Color.WHITE else Color.rgb(30, 35, 45)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))

        content.addView(TextView(this).apply {
            text = "Student Task Manager"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(fg)
        })
        content.addView(TextView(this).apply {
            text = "Keep your school tasks and deadlines organized."
            textSize = 14f
            setTextColor(if (darkMode) Color.LTGRAY else Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(12))
        })

        val dashboardPrefs = getSharedPreferences("student_task_manager", MODE_PRIVATE)
        val todayKey = java.time.LocalDate.now().toString()
        val streak = dashboardPrefs.getInt("study_streak", 0)
        val motivation = listOf(
            "Small progress every day adds up.",
            "Focus on the next task, not every task at once.",
            "Your effort today helps your future self.",
            "A short study session is better than waiting for perfect motivation.",
            "Keep going: learning takes practice.",
            "Take breaks, stay consistent, and ask for help when needed.",
            "You do not need to be perfect; you need to keep learning."
        )[java.time.LocalDate.now().dayOfYear % 7]
        content.addView(TextView(this).apply {
            text = "DASHBOARD\nStudy streak: $streak day(s)\n\nDaily motivation: $motivation"
            textSize = 16f
            setTextColor(fg)
            setPadding(0, dp(14), 0, dp(8))
        })
        content.addView(Button(this).apply {
            text = "I studied today"
            setOnClickListener {
                val prefs = getSharedPreferences("student_task_manager", MODE_PRIVATE)
                val last = prefs.getString("last_study_day", "") ?: ""
                var current = prefs.getInt("study_streak", 0)
                val yesterday = java.time.LocalDate.now().minusDays(1).toString()
                if (last != todayKey) {
                    current = if (last == yesterday) current + 1 else 1
                    prefs.edit().putString("last_study_day", todayKey).putInt("study_streak", current).apply()
                    Toast.makeText(this@MainActivity, "Study day recorded! Streak: $current", Toast.LENGTH_SHORT).show()
                    buildScreen()
                    renderTasks()
                } else {
                    Toast.makeText(this@MainActivity, "Today's study day is already recorded.", Toast.LENGTH_SHORT).show()
                }
            }
        }, matchWrap())

        darkSwitch = Switch(this).apply {
            text = "Dark mode"
            textSize = 15f
            setTextColor(fg)
            isChecked = darkMode
            setOnCheckedChangeListener { _, checked ->
                darkMode = checked
                TaskStore.setDark(this@MainActivity, checked)
                buildScreen()
                renderTasks()
            }
        }
        content.addView(darkSwitch)

        content.addView(Button(this).apply {
            text = "AI Quiz Generator — upload a review module"
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, QuizActivity::class.java))
            }
        }, matchWrap())

        addSectionTitle(content, "Add a task")
        taskInput = EditText(this).apply {
            hint = "Task name (e.g. ICT project)"
            textSize = 16f
            setSingleLine(true)
            setTextColor(fg)
            setHintTextColor(if (darkMode) Color.LTGRAY else Color.GRAY)
        }
        content.addView(taskInput, matchWrap())
        subjectInput = EditText(this).apply {
            hint = "Subject (optional)"
            textSize = 16f
            setSingleLine(true)
            setTextColor(fg)
            setHintTextColor(if (darkMode) Color.LTGRAY else Color.GRAY)
        }
        content.addView(subjectInput, matchWrap())

        dueButton = Button(this).apply {
            text = "Due: ${formatDate(selectedDueDate.timeInMillis)}"
            setOnClickListener { showDatePicker() }
        }
        content.addView(dueButton, matchWrap())

        content.addView(Button(this).apply {
            text = "＋ Add task"
            setOnClickListener { addTask() }
        }, matchWrap())

        addSectionTitle(content, "Calendar")
        calendarView = CalendarView(this).apply {
            date = selectedCalendarDate
            setOnDateChangeListener { _, year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedCalendarDate = cal.timeInMillis
                renderTasks()
            }
        }
        content.addView(calendarView, LinearLayout.LayoutParams(-1, dp(300)))

        addSectionTitle(content, "Tasks on selected date")
        taskListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(taskListContainer, matchWrap())

        addSectionTitle(content, "Upcoming deadlines")
        upcomingContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(upcomingContainer, matchWrap())

        setContentView(root)
    }

    private fun addTask() {
        val title = taskInput.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task name.", Toast.LENGTH_SHORT).show()
            return
        }
        val task = Task(
            id = System.currentTimeMillis(),
            title = title,
            subject = subjectInput.text.toString().trim(),
            dueMillis = selectedDueDate.timeInMillis
        )
        tasks.add(task)
        TaskStore.save(this, tasks)
        ReminderScheduler.schedule(this, task)
        taskInput.setText("")
        subjectInput.setText("")
        selectedCalendarDate = dayStart(task.dueMillis)
        calendarView.date = selectedCalendarDate
        Toast.makeText(this, "Task added. A reminder was scheduled.", Toast.LENGTH_SHORT).show()
        renderTasks()
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDueDate = Calendar.getInstance().apply {
                    set(year, month, day, 8, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dueButton.text = "Due: ${formatDate(selectedDueDate.timeInMillis)}"
            },
            selectedDueDate.get(Calendar.YEAR),
            selectedDueDate.get(Calendar.MONTH),
            selectedDueDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderTasks() {
        if (!::taskListContainer.isInitialized || !::upcomingContainer.isInitialized) return
        taskListContainer.removeAllViews()
        upcomingContainer.removeAllViews()

        val selectedDayTasks = tasks.filter { dayStart(it.dueMillis) == selectedCalendarDate }
            .sortedBy { it.dueMillis }
        if (selectedDayTasks.isEmpty()) {
            taskListContainer.addView(label("No tasks on this date yet."))
        } else {
            selectedDayTasks.forEach { task -> taskListContainer.addView(taskRow(task)) }
        }

        val upcoming = tasks.filter { !it.completed && it.dueMillis >= dayStart(System.currentTimeMillis()) }
            .sortedBy { it.dueMillis }
            .take(10)
        if (upcoming.isEmpty()) {
            upcomingContainer.addView(label("No upcoming deadlines. You're all caught up!"))
        } else {
            upcoming.forEach { task ->
                upcomingContainer.addView(label("• ${task.title} — ${formatDate(task.dueMillis)}" +
                    if (task.subject.isBlank()) "" else " (${task.subject})"))
            }
        }
    }

    private fun taskRow(task: Task): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(if (darkMode) Color.rgb(42, 45, 54) else Color.WHITE)
        }
        val check = CheckBox(this).apply {
            text = task.title
            textSize = 16f
            isChecked = task.completed
            setTextColor(if (darkMode) Color.WHITE else Color.rgb(30, 35, 45))
            setOnCheckedChangeListener { _, checked ->
                task.completed = checked
                TaskStore.save(this@MainActivity, tasks)
                if (checked) ReminderScheduler.cancel(this@MainActivity, task)
                renderTasks()
            }
        }
        row.addView(check)
        row.addView(label("${if (task.subject.isBlank()) "No subject" else task.subject} • Due ${formatDate(task.dueMillis)}"))
        row.addView(Button(this).apply {
            text = "Delete task"
            setOnClickListener {
                ReminderScheduler.cancel(this@MainActivity, task)
                tasks.removeAll { it.id == task.id }
                TaskStore.save(this@MainActivity, tasks)
                renderTasks()
            }
        })
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(8)
        row.layoutParams = params
        return row
    }

    private fun addSectionTitle(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (darkMode) Color.WHITE else Color.rgb(32, 48, 75))
            setPadding(0, dp(18), 0, dp(8))
        })
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(if (darkMode) Color.LTGRAY else Color.DKGRAY)
        setPadding(dp(4), dp(6), dp(4), dp(6))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
        }
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    private fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
