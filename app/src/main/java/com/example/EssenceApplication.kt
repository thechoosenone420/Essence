package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.EssenceDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.repository.BackupRepository
import com.example.data.repository.NoteRepository
import com.example.data.repository.TaskRepository
import com.example.service.AudioRecordService
import com.example.service.TranscriptionService
import com.example.widget.WidgetUpdater

class EssenceApplication : Application() {

    lateinit var database: EssenceDatabase
        private set
    lateinit var noteRepository: NoteRepository
        private set
    lateinit var taskRepository: TaskRepository
        private set
    lateinit var backupRepository: BackupRepository
        private set
    lateinit var appPreferences: AppPreferences
        private set
    lateinit var transcriptionService: TranscriptionService
        private set

    companion object {
        lateinit var instance: EssenceApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = EssenceDatabase.getInstance(this)
        noteRepository = NoteRepository(database.noteDao())
        taskRepository = TaskRepository(database.taskDao())
        backupRepository = BackupRepository(this, database.noteDao(), database.taskDao())
        appPreferences = AppPreferences(this)
        transcriptionService = TranscriptionService(this, appPreferences)

        createNotificationChannels()
        WidgetUpdater.updateAllWidgets(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val recordingChannel = NotificationChannel(
                AudioRecordService.CHANNEL_ID,
                getString(R.string.channel_recording_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_recording_desc)
                setShowBadge(false)
            }

            val reminderChannel = NotificationChannel(
                "channel_reminders",
                getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_reminders_desc)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(recordingChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
}
