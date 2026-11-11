package com.example.ui.screens.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.SubtaskItem
import com.example.data.model.Task
import com.example.ui.theme.DotMatrixHeading
import com.example.ui.theme.DottedDivider
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
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
import com.example.util.AlarmScheduler
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskEditScreen(
    taskId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberTactileHaptic()
    val scope = rememberCoroutineScope()
    val app = EssenceApplication.instance
    val taskRepo = app.taskRepository

    var task by remember { mutableStateOf<Task?>(null) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(3) }
    var project by remember { mutableStateOf("Inbox") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var dueTimeMinutes by remember { mutableStateOf<Int?>(null) }
    var repeatRule by remember { mutableStateOf("NONE") }

    val subtasks = remember { mutableStateListOf<SubtaskItem>() }
    var newSubtaskText by remember { mutableStateOf("") }

    val reminders = remember { mutableStateListOf<Long>() }

    LaunchedEffect(taskId) {
        if (taskId > 0) {
            val loaded = taskRepo.getTaskById(taskId)
            if (loaded != null) {
                task = loaded
                title = loaded.title
                notes = loaded.notes
                priority = loaded.priority
                project = loaded.project
                dueDate = loaded.dueDate
                dueTimeMinutes = loaded.dueTimeMinutes
                repeatRule = loaded.repeatRule
                subtasks.clear()
                subtasks.addAll(loaded.getSubtasks())
                reminders.clear()
                reminders.addAll(loaded.getReminderTimes())
            }
        }
    }

    fun saveTask() {
        scope.launch {
            val subtasksJson = SubtaskItem.encodeList(subtasks.toList())
            val remindersJson = JSONArray(reminders.toList()).toString()
            val currentTask = task

            val updated = if (currentTask != null) {
                currentTask.copy(
                    title = title.ifBlank { "Untitled Task" },
                    notes = notes,
                    subtasksJson = subtasksJson,
                    priority = priority,
                    project = project,
                    dueDate = dueDate,
                    dueTimeMinutes = dueTimeMinutes,
                    repeatRule = repeatRule,
                    reminderTimesJson = remindersJson
                )
            } else {
                Task(
                    title = title.ifBlank { "Untitled Task" },
                    notes = notes,
                    subtasksJson = subtasksJson,
                    priority = priority,
                    project = project,
                    dueDate = dueDate,
                    dueTimeMinutes = dueTimeMinutes,
                    repeatRule = repeatRule,
                    reminderTimesJson = remindersJson
                )
            }

            val savedId = if (currentTask != null) {
                taskRepo.updateTask(updated)
                currentTask.id
            } else {
                taskRepo.insertTask(updated)
            }

            // Schedule Alarms
            AlarmScheduler.scheduleTaskReminders(context, updated.copy(id = savedId))
        }
    }

    val calendar = Calendar.getInstance()

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
                    saveTask()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NothingWhite)
                }
                DotMatrixHeading(text = if (taskId > 0) "EDIT TASK" else "NEW TASK", accentDot = true, fontSize = 18)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    saveTask()
                    Toast.makeText(context, "Task saved", Toast.LENGTH_SHORT).show()
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
            // Priority Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("PRIORITY:", style = Typography.labelSmall.copy(color = NothingGray))
                listOf(
                    1 to "P1 URGENT",
                    2 to "P2 HIGH",
                    3 to "P3 MEDIUM",
                    4 to "P4 LOW"
                ).forEach { (pNum, pLabel) ->
                    val isSelected = priority == pNum
                    val pColor = when (pNum) {
                        1 -> NothingRed
                        2 -> NothingWhite
                        3 -> NothingGray
                        else -> NothingBorder
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) NothingWhite else NothingDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NothingWhite else NothingBorder),
                        onClick = { priority = pNum }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(pColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pLabel,
                                style = Typography.labelSmall.copy(
                                    color = if (isSelected) NothingBlack else NothingGrayLight,
                                    fontSize = 9.sp
                                )
                            )
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
                    .testTag("task_edit_title"),
                placeholder = {
                    Text(
                        "Task title...",
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

            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_edit_notes"),
                placeholder = {
                    Text("Add notes or details...", style = Typography.bodyMedium.copy(color = NothingGray))
                },
                textStyle = Typography.bodyMedium.copy(color = NothingWhite),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Due Date & Time Section
            DotMatrixHeading(text = "SCHEDULE & REMINDERS", fontSize = 14)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date Button
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NothingDarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder),
                    onClick = {
                        val dPicker = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                dueDate = cal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dPicker.show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dueDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(it)) } ?: "SET DATE",
                            style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 11.sp)
                        )
                    }
                }

                // Time Button
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NothingDarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder),
                    onClick = {
                        val tPicker = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                dueTimeMinutes = (hourOfDay * 60) + minute
                            },
                            dueTimeMinutes?.div(60) ?: 9,
                            dueTimeMinutes?.rem(60) ?: 0,
                            true
                        )
                        tPicker.show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val timeStr = dueTimeMinutes?.let {
                            String.format(Locale.US, "%02d:%02d", it / 60, it % 60)
                        } ?: "SET TIME"
                        Text(
                            text = timeStr,
                            style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 11.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Repeat Rule Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = NothingGray, modifier = Modifier.size(16.dp))
                listOf("NONE", "DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY").forEach { rule ->
                    NothingTag(
                        text = rule,
                        isSelected = repeatRule == rule,
                        onClick = { repeatRule = rule }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reminders List
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = NothingRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EXACT ALARM REMINDERS", style = Typography.labelSmall.copy(color = NothingWhite))
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NothingDarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder),
                    onClick = {
                        val targetDate = dueDate ?: System.currentTimeMillis()
                        val minutes = dueTimeMinutes ?: (9 * 60)
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = targetDate
                            set(Calendar.HOUR_OF_DAY, minutes / 60)
                            set(Calendar.MINUTE, minutes % 60)
                        }
                        if (cal.timeInMillis > System.currentTimeMillis()) {
                            reminders.add(cal.timeInMillis)
                            Toast.makeText(context, "Reminder alarm scheduled", Toast.LENGTH_SHORT).show()
                        } else {
                            // 10 min from now
                            val testAlarm = System.currentTimeMillis() + (10 * 60 * 1000)
                            reminders.add(testAlarm)
                            Toast.makeText(context, "Alarm set for 10 min from now", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("+ ADD ALARM", style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 10.sp), modifier = Modifier.padding(6.dp))
                }
            }

            reminders.forEachIndexed { idx, alarmTime ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• Alarm at ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(alarmTime))}",
                        style = Typography.bodySmall.copy(color = NothingGrayLight),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { reminders.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = NothingGray, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Subtasks section
            DotMatrixHeading(text = "SUBTASKS", fontSize = 14)
            Spacer(modifier = Modifier.height(8.dp))

            subtasks.forEachIndexed { index, sub ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sub.isDone,
                        onCheckedChange = { isChecked ->
                            subtasks[index] = sub.copy(isDone = isChecked)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NothingRed,
                            uncheckedColor = NothingGray,
                            checkmarkColor = NothingWhite
                        )
                    )
                    Text(
                        text = sub.text,
                        style = Typography.bodyMedium.copy(
                            color = if (sub.isDone) NothingGrayDark else NothingWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { subtasks.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete Subtask", tint = NothingGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add subtask...", style = Typography.bodySmall) },
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
                IconButton(onClick = {
                    if (newSubtaskText.isNotBlank()) {
                        subtasks.add(SubtaskItem(text = newSubtaskText.trim()))
                        newSubtaskText = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = NothingWhite)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
