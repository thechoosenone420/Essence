package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.EssenceDatabase
import com.example.service.TransparentRecordActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            val db = EssenceDatabase.getInstance(context)
            val notes = db.noteDao().getAllNotesDirect().filter { !it.isDeleted && !it.isArchived }
            val tasks = db.taskDao().getAllTasksDirect().filter { !it.isDeleted && !it.isDone }

            val statusText = "${notes.size} NOTES  •  ${tasks.size} TASKS"
            val latestTitle = notes.firstOrNull()?.title?.ifBlank { "Voice Note" } ?: "Ready to capture"

            // Update 2x2
            val comp2x2 = ComponentName(context, EssenceWidget2x2::class.java)
            val ids2x2 = appWidgetManager.getAppWidgetIds(comp2x2)
            for (id in ids2x2) {
                val views = RemoteViews(context.packageName, R.layout.widget_2x2)
                attachPendingIntents(context, views, false)
                appWidgetManager.updateAppWidget(id, views)
            }

            // Update 4x2
            val comp4x2 = ComponentName(context, EssenceWidget4x2::class.java)
            val ids4x2 = appWidgetManager.getAppWidgetIds(comp4x2)
            for (id in ids4x2) {
                val views = RemoteViews(context.packageName, R.layout.widget_4x2)
                views.setTextViewText(R.id.widget_4x2_status_title, statusText)
                views.setTextViewText(R.id.widget_4x2_status_body, latestTitle)
                attachPendingIntents(context, views, true)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    private fun attachPendingIntents(context: Context, views: RemoteViews, is4x2: Boolean) {
        // Record intent launches TransparentRecordActivity
        val recordIntent = Intent(context, TransparentRecordActivity::class.java).apply {
            action = "com.example.essence.ACTION_START_RECORDING"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val recordPending = PendingIntent.getActivity(
            context,
            1001,
            recordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // New note intent
        val noteIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.essence.ACTION_NEW_NOTE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notePending = PendingIntent.getActivity(
            context,
            1002,
            noteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // New task intent
        val taskIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.essence.ACTION_NEW_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val taskPending = PendingIntent.getActivity(
            context,
            1003,
            taskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (is4x2) {
            views.setOnClickPendingIntent(R.id.widget_4x2_btn_record, recordPending)
            views.setOnClickPendingIntent(R.id.widget_4x2_btn_note, notePending)
            views.setOnClickPendingIntent(R.id.widget_4x2_btn_task, taskPending)
        } else {
            views.setOnClickPendingIntent(R.id.widget_btn_record, recordPending)
            views.setOnClickPendingIntent(R.id.widget_btn_note, notePending)
            views.setOnClickPendingIntent(R.id.widget_btn_task, taskPending)
        }
    }
}
