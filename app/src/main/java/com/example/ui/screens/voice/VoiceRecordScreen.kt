package com.example.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.EssenceApplication
import com.example.data.model.Note
import com.example.service.AudioRecordService
import com.example.ui.theme.DotMatrixHeading
import com.example.ui.theme.DottedDivider
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCard
import com.example.ui.theme.NothingDarkCard
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGrayDark
import com.example.ui.theme.NothingGrayLight
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.Typography
import com.example.ui.theme.rememberTactileHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceRecordScreen(
    onNavigateToNote: (Long) -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberTactileHaptic()
    val noteRepo = EssenceApplication.instance.noteRepository
    val voiceNotes by noteRepo.voiceNotes.collectAsState(initial = emptyList())

    val isRecording by AudioRecordService.isRecording.collectAsState()
    val isPaused by AudioRecordService.isPaused.collectAsState()
    val durationMs by AudioRecordService.durationMs.collectAsState()
    val amplitudes by AudioRecordService.amplitudes.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val recordAudioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            haptic()
            AudioRecordService.start(context)
        } else {
            Toast.makeText(context, "Microphone permission is required to record", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleRecordClick() {
        haptic()
        if (isRecording) {
            AudioRecordService.stop(context)
            Toast.makeText(context, "Recording saved and processing...", Toast.LENGTH_SHORT).show()
        } else {
            val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true

            if (hasAudio && hasNotif) {
                AudioRecordService.start(context)
            } else {
                val toRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotif) {
                    toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(toRequest.toTypedArray())
            }
        }
    }

    // Animation for pulse when recording
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 60000) % 60
    val hours = (durationMs / 3600000)
    val timeFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    Scaffold(
        containerColor = NothingBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DotMatrixHeading(text = "VOICE // SPACE", accentDot = true, fontSize = 22)
                Text(
                    text = if (isRecording) "LIVE RECORDING" else "READY",
                    style = Typography.labelSmall.copy(
                        color = if (isRecording) NothingRed else NothingGray,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Big Timer
            Text(
                text = timeFormatted,
                style = Typography.displayLarge.copy(
                    fontSize = 42.sp,
                    color = if (isRecording) NothingWhite else NothingGray,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isRecording && isPaused -> "RECORDING PAUSED"
                    isRecording -> "RECORDING • SCREEN OFF SAFE"
                    else -> "ONE TAP TO RECORD"
                },
                style = Typography.labelSmall.copy(
                    color = when {
                        isRecording && isPaused -> NothingWhite
                        isRecording -> NothingRed
                        else -> NothingGray
                    },
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Waveform visualizer
            WaveformVisualizer(
                amplitudes = amplitudes,
                isRecording = isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Record Button Area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                if (isRecording && !isPaused) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .border(2.dp, NothingRed.copy(alpha = 0.5f), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                        .border(1.dp, NothingBorder, CircleShape)
                        .clickable { handleRecordClick() }
                        .testTag("voice_main_record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        tint = NothingWhite,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Controls when recording: Pause / Resume
            if (isRecording) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NothingDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder),
                        onClick = {
                            haptic()
                            AudioRecordService.togglePause(context)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = NothingWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaused) "RESUME" else "PAUSE",
                                style = Typography.labelSmall.copy(color = NothingWhite, letterSpacing = 1.sp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NothingDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed),
                        onClick = {
                            haptic()
                            AudioRecordService.stop(context)
                            Toast.makeText(context, "Recording saved and processing...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Done", tint = NothingRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVE NOTE",
                                style = Typography.labelSmall.copy(color = NothingRed, letterSpacing = 1.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Recent Voice Notes Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DotMatrixHeading(text = "RECENT VOICE NOTES", fontSize = 14)
                Text(
                    text = "${voiceNotes.size} SAVED",
                    style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // List of Voice Notes
            if (voiceNotes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No voice notes captured yet",
                        style = Typography.bodySmall.copy(color = NothingGray)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(voiceNotes.take(10), key = { it.id }) { note ->
                        VoiceNoteItemRow(
                            note = note,
                            onClick = { onNavigateToNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    amplitudes: List<Int>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 40
        val barWidth = 3.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
        val centerY = size.height / 2

        val displayAmps = if (amplitudes.isEmpty() || !isRecording) {
            List(barCount) { 4 }
        } else {
            val list = amplitudes.takeLast(barCount).toMutableList()
            while (list.size < barCount) list.add(0, 4)
            list
        }

        displayAmps.forEachIndexed { i, amp ->
            val heightFrac = (amp / 100f).coerceIn(0.08f, 1f)
            val barHeight = size.height * heightFrac
            val left = i * (barWidth + spacing)
            val top = centerY - (barHeight / 2)

            drawRoundRect(
                color = if (isRecording) NothingRed else NothingBorder,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun VoiceNoteItemRow(
    note: Note,
    onClick: () -> Unit
) {
    val haptic = rememberTactileHaptic()
    val min = (note.audioDurationMs / 1000) / 60
    val sec = (note.audioDurationMs / 1000) % 60
    val durationFormatted = String.format(Locale.US, "%02d:%02d", min, sec)
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(note.createdAt))

    NothingCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic()
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingDarkCard)
                    .border(1.dp, NothingBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = NothingRed, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Voice Note" },
                    style = Typography.titleSmall.copy(fontSize = 14.sp)
                )
                if (note.transcript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.transcript,
                        style = Typography.bodySmall.copy(color = NothingGrayLight),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationFormatted,
                    style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = Typography.bodySmall.copy(color = NothingGray, fontSize = 9.sp)
                )
            }
        }
    }
}
