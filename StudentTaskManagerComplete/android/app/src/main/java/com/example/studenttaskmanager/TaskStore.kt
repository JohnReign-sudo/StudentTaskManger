package com.example.studenttaskmanager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TaskStore {
    private const val PREFS = "student_task_manager"
    private const val TASKS = "tasks"
    private const val DARK = "dark_mode"

    fun load(context: Context): MutableList<Task> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TASKS, "[]") ?: "[]"
        val result = mutableListOf<Task>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result.add(
                    Task(
                        id = item.getLong("id"),
                        title = item.getString("title"),
                        subject = item.optString("subject", ""),
                        dueMillis = item.getLong("dueMillis"),
                        completed = item.optBoolean("completed", false)
                    )
                )
            }
        } catch (_: Exception) {
            // If stored data is damaged, start with an empty list rather than crashing.
        }
        return result
    }

    fun save(context: Context, tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("subject", task.subject)
                    .put("dueMillis", task.dueMillis)
                    .put("completed", task.completed)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(TASKS, array.toString()).apply()
    }

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(DARK, false)

    fun setDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(DARK, dark).apply()
    }
}
