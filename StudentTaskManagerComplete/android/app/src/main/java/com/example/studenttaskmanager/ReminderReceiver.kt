package com.example.studenttaskmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "deadline_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Deadline reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for upcoming school task deadlines"
                }
            )
        }

        val title = intent.getStringExtra("task_title") ?: "School task due"
        val subject = intent.getStringExtra("task_subject").orEmpty()
        val openApp = PendingIntent.getActivity(
            context,
            1001,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Deadline reminder: $title")
            .setContentText(if (subject.isBlank()) "Your task is due today." else "$subject • Your task is due today.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify((intent.getLongExtra("task_id", System.currentTimeMillis())).hashCode(), notification)
        } catch (_: SecurityException) {
            // Notifications may be disabled by the user in Android settings.
        }
    }
}
