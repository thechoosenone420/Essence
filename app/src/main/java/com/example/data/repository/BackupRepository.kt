package com.example.data.repository

import android.content.Context
import com.example.data.local.NoteDao
import com.example.data.local.TaskDao
import com.example.data.model.Note
import com.example.data.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val context: Context,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao
) {

    suspend fun exportBackup(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val notes = noteDao.getAllNotesDirect()
            val tasks = taskDao.getAllTasksDirect()

            val rootJson = JSONObject().apply {
                put("version", 1)
                put("exportTime", System.currentTimeMillis())

                val notesArray = JSONArray()
                notes.forEach { note ->
                    val obj = JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("body", note.body)
                        put("checklistJson", note.checklistJson)
                        put("tagsJson", note.tagsJson)
                        put("folder", note.folder)
                        put("isPinned", note.isPinned)
                        put("isArchived", note.isArchived)
                        put("isDeleted", note.isDeleted)
                        put("isVoiceNote", note.isVoiceNote)
                        put("audioFileName", note.audioPath?.let { File(it).name } ?: "")
                        put("audioDurationMs", note.audioDurationMs)
                        put("audioAmplitudesJson", note.audioAmplitudesJson)
                        put("transcript", note.transcript)
                        put("transcriptionStatus", note.transcriptionStatus)
                        put("createdAt", note.createdAt)
                        put("updatedAt", note.updatedAt)
                    }
                    notesArray.put(obj)
                }
                put("notes", notesArray)

                val tasksArray = JSONArray()
                tasks.forEach { task ->
                    val obj = JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("notes", task.notes)
                        put("subtasksJson", task.subtasksJson)
                        put("priority", task.priority)
                        put("tagsJson", task.tagsJson)
                        put("project", task.project)
                        put("dueDate", task.dueDate ?: -1L)
                        put("dueTimeMinutes", task.dueTimeMinutes ?: -1)
                        put("isDone", task.isDone)
                        put("completedAt", task.completedAt ?: -1L)
                        put("reminderTimesJson", task.reminderTimesJson)
                        put("repeatRule", task.repeatRule)
                        put("isDeleted", task.isDeleted)
                        put("createdAt", task.createdAt)
                        put("updatedAt", task.updatedAt)
                    }
                    tasksArray.put(obj)
                }
                put("tasks", tasksArray)
            }

            ZipOutputStream(outputStream).use { zipOut ->
                // Write backup.json
                val jsonEntry = ZipEntry("backup.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // Write audio files
                notes.filter { it.isVoiceNote && !it.audioPath.isNullOrBlank() }.forEach { note ->
                    val file = File(note.audioPath!!)
                    if (file.exists()) {
                        val audioEntry = ZipEntry("audio/${file.name}")
                        zipOut.putNextEntry(audioEntry)
                        FileInputStream(file).use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }

            Result.success(notes.size + tasks.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importBackup(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val audioDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            var jsonString = ""
            var importedCount = 0

            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == "backup.json") {
                        jsonString = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    } else if (entry.name.startsWith("audio/")) {
                        val fileName = entry.name.removePrefix("audio/")
                        if (fileName.isNotEmpty()) {
                            val outFile = File(audioDir, fileName)
                            FileOutputStream(outFile).use { fileOut ->
                                zipIn.copyTo(fileOut)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (jsonString.isBlank()) {
                return@withContext Result.failure(Exception("Invalid backup ZIP: backup.json missing"))
            }

            val rootJson = JSONObject(jsonString)
            val notesArray = rootJson.optJSONArray("notes") ?: JSONArray()
            val tasksArray = rootJson.optJSONArray("tasks") ?: JSONArray()

            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                val audioFileName = obj.optString("audioFileName", "")
                val audioPath = if (audioFileName.isNotEmpty()) {
                    File(audioDir, audioFileName).absolutePath
                } else null

                val note = Note(
                    id = 0, // auto-generate fresh IDs
                    title = obj.optString("title", "Untitled Note"),
                    body = obj.optString("body", ""),
                    checklistJson = obj.optString("checklistJson", ""),
                    tagsJson = obj.optString("tagsJson", ""),
                    folder = obj.optString("folder", "General"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    isVoiceNote = obj.optBoolean("isVoiceNote", false),
                    audioPath = audioPath,
                    audioDurationMs = obj.optLong("audioDurationMs", 0L),
                    audioAmplitudesJson = obj.optString("audioAmplitudesJson", ""),
                    transcript = obj.optString("transcript", ""),
                    transcriptionStatus = obj.optString("transcriptionStatus", "NONE"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                noteDao.insertNote(note)
                importedCount++
            }

            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val dueDate = obj.optLong("dueDate", -1L)
                val dueTime = obj.optInt("dueTimeMinutes", -1)
                val completedAt = obj.optLong("completedAt", -1L)

                val task = Task(
                    id = 0,
                    title = obj.optString("title", "Untitled Task"),
                    notes = obj.optString("notes", ""),
                    subtasksJson = obj.optString("subtasksJson", ""),
                    priority = obj.optInt("priority", 3),
                    tagsJson = obj.optString("tagsJson", ""),
                    project = obj.optString("project", "Inbox"),
                    dueDate = if (dueDate > 0) dueDate else null,
                    dueTimeMinutes = if (dueTime >= 0) dueTime else null,
                    isDone = obj.optBoolean("isDone", false),
                    completedAt = if (completedAt > 0) completedAt else null,
                    reminderTimesJson = obj.optString("reminderTimesJson", ""),
                    repeatRule = obj.optString("repeatRule", "NONE"),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                taskDao.insertTask(task)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
