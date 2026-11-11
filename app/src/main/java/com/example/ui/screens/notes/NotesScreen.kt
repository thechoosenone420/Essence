package com.example.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EssenceApplication
import com.example.data.model.Note
import com.example.ui.theme.DotMatrixHeading
import com.example.ui.theme.DottedDivider
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingBorderSubtle
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NoteFilter {
    ALL, PINNED, VOICE, CHECKLISTS, ARCHIVE, TRASH
}

@Composable
fun NotesScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToNewNote: () -> Unit
) {
    val haptic = rememberTactileHaptic()
    val scope = rememberCoroutineScope()
    val noteRepo = EssenceApplication.instance.noteRepository

    val activeNotes by noteRepo.activeNotes.collectAsState(initial = emptyList())
    val archivedNotes by noteRepo.archivedNotes.collectAsState(initial = emptyList())
    val trashNotes by noteRepo.trashNotes.collectAsState(initial = emptyList())

    var currentFilter by remember { mutableStateOf(NoteFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val rawList = when (currentFilter) {
        NoteFilter.ALL -> activeNotes
        NoteFilter.PINNED -> activeNotes.filter { it.isPinned }
        NoteFilter.VOICE -> activeNotes.filter { it.isVoiceNote }
        NoteFilter.CHECKLISTS -> activeNotes.filter { it.getChecklist().isNotEmpty() }
        NoteFilter.ARCHIVE -> archivedNotes
        NoteFilter.TRASH -> trashNotes
    }

    val filteredList = rawList.filter { note ->
        val matchesFolder = selectedFolder == null || note.folder.equals(selectedFolder, ignoreCase = true)
        val matchesSearch = if (searchQuery.isBlank()) true else {
            note.title.contains(searchQuery, ignoreCase = true) ||
                    note.body.contains(searchQuery, ignoreCase = true) ||
                    note.transcript.contains(searchQuery, ignoreCase = true) ||
                    note.tagsJson.contains(searchQuery, ignoreCase = true)
        }
        matchesFolder && matchesSearch
    }

    val folders = activeNotes.map { it.folder }.filter { it.isNotBlank() }.distinct()

    Scaffold(
        containerColor = NothingBlack,
        floatingActionButton = {
            if (currentFilter != NoteFilter.TRASH) {
                FloatingActionButton(
                    onClick = {
                        haptic()
                        onNavigateToNewNote()
                    },
                    containerColor = NothingRed,
                    contentColor = NothingWhite,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_new_note")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Note", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DotMatrixHeading(
                    text = when (currentFilter) {
                        NoteFilter.ARCHIVE -> "ARCHIVE"
                        NoteFilter.TRASH -> "TRASH"
                        else -> "ESSENCE // NOTES"
                    },
                    accentDot = true,
                    fontSize = 22
                )
                Text(
                    text = "${filteredList.size} ITEMS",
                    style = Typography.labelSmall.copy(color = NothingGray, letterSpacing = 1.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_notes_input"),
                placeholder = {
                    Text("Search notes & voice transcripts...", style = Typography.bodySmall.copy(color = NothingGray))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NothingGray, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = NothingGray, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingBorder,
                    focusedContainerColor = NothingDarkCard,
                    unfocusedContainerColor = NothingDarkCard,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                ),
                shape = RoundedCornerShape(16.dp),
                textStyle = Typography.bodyMedium.copy(color = NothingWhite)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteFilter.values().forEach { filter ->
                    NothingTag(
                        text = filter.name,
                        isSelected = currentFilter == filter,
                        onClick = {
                            haptic()
                            currentFilter = filter
                        }
                    )
                }
            }

            // Folder row if folders exist
            if (folders.isNotEmpty() && currentFilter != NoteFilter.TRASH) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOLDER:",
                        style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp)
                    )
                    NothingTag(
                        text = "ALL",
                        isSelected = selectedFolder == null,
                        onClick = { selectedFolder = null }
                    )
                    folders.forEach { folder ->
                        NothingTag(
                            text = folder,
                            isSelected = selectedFolder == folder,
                            onClick = { selectedFolder = folder }
                        )
                    }
                }
            }

            if (currentFilter == NoteFilter.TRASH && trashNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "EMPTY TRASH",
                        style = Typography.labelSmall.copy(color = NothingRed, letterSpacing = 1.sp),
                        modifier = Modifier
                            .clickable {
                                scope.launch { noteRepo.emptyTrash() }
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // List of Notes
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NothingDarkCard)
                                .border(1.dp, NothingBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NothingGray)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "NO MATCHING NOTES" else "NO NOTES FOUND",
                            style = Typography.titleSmall.copy(letterSpacing = 1.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to capture your first thought",
                            style = Typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            isTrash = currentFilter == NoteFilter.TRASH,
                            onClick = { onNavigateToEdit(note.id) },
                            onTogglePin = { scope.launch { noteRepo.togglePin(note) } },
                            onToggleArchive = { scope.launch { noteRepo.toggleArchive(note) } },
                            onDelete = { scope.launch { noteRepo.moveToTrash(note) } },
                            onRestore = { scope.launch { noteRepo.restoreFromTrash(note) } },
                            onDeletePermanent = { scope.launch { noteRepo.deletePermanently(note) } }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCardItem(
    note: Note,
    isTrash: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    val haptic = rememberTactileHaptic()
    val checklist = note.getChecklist()
    val tags = note.getTags()
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(note.updatedAt))

    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        onClick = if (!isTrash) onClick else null,
        borderColor = if (note.isPinned) NothingWhite else NothingBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Tags/Folder + Pin Indicator + Action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isVoiceNote) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VOICE",
                        style = Typography.labelSmall.copy(color = NothingRed, fontSize = 10.sp, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (note.folder.isNotBlank() && note.folder != "General") {
                    Text(
                        text = "[${note.folder.uppercase()}]",
                        style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!isTrash) {
                    IconButton(
                        onClick = {
                            haptic()
                            onTogglePin()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) NothingWhite else NothingGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            haptic()
                            onToggleArchive()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = if (note.isArchived) NothingRed else NothingGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            haptic()
                            onDelete()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NothingGray, modifier = Modifier.size(16.dp))
                    }
                } else {
                    IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restore", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDeletePermanent, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Permanently", tint = NothingRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                style = Typography.titleMedium.copy(fontSize = 17.sp, color = NothingWhite)
            )

            // Body or Voice Transcript Preview
            if (note.isVoiceNote) {
                Spacer(modifier = Modifier.height(6.dp))
                val min = (note.audioDurationMs / 1000) / 60
                val sec = (note.audioDurationMs / 1000) % 60
                val durationStr = String.format(Locale.US, "%02d:%02d", min, sec)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = NothingRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECORDING ($durationStr)",
                        style = Typography.labelSmall.copy(color = NothingRed, fontSize = 10.sp)
                    )
                }

                if (note.transcript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.transcript,
                        style = Typography.bodyMedium.copy(color = NothingGrayLight),
                        maxLines = 2
                    )
                }
            } else if (note.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.body,
                    style = Typography.bodyMedium.copy(color = NothingGrayLight),
                    maxLines = 3
                )
            }

            // Checklist Preview
            if (checklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val checkedCount = checklist.count { it.isChecked }
                Text(
                    text = "$checkedCount/${checklist.size} CHECKLIST COMPLETED",
                    style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp)
                )
                checklist.take(2).forEach { item ->
                    Text(
                        text = "• ${item.text}",
                        style = Typography.bodySmall.copy(
                            color = if (item.isChecked) NothingGrayDark else NothingGrayLight
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Tags + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.take(3).forEach { tag ->
                        NothingTag(text = tag)
                    }
                }
                Text(
                    text = formattedDate,
                    style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp)
                )
            }
        }
    }
}
