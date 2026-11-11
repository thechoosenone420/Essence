package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.EssenceDatabase
import com.example.data.model.Note
import com.example.data.preferences.AppPreferences
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecordService : Service() {

    companion object {
        const val ACTION_START = "com.example.essence.ACTION_RECORD_START"
        const val ACTION_STOP = "com.example.essence.ACTION_RECORD_STOP"
        const val ACTION_PAUSE = "com.example.essence.ACTION_RECORD_PAUSE"
        const val ACTION_RESUME = "com.example.essence.ACTION_RECORD_RESUME"

        const val CHANNEL_ID = "channel_recording"
        const val NOTIFICATION_ID = 2001

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        private val _durationMs = MutableStateFlow(0L)
        val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

        private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
        val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, AudioRecordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioRecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun togglePause(context: Context) {
            val intent = Intent(context, AudioRecordService::class.java).apply {
                action = if (_isPaused.value) ACTION_RESUME else ACTION_PAUSE
            }
            context.startService(intent)
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private val recordedAmplitudes = mutableListOf<Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            else -> {
                if (!_isRecording.value) {
                    startRecording()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_recording_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_recording_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPausedState: Boolean, durationSec: Long): Notification {
        val stopIntent = Intent(this, AudioRecordService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, AudioRecordService::class.java).apply {
            action = if (isPausedState) ACTION_RESUME else ACTION_PAUSE
        }
        val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val min = durationSec / 60
        val sec = durationSec % 60
        val timeString = String.format(Locale.US, "%02d:%02d", min, sec)

        val title = if (isPausedState) "RECORDING PAUSED // $timeString" else "RECORDING // $timeString"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Essence Voice Capture • Screen Off Safe")
            .setSmallIcon(R.drawable.ic_widget_mic)
            .setColor(0xFFD71921.toInt())
            .setContentIntent(openAppPending)
            .setOngoing(true)
            .addAction(
                if (isPausedState) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPausedState) "Resume" else "Pause",
                pausePending
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Save", stopPending)
            .build()
    }

    private fun startRecording() {
        if (_isRecording.value) return

        try {
            val audioDir = File(filesDir, "voice_notes").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            outputFile = File(audioDir, "essence_rec_$timestamp.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            startTimeMs = System.currentTimeMillis()
            accumulatedDurationMs = 0L
            recordedAmplitudes.clear()

            _isRecording.value = true
            _isPaused.value = false
            _durationMs.value = 0L
            _amplitudes.value = emptyList()

            val notification = buildNotification(false, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            startTicker()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && _isRecording.value) {
                delay(100)
                if (!_isPaused.value) {
                    val currentElapsed = accumulatedDurationMs + (System.currentTimeMillis() - startTimeMs)
                    _durationMs.value = currentElapsed

                    val amp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalizedAmp = (amp / 32767f * 100).toInt().coerceIn(2, 100)
                    recordedAmplitudes.add(normalizedAmp)
                    _amplitudes.value = recordedAmplitudes.takeLast(60)

                    // Update notification every second
                    if ((currentElapsed / 100) % 10 == 0L) {
                        val notification = buildNotification(false, currentElapsed / 1000)
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(NOTIFICATION_ID, notification)
                    }
                }
            }
        }
    }

    private fun pauseRecording() {
        if (!_isRecording.value || _isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
            accumulatedDurationMs += (System.currentTimeMillis() - startTimeMs)
            pauseStartTimeMs = System.currentTimeMillis()
            _isPaused.value = true

            val notification = buildNotification(true, accumulatedDurationMs / 1000)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resumeRecording() {
        if (!_isRecording.value || !_isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
            startTimeMs = System.currentTimeMillis()
            _isPaused.value = false

            val notification = buildNotification(false, accumulatedDurationMs / 1000)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        tickerJob?.cancel()
        val finalDuration = if (_isPaused.value) {
            accumulatedDurationMs
        } else {
            accumulatedDurationMs + (System.currentTimeMillis() - startTimeMs)
        }

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null

        _isRecording.value = false
        _isPaused.value = false
        _durationMs.value = 0L

        stopForeground(STOP_FOREGROUND_REMOVE)

        val file = outputFile
        if (file != null && file.exists() && file.length() > 0) {
            saveNoteAndTranscribe(file, finalDuration, recordedAmplitudes.toList())
        }

        stopSelf()
    }

    private fun saveNoteAndTranscribe(audioFile: File, durationMs: Long, amps: List<Int>) {
        val appCtx = applicationContext
        serviceScope.launch(Dispatchers.IO) {
            val db = EssenceDatabase.getInstance(appCtx)
            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
            val amplitudesJson = JSONArray(amps).toString()

            val note = Note(
                title = "Voice Note $dateStr",
                body = "",
                folder = "Voice Notes",
                isVoiceNote = true,
                audioPath = audioFile.absolutePath,
                audioDurationMs = durationMs,
                audioAmplitudesJson = amplitudesJson,
                transcript = "",
                transcriptionStatus = "PENDING"
            )

            val noteId = db.noteDao().insertNote(note)
            WidgetUpdater.updateAllWidgets(appCtx)

            // Trigger transcription
            val appPreferences = AppPreferences(appCtx)
            if (appPreferences.isTranscriptionEnabled.value) {
                val transcriptionService = TranscriptionService(appCtx, appPreferences)
                val result = transcriptionService.transcribeAudio(audioFile)
                result.onSuccess { transcriptText ->
                    val updatedNote = note.copy(
                        id = noteId,
                        transcript = transcriptText,
                        transcriptionStatus = "COMPLETED"
                    )
                    db.noteDao().updateNote(updatedNote)
                    WidgetUpdater.updateAllWidgets(appCtx)
                }.onFailure {
                    val updatedNote = note.copy(
                        id = noteId,
                        transcript = "Transcription unavailable.",
                        transcriptionStatus = "FAILED"
                    )
                    db.noteDao().updateNote(updatedNote)
                }
            } else {
                val updatedNote = note.copy(
                    id = noteId,
                    transcriptionStatus = "NONE"
                )
                db.noteDao().updateNote(updatedNote)
            }
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaRecorder = null
        _isRecording.value = false
        _isPaused.value = false
        super.onDestroy()
    }
}
