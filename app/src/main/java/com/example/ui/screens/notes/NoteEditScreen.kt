package com.example.ui.screens.notes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EssenceApplication
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Task
import com.example.ui.theme.DotMatrixHeading
import com.example.ui.theme.DottedDivider
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingBorderSubtle
import com.example.ui.theme.NothingButton
import com.example.ui.theme.NothingCard
import com.example.ui.theme.NothingDarkCard
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGrayDark
import com.example.ui.theme.NothingGrayLight
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingTag
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.theme.Typography
import com.example.ui.theme.rememberTactileHaptic
import com.example.util.AudioPlayerHelper
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.util.Locale

@Composable
fun NoteEditScreen(
    noteId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberTactileHaptic()
    val scope = rememberCoroutineScope()
    val app = EssenceApplication.instance
    val noteRepo = app.noteRepository
    val taskRepo = app.taskRepository
    val audioPlayer = remember { AudioPlayerHelper() }

    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("General") }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }

    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }
    var newChecklistText by remember { mutableStateOf("") }

    val tags = remember { mutableStateListOf<String>() }
    var newTagText by remember { mutableStateOf("") }
    var isAddingTag by remember { mutableStateOf(false) }

    var isTranscribing by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }
    var isExtractingTasks by remember { mutableStateOf(false) }
    var extractedTasks by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load Note
    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val loaded = noteRepo.getNoteById(noteId)
            if (loaded != null) {
                note = loaded
                title = loaded.title
                body = loaded.body
                folder = loaded.folder
                isPinned = loaded.isPinned
                isArchived = loaded.isArchived
                transcript = loaded.transcript
                checklistItems.clear()
                checklistItems.addAll(loaded.getChecklist())
                tags.clear()
                tags.addAll(loaded.getTags())

                if (loaded.isVoiceNote && !loaded.audioPath.isNullOrBlank()) {
                    audioPlayer.load(loaded.audioPath)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    fun saveNote() {
        scope.launch {
            val currentNote = note
            val tagsJson = JSONArray(tags.toList()).toString()
            val checklistJson = ChecklistItem.encodeList(checklistItems.toList())

            if (currentNote != null) {
                val updated = currentNote.copy(
                    title = title.ifBlank { if (currentNote.isVoiceNote) "Voice Note" else "Untitled Note" },
                    body = body,
                    checklistJson = checklistJson,
                    tagsJson = tagsJson,
                    folder = folder,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    transcript = transcript
                )
                noteRepo.updateNote(updated)
            } else {
                val newNote = Note(
                    title = title.ifBlank { "Untitled Note" },
                    body = body,
                    checklistJson = checklistJson,
                    tagsJson = tagsJson,
                    folder = folder,
                    isPinned = isPinned,
                    isArchived = isArchived
                )
                noteRepo.insertNote(newNote)
            }
        }
    }

    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPos by audioPlayer.currentPositionMs.collectAsState()
    val duration by audioPlayer.durationMs.collectAsState()
    val speed by audioPlayer.playbackSpeed.collectAsState()

    Scaffold(
        containerColor = NothingBlack,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    saveNote()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NothingWhite)
                }
                DotMatrixHeading(
                    text = if (note?.isVoiceNote == true) "VOICE NOTE" else "NOTE",
                    accentDot = note?.isVoiceNote == true,
                    fontSize = 18
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (isPinned) NothingWhite else NothingGray
                    )
                }
                IconButton(onClick = {
                    saveNote()
                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = NothingRed)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Folder and Tag row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NothingDarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = NothingGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = folder.uppercase(),
                            style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 11.sp)
                        )
                    }
                }

                tags.forEach { tag ->
                    NothingTag(
                        text = tag,
                        onClick = { tags.remove(tag) }
                    )
                }

                if (!isAddingTag) {
                    Surface(
                        color = NothingDarkCard,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorderSubtle),
                        onClick = { isAddingTag = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NothingGray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TAG", style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            textStyle = Typography.bodySmall.copy(color = NothingWhite),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NothingWhite,
                                unfocusedBorderColor = NothingBorder
                            )
                        )
                        IconButton(onClick = {
                            if (newTagText.isNotBlank()) {
                                tags.add(newTagText.trim())
                                newTagText = ""
                            }
                            isAddingTag = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Add", tint = NothingWhite, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_edit_title"),
                placeholder = {
                    Text(
                        "Title...",
                        style = Typography.headlineMedium.copy(color = NothingGray, fontFamily = SpaceMonoFontFamily)
                    )
                },
                textStyle = Typography.headlineMedium.copy(color = NothingWhite, fontFamily = SpaceMonoFontFamily),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            // Voice Player & Transcript Section if Voice Note
            if (note?.isVoiceNote == true) {
                Spacer(modifier = Modifier.height(8.dp))
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NothingRed
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = NothingRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUDIO PLAYBACK",
                                style = Typography.labelMedium.copy(color = NothingWhite, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NothingDarkCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder),
                                onClick = { audioPlayer.cycleSpeed() }
                            ) {
                                Text(
                                    text = "${speed}X",
                                    style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 11.sp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Seek bar
                        val progress = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f
                        Slider(
                            value = progress,
                            onValueChange = { frac ->
                                audioPlayer.seekTo((frac * duration).toInt())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NothingRed,
                                activeTrackColor = NothingRed,
                                inactiveTrackColor = NothingBorder
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val curSec = currentPos / 1000
                            val durSec = duration / 1000
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", curSec / 60, curSec % 60),
                                style = Typography.bodySmall.copy(fontSize = 11.sp)
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", durSec / 60, durSec % 60),
                                style = Typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls: Play/Pause, Re-transcribe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                                    .clickable { audioPlayer.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = NothingWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transcript
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DotMatrixHeading(text = "TRANSCRIPT", fontSize = 14)
                    Spacer(modifier = Modifier.weight(1f))
                    if (isTranscribing) {
                        CircularProgressIndicator(color = NothingRed, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NothingDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder),
                            onClick = {
                                val audioPath = note?.audioPath
                                if (!audioPath.isNullOrBlank()) {
                                    val file = File(audioPath)
                                    if (file.exists()) {
                                        isTranscribing = true
                                        scope.launch {
                                            val res = app.transcriptionService.transcribeAudio(file)
                                            res.onSuccess {
                                                transcript = it
                                                saveNote()
                                                Toast.makeText(context, "Transcribed successfully", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isTranscribing = false
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RE-TRANSCRIBE", style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 9.sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Transcript box (tap to seek)
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NothingDarkCard
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (transcript.isNotBlank()) {
                            Text(
                                text = transcript,
                                style = Typography.bodyMedium.copy(color = NothingWhite, lineHeight = 20.sp),
                                modifier = Modifier.clickable {
                                    audioPlayer.play()
                                }
                            )
                        } else {
                            Text(
                                text = if (isTranscribing) "Transcribing audio..." else "No transcript available. Tap 'RE-TRANSCRIBE' to generate.",
                                style = Typography.bodySmall.copy(color = NothingGray)
                            )
                        }
                    }
                }

                // AI Actions row: Summarize, Extract To-Dos
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingButton(
                        text = if (isSummarizing) "SUMMARIZING..." else "SUMMARIZE",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.weight(1f),
                        enabled = !isSummarizing && transcript.isNotBlank(),
                        onClick = {
                            isSummarizing = true
                            scope.launch {
                                val res = app.transcriptionService.summarizeText(transcript)
                                res.onSuccess { summary ->
                                    body = if (body.isBlank()) "SUMMARY:\n$summary" else "$body\n\nSUMMARY:\n$summary"
                                    saveNote()
                                    Toast.makeText(context, "Summary added to note body", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                }
                                isSummarizing = false
                            }
                        }
                    )

                    NothingButton(
                        text = if (isExtractingTasks) "EXTRACTING..." else "EXTRACT TO-DOS",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.weight(1f),
                        enabled = !isExtractingTasks && transcript.isNotBlank(),
                        onClick = {
                            isExtractingTasks = true
                            scope.launch {
                                val res = app.transcriptionService.extractTodos(transcript)
                                res.onSuccess { tasksList ->
                                    extractedTasks = tasksList
                                    // Add to checklist or add to task repo
                                    tasksList.forEach { taskTitle ->
                                        taskRepo.insertTask(
                                            Task(
                                                title = taskTitle,
                                                notes = "Extracted from voice note: $title",
                                                project = "Voice Tasks"
                                            )
                                        )
                                    }
                                    Toast.makeText(context, "Added ${tasksList.size} tasks to To-Dos!", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                }
                                isExtractingTasks = false
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Body Text Field
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_edit_body"),
                placeholder = {
                    Text(
                        "Write your thoughts, ideas, or notes here...",
                        style = Typography.bodyMedium.copy(color = NothingGray)
                    )
                },
                textStyle = Typography.bodyMedium.copy(color = NothingWhite),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            // Checklist section
            Spacer(modifier = Modifier.height(16.dp))
            DotMatrixHeading(text = "CHECKLIST", fontSize = 14)
            Spacer(modifier = Modifier.height(8.dp))

            checklistItems.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { checked ->
                            checklistItems[index] = item.copy(isChecked = checked)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NothingRed,
                            uncheckedColor = NothingGray,
                            checkmarkColor = NothingWhite
                        )
                    )
                    Text(
                        text = item.text,
                        style = Typography.bodyMedium.copy(
                            color = if (item.isChecked) NothingGrayDark else NothingWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { checklistItems.removeAt(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete Item", tint = NothingGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Add checklist item field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newChecklistText,
                    onValueChange = { newChecklistText = it },
                    placeholder = { Text("Add checklist item...", style = Typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingWhite,
                        unfocusedBorderColor = NothingBorder,
                        focusedContainerColor = NothingDarkCard,
                        unfocusedContainerColor = NothingDarkCard,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newChecklistText.isNotBlank()) {
                            checklistItems.add(ChecklistItem(text = newChecklistText.trim()))
                            newChecklistText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = NothingWhite)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
