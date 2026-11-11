package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.service.AudioRecordService
import com.example.ui.screens.notes.NoteEditScreen
import com.example.ui.screens.notes.NotesScreen
import com.example.ui.screens.oem.OemSetupDialog
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TaskEditScreen
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.voice.VoiceRecordScreen
import com.example.ui.theme.DottedDivider
import com.example.ui.theme.EssenceTheme
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDarkCard
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.Typography
import com.example.ui.theme.rememberTactileHaptic
import com.example.util.OemHardeningHelper

sealed class Screen(val route: String, val title: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    data object Notes : Screen("notes", "NOTES", Icons.Filled.Description, Icons.Outlined.Description)
    data object Tasks : Screen("tasks", "TO-DOS", Icons.Filled.CheckBox, Icons.Outlined.CheckBox)
    data object Voice : Screen("voice", "VOICE", Icons.Filled.Mic, Icons.Outlined.Mic)
    data object Settings : Screen("settings", "SETTINGS", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EssenceTheme {
                MainContent(intent = intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainContent(intent: Intent?) {
    val context = LocalContext.current
    val haptic = rememberTactileHaptic()
    val navController = rememberNavController()
    val prefs = EssenceApplication.instance.appPreferences
    val hasCompletedOemSetup by prefs.hasCompletedOemSetup.collectAsState()

    var showOemPrompt by remember { mutableStateOf(false) }

    // Auto-prompt OEM setup on first launch if on Xiaomi / OEM
    LaunchedEffect(Unit) {
        if (!hasCompletedOemSetup && OemHardeningHelper.isXiaomiOrHyperOs()) {
            showOemPrompt = true
        }
    }

    // Permission launcher for quick record requested via shortcut or notification
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val recordAudioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            AudioRecordService.start(context)
            Toast.makeText(context, "Voice recording started", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle deep link / shortcut / notification intents
    LaunchedEffect(intent) {
        if (intent == null) return@LaunchedEffect

        when (intent.action) {
            "com.example.essence.ACTION_NEW_NOTE" -> {
                navController.navigate("note_edit/0")
            }
            "com.example.essence.ACTION_NEW_TASK" -> {
                navController.navigate("task_edit/0")
            }
            else -> {
                if (intent.getBooleanExtra("NAVIGATE_TO_TASKS", false)) {
                    navController.navigate(Screen.Tasks.route)
                } else if (intent.getBooleanExtra("REQUEST_PERMISSIONS_FOR_RECORD", false)) {
                    val toRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(toRequest.toTypedArray())
                }
            }
        }
    }

    if (showOemPrompt) {
        OemSetupDialog(
            onDismiss = { showOemPrompt = false },
            onComplete = {
                prefs.setOemSetupCompleted(true)
                showOemPrompt = false
            }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevel = currentRoute in listOf(
        Screen.Notes.route,
        Screen.Tasks.route,
        Screen.Voice.route,
        Screen.Settings.route
    )

    Scaffold(
        containerColor = NothingBlack,
        bottomBar = {
            if (isTopLevel) {
                NothingBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        haptic()
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Notes.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Notes.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Notes.route) {
                NotesScreen(
                    onNavigateToEdit = { noteId ->
                        navController.navigate("note_edit/$noteId")
                    },
                    onNavigateToNewNote = {
                        navController.navigate("note_edit/0")
                    }
                )
            }

            composable(Screen.Tasks.route) {
                TasksScreen(
                    onNavigateToEdit = { taskId ->
                        navController.navigate("task_edit/$taskId")
                    },
                    onNavigateToNewTask = {
                        navController.navigate("task_edit/0")
                    }
                )
            }

            composable(Screen.Voice.route) {
                VoiceRecordScreen(
                    onNavigateToNote = { noteId ->
                        navController.navigate("note_edit/$noteId")
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = "note_edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                NoteEditScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "task_edit/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                TaskEditScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun NothingBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val screens = listOf(Screen.Notes, Screen.Tasks, Screen.Voice, Screen.Settings)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NothingDarkCard)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        DottedDivider(color = NothingBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val icon = if (isSelected) screen.filledIcon else screen.outlinedIcon
                val tint = when {
                    screen == Screen.Voice && isSelected -> NothingRed
                    isSelected -> NothingWhite
                    else -> NothingGray
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(screen) }
                        .padding(vertical = 6.dp)
                        .testTag("nav_item_${screen.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(if (screen == Screen.Voice) NothingRed else NothingWhite)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = screen.title,
                            style = Typography.labelSmall.copy(
                                color = tint,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
