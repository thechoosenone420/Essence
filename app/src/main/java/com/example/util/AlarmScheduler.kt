package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Task
import com.example.receiver.ReminderReceiver
import java.util.Calendar

object AlarmScheduler {

    fun scheduleTaskReminders(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // Schedule explicit reminder times
        task.getReminderTimes().forEachIndexed { index, triggerTime ->
            if (triggerTime > now) {
                scheduleExactAlarm(context, alarmManager, task.id, triggerTime, index)
            }
        }

        // Schedule based on dueDate & dueTimeMinutes if no explicit reminder times
        if (task.getReminderTimes().isEmpty() && task.dueDate != null) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = task.dueDate
                val minutes = task.dueTimeMinutes ?: (9 * 60) // default 9:00 AM
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis > now) {
                scheduleExactAlarm(context, alarmManager, task.id, cal.timeInMillis, 0)
            }
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        taskId: Long,
        triggerAtMs: Long,
        reminderIndex: Int
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_INDEX, reminderIndex)
        }
        val requestCode = (taskId * 100 + reminderIndex).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } catch (ignored: Exception) {}
        }
    }

    fun cancelTaskReminders(context: Context, taskId: Long, reminderCount: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0..reminderCount) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TRIGGER
                putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
                putExtra(ReminderReceiver.EXTRA_REMINDER_INDEX, i)
            }
            val requestCode = (taskId * 100 + i).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun computeNextDueDate(currentDueDate: Long?, repeatRule: String): Long? {
        if (currentDueDate == null || repeatRule == "NONE") return null
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when (repeatRule) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            "WEEKDAYS" -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            else -> return null
        }
        return cal.timeInMillis
    }
}
