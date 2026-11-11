package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.EssenceDatabase
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER = "com.example.essence.ACTION_REMINDER_TRIGGER"
        const val ACTION_TASK_DONE = "com.example.essence.ACTION_TASK_DONE"
        const val ACTION_TASK_SNOOZE = "com.example.essence.ACTION_TASK_SNOOZE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_INDEX = "extra_reminder_index"
        const val CHANNEL_ID = "channel_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        when (intent.action) {
            ACTION_TRIGGER -> {
                handleTrigger(context, taskId)
            }
            ACTION_TASK_DONE -> {
                handleDone(context, taskId)
            }
            ACTION_TASK_SNOOZE -> {
                handleSnooze(context, taskId)
            }
        }
    }

    private fun handleTrigger(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = EssenceDatabase.getInstance(context)
            val task = db.taskDao().getTaskById(taskId) ?: return@launch
            if (task.isDone || task.isDeleted) return@launch

            createChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO_TASKS", true)
            }
            val openPending = PendingIntent.getActivity(
                context,
                taskId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_TASK_DONE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val donePending = PendingIntent.getBroadcast(
                context,
                (taskId * 10 + 1).toInt(),
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_TASK_SNOOZE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val snoozePending = PendingIntent.getBroadcast(
                context,
                (taskId * 10 + 2).toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_widget_task)
                .setContentTitle(task.title.ifBlank { "Task Reminder" })
                .setContentText(task.notes.ifBlank { "Due now" })
                .setColor(0xFFD71921.toInt())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.checkbox_on_background, "Done", donePending)
                .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePending)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(taskId.toInt(), notification)
        }
    }

    private fun handleDone(context: Context, taskId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(taskId.toInt())

        CoroutineScope(Dispatchers.IO).launch {
            val db = EssenceDatabase.getInstance(context)
            val task = db.taskDao().getTaskById(taskId) ?: return@launch
            if (task.repeatRule != "NONE" && task.dueDate != null) {
                val nextDate = AlarmScheduler.computeNextDueDate(task.dueDate, task.repeatRule)
                val updated = task.copy(
                    dueDate = nextDate,
                    updatedAt = System.currentTimeMillis()
                )
                db.taskDao().updateTask(updated)
                AlarmScheduler.scheduleTaskReminders(context, updated)
            } else {
                db.taskDao().updateTask(
                    task.copy(
                        isDone = true,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun handleSnooze(context: Context, taskId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(taskId.toInt())

        val snoozeTimeMs = System.currentTimeMillis() + (10 * 60 * 1000)
        CoroutineScope(Dispatchers.IO).launch {
            val db = EssenceDatabase.getInstance(context)
            val task = db.taskDao().getTaskById(taskId) ?: return@launch
            val updated = task.copy(
                reminderTimesJson = "[${snoozeTimeMs}]"
            )
            AlarmScheduler.scheduleTaskReminders(context, updated)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
