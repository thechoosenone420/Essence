package com.example.ui.screens.tasks

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EssenceApplication
import com.example.data.model.Task
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
import com.example.ui.theme.NothingTag
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.theme.Typography
import com.example.ui.theme.rememberTactileHaptic
import com.example.util.AlarmScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TaskViewTab {
    TODAY, UPCOMING, OVERDUE, ALL, COMPLETED
}

@Composable
fun TasksScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToNewTask: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberTactileHaptic()
    val scope = rememberCoroutineScope()
    val taskRepo = EssenceApplication.instance.taskRepository

    val allTasks by taskRepo.allTasks.collectAsState(initial = emptyList())
    var currentTab by remember { mutableStateOf(TaskViewTab.TODAY) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<String?>(null) }

    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfToday = now + (24 * 60 * 60 * 1000)

    val tabFilteredTasks = when (currentTab) {
        TaskViewTab.TODAY -> allTasks.filter {
            !it.isDone && ((it.dueDate != null && it.dueDate in now..endOfToday) || (it.dueDate != null && it.dueDate < now))
        }
        TaskViewTab.UPCOMING -> allTasks.filter {
            !it.isDone && (it.dueDate != null && it.dueDate > endOfToday)
        }
        TaskViewTab.OVERDUE -> allTasks.filter {
            !it.isDone && (it.dueDate != null && it.dueDate < now)
        }
        TaskViewTab.ALL -> allTasks.filter { !it.isDone }
        TaskViewTab.COMPLETED -> allTasks.filter { it.isDone }
    }

    val filteredTasks = tabFilteredTasks.filter { task ->
        val matchesProject = selectedProject == null || task.project.equals(selectedProject, ignoreCase = true)
        val matchesSearch = if (searchQuery.isBlank()) true else {
            task.title.contains(searchQuery, ignoreCase = true) ||
                    task.notes.contains(searchQuery, ignoreCase = true) ||
                    task.tagsJson.contains(searchQuery, ignoreCase = true)
        }
        matchesProject && matchesSearch
    }

    val projects = allTasks.map { it.project }.filter { it.isNotBlank() }.distinct()

    Scaffold(
        containerColor = NothingBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic()
                    onNavigateToNewTask()
                },
                containerColor = NothingRed,
                contentColor = NothingWhite,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_new_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Task", modifier = Modifier.size(28.dp))
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
                    text = "TO-DOS",
                    accentDot = true,
                    fontSize = 22
                )
                Text(
                    text = "${filteredTasks.size} PENDING",
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
                    .testTag("search_tasks_input"),
                placeholder = {
                    Text("Search tasks, projects, tags...", style = Typography.bodySmall.copy(color = NothingGray))
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

            // Tab View selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskViewTab.values().forEach { tab ->
                    NothingTag(
                        text = tab.name,
                        isSelected = currentTab == tab,
                        onClick = {
                            haptic()
                            currentTab = tab
                        }
                    )
                }
            }

            if (projects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROJECT:",
                        style = Typography.labelSmall.copy(color = NothingGray, fontSize = 10.sp)
                    )
                    NothingTag(
                        text = "ALL",
                        isSelected = selectedProject == null,
                        onClick = { selectedProject = null }
                    )
                    projects.forEach { proj ->
                        NothingTag(
                            text = proj,
                            isSelected = selectedProject == proj,
                            onClick = { selectedProject = proj }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Tasks List
            if (filteredTasks.isEmpty()) {
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
                            Icon(Icons.Default.Check, contentDescription = null, tint = NothingGray)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO TASKS HERE",
                            style = Typography.titleSmall.copy(letterSpacing = 1.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to add an action item",
                            style = Typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            onClick = { onNavigateToEdit(task.id) },
                            onToggleDone = {
                                haptic()
                                scope.launch {
                                    if (!task.isDone && task.repeatRule != "NONE" && task.dueDate != null) {
                                        val nextDate = AlarmScheduler.computeNextDueDate(task.dueDate, task.repeatRule)
                                        val updated = task.copy(dueDate = nextDate)
                                        taskRepo.updateTask(updated)
                                        AlarmScheduler.scheduleTaskReminders(context, updated)
                                    } else {
                                        taskRepo.toggleDone(task)
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    AlarmScheduler.cancelTaskReminders(context, task.id)
                                    taskRepo.deleteTask(task)
                                }
                            }
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
fun TaskCardItem(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    val subtasks = task.getSubtasks()
    val isOverdue = task.dueDate != null && task.dueDate < System.currentTimeMillis() && !task.isDone
    val dueDateStr = task.dueDate?.let {
        SimpleDateFormat("MMM d", Locale.US).format(Date(it))
    }

    val priorityColor = when (task.priority) {
        1 -> NothingRed
        2 -> NothingWhite
        3 -> NothingGray
        else -> NothingBorder
    }

    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (task.isDone) NothingRed else NothingGray,
                        CircleShape
                    )
                    .background(if (task.isDone) NothingRed else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isDone) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = NothingWhite, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Priority Dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.title.ifBlank { "Untitled Task" },
                        style = Typography.titleMedium.copy(
                            fontSize = 15.sp,
                            color = if (task.isDone) NothingGrayDark else NothingWhite
                        )
                    )
                }

                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.notes,
                        style = Typography.bodySmall.copy(
                            color = if (task.isDone) NothingGrayDark else NothingGray
                        ),
                        maxLines = 1
                    )
                }

                // Metadata row: Due date, reminders, repeat, project
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dueDateStr != null) {
                        Text(
                            text = if (isOverdue) "DUE $dueDateStr (OVERDUE)" else "DUE $dueDateStr",
                            style = Typography.labelSmall.copy(
                                color = if (isOverdue) NothingRed else NothingGray,
                                fontSize = 10.sp
                            )
                        )
                    }

                    if (task.getReminderTimes().isNotEmpty()) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Reminder Set",
                            tint = NothingWhite,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (task.repeatRule != "NONE") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, contentDescription = null, tint = NothingGray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = task.repeatRule,
                                style = Typography.labelSmall.copy(color = NothingGray, fontSize = 9.sp)
                            )
                        }
                    }

                    if (subtasks.isNotEmpty()) {
                        val doneCount = subtasks.count { it.isDone }
                        Text(
                            text = "$doneCount/${subtasks.size} SUBS",
                            style = Typography.labelSmall.copy(color = NothingGray, fontSize = 9.sp)
                        )
                    }

                    if (task.project.isNotBlank() && task.project != "Inbox") {
                        Text(
                            text = "[${task.project.uppercase()}]",
                            style = Typography.labelSmall.copy(color = NothingGrayLight, fontSize = 9.sp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NothingGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}
