package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.EssenceDatabase
import com.example.util.AlarmScheduler
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            WidgetUpdater.updateAllWidgets(context)

            CoroutineScope(Dispatchers.IO).launch {
                val db = EssenceDatabase.getInstance(context)
                val tasks = db.taskDao().getTasksWithReminders()
                tasks.forEach { task ->
                    AlarmScheduler.scheduleTaskReminders(context, task)
                }
            }
        }
    }
}
