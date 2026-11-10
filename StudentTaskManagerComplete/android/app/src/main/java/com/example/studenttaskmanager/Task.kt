package com.example.studenttaskmanager

data class Task(
    val id: Long,
    val title: String,
    val subject: String,
    val dueMillis: Long,
    var completed: Boolean = false
)
