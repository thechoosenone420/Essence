package com.example.ui.screens.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.preferences.AppPreferences
import com.example.ui.screens.oem.OemSetupDialog
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
import com.example.ui.theme.Typography
import com.example.ui.theme.rememberTactileHaptic
import com.example.util.OemHardeningHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val haptic = rememberTactileHaptic()
    val scope = rememberCoroutineScope()
    val app = EssenceApplication.instance
    val prefs = app.appPreferences
    val backupRepo = app.backupRepository
    val transcriptionService = app.transcriptionService

    val isTranscriptionEnabled by prefs.isTranscriptionEnabled.collectAsState()
    val provider by prefs.provider.collectAsState()
    val customBaseUrl by prefs.customBaseUrl.collectAsState()
    val customModelName by prefs.customModelName.collectAsState()
    val aiSummarizeEnabled by prefs.aiSummarizeEnabled.collectAsState()
    val aiExtractTasksEnabled by prefs.aiExtractTasksEnabled.collectAsState()

    var inputBaseUrl by remember(customBaseUrl) { mutableStateOf(customBaseUrl) }
    var inputModelName by remember(customModelName) { mutableStateOf(customModelName) }
    var inputApiKey by remember { mutableStateOf(prefs.getApiKey()) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showOemDialog by remember { mutableStateOf(false) }

    // Export ZIP Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        val result = backupRepo.exportBackup(out)
                        result.onSuccess { count ->
                            Toast.makeText(context, "Exported $count items to ZIP backup", Toast.LENGTH_LONG).show()
                        }.onFailure { e ->
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Import ZIP Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        val result = backupRepo.importBackup(inStream)
                        result.onSuccess { count ->
                            Toast.makeText(context, "Imported $count items successfully!", Toast.LENGTH_LONG).show()
                        }.onFailure { e ->
                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val canExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() ?: false
    } else true

    if (showOemDialog) {
        OemSetupDialog(
            onDismiss = { showOemDialog = false },
            onComplete = {
                prefs.setOemSetupCompleted(true)
                showOemDialog = false
            }
        )
    }

    Scaffold(
        containerColor = NothingBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DotMatrixHeading(text = "SETTINGS", accentDot = true, fontSize = 22)
                Text(
                    text = "v1.0 // PURE PRIVACY",
                    style = Typography.labelSmall.copy(color = NothingGray, letterSpacing = 1.sp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: TRANSCRIPTION & PROVIDER
            DotMatrixHeading(text = "TRANSCRIPTION", fontSize = 14)
            Spacer(modifier = Modifier.height(8.dp))

            NothingCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speech Transcription", style = Typography.titleSmall.copy(fontSize = 14.sp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isTranscriptionEnabled) "Audio is converted to text" else "Audio only (nothing converted)",
                                style = Typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isTranscriptionEnabled,
                            onCheckedChange = {
                                haptic()
                                prefs.setTranscriptionEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NothingWhite,
                                checkedTrackColor = NothingRed,
                                uncheckedThumbColor = NothingGray,
                                uncheckedTrackColor = NothingDarkCard
                            )
                        )
                    }

                    if (isTranscriptionEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("PROVIDER", style = Typography.labelSmall.copy(color = NothingGray))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NothingTag(
                                text = "ON-DEVICE (PRIVATE)",
                                isSelected = provider == AppPreferences.PROVIDER_ON_DEVICE,
                                onClick = {
                                    haptic()
                                    prefs.setProvider(AppPreferences.PROVIDER_ON_DEVICE)
                                }
                            )

                            NothingTag(
                                text = "CUSTOM ENDPOINT",
                                isSelected = provider == AppPreferences.PROVIDER_CUSTOM,
                                onClick = {
                                    haptic()
                                    prefs.setProvider(AppPreferences.PROVIDER_CUSTOM)
                                }
                            )
                        }

                        if (provider == AppPreferences.PROVIDER_ON_DEVICE) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Uses offline Android SpeechRecognizer on-device engine. 100% private, free, and no network calls.",
                                style = Typography.bodySmall.copy(color = NothingGrayLight, lineHeight = 16.sp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "OpenAI-compatible speech-to-text API (e.g. self-hosted faster-whisper, LocalAI, Ollama, Groq, OpenAI).",
                                style = Typography.bodySmall.copy(color = NothingGray, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputBaseUrl,
                                onValueChange = {
                                    inputBaseUrl = it
                                    prefs.setCustomBaseUrl(it)
                                },
                                label = { Text("Base URL", style = Typography.labelSmall) },
                                placeholder = { Text("https://api.openai.com/v1") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = Typography.bodySmall.copy(color = NothingWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NothingWhite,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedLabelColor = NothingWhite,
                                    unfocusedLabelColor = NothingGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputModelName,
                                onValueChange = {
                                    inputModelName = it
                                    prefs.setCustomModelName(it)
                                },
                                label = { Text("Model Name", style = Typography.labelSmall) },
                                placeholder = { Text("whisper-1") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = Typography.bodySmall.copy(color = NothingWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NothingWhite,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedLabelColor = NothingWhite,
                                    unfocusedLabelColor = NothingGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputApiKey,
                                onValueChange = {
                                    inputApiKey = it
                                    prefs.saveApiKey(it)
                                },
                                label = { Text("API Key (Encrypted in Keystore)", style = Typography.labelSmall) },
                                placeholder = { Text("sk-...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = Typography.bodySmall.copy(color = NothingWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NothingWhite,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedLabelColor = NothingWhite,
                                    unfocusedLabelColor = NothingGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            NothingButton(
                                text = if (isTestingConnection) "TESTING..." else "TEST CONNECTION",
                                isPrimary = false,
                                enabled = !isTestingConnection,
                                onClick = {
                                    isTestingConnection = true
                                    testResult = null
                                    scope.launch {
                                        val res = transcriptionService.testConnection(inputBaseUrl, inputApiKey, inputModelName)
                                        res.onSuccess { msg ->
                                            testResult = msg
                                        }.onFailure { err ->
                                            testResult = "Error: ${err.message}"
                                        }
                                        isTestingConnection = false
                                    }
                                }
                            )

                            testResult?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg,
                                    style = Typography.bodySmall.copy(
                                        color = if (msg.startsWith("Error")) NothingRed else NothingWhite,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        // AI Actions toggles
                        Text("INTELLIGENT ACTIONS", style = Typography.labelSmall.copy(color = NothingGray))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Summarize Notes", style = Typography.titleSmall.copy(fontSize = 13.sp))
                                Text("Generates concise bullet summaries", style = Typography.bodySmall)
                            }
                            Switch(
                                checked = aiSummarizeEnabled,
                                onCheckedChange = { prefs.setAiSummarizeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NothingWhite, checkedTrackColor = NothingRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Extract To-Dos from Voice", style = Typography.titleSmall.copy(fontSize = 13.sp))
                                Text("Finds action items in recordings", style = Typography.bodySmall)
                            }
                            Switch(
                                checked = aiExtractTasksEnabled,
                                onCheckedChange = { prefs.setAiExtractTasksEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NothingWhite, checkedTrackColor = NothingRed)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: DATA & BACKUP
            DotMatrixHeading(text = "BACKUP & RESTORE", fontSize = 14)
            Spacer(modifier = Modifier.height(8.dp))

            NothingCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Full Local Backup", style = Typography.titleSmall.copy(fontSize = 14.sp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Export all notes, to-dos, checklists, and voice audio files into a standard ZIP archive.",
                        style = Typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NothingButton(
                            text = "EXPORT ZIP",
                            icon = Icons.Default.Upload,
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                exportLauncher.launch("essence_backup_$timestamp.zip")
                            }
                        )

                        NothingButton(
                            text = "IMPORT ZIP",
                            icon = Icons.Default.Download,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    DottedDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Placeholder Sync to Server
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = NothingGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sync to My Server", style = Typography.titleSmall.copy(fontSize = 13.sp, color = NothingGray))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "[COMING SOON]",
                                    style = Typography.labelSmall.copy(color = NothingRed, fontSize = 9.sp, letterSpacing = 1.sp)
                                )
                            }
                            Text(
                                "Planned: WebDAV / REST sync to your private self-hosted home server.",
                                style = Typography.bodySmall.copy(fontSize = 11.sp, color = NothingGrayDark)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: OEM HARDENING & ALARMS
            DotMatrixHeading(text = "SYSTEM & OEM HARDENING", fontSize = 14)
            Spacer(modifier = Modifier.height(8.dp))

            NothingCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val isXiaomi = OemHardeningHelper.isXiaomiOrHyperOs()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NothingRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isXiaomi) "Xiaomi HyperOS Hardening" else "OEM Background Protection",
                                style = Typography.titleSmall.copy(fontSize = 14.sp)
                            )
                            Text(
                                text = if (isXiaomi) "Xiaomi / MIUI device detected" else "Universal Android background check",
                                style = Typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Prevent aggressive task killers from killing the voice recorder or dropping exact alarm notifications.",
                        style = Typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NothingButton(
                        text = "RUN HARDENING CHECKLIST",
                        icon = Icons.Default.Security,
                        onClick = { showOemDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    DottedDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Exact Alarm Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Exact Alarms Permission", style = Typography.titleSmall.copy(fontSize = 13.sp))
                                Text(
                                    text = if (canExactAlarms) "Granted (alarms fire accurately)" else "Restricted by system",
                                    style = Typography.bodySmall.copy(color = if (canExactAlarms) NothingGrayLight else NothingRed)
                                )
                            }
                        }

                        if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            NothingButton(
                                text = "FIX",
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        OemHardeningHelper.openAppDetailsSettings(context)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
