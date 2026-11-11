package com.example.ui.screens.oem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DotMatrixHeading
import com.example.ui.theme.DottedDivider
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingButton
import com.example.ui.theme.NothingCard
import com.example.ui.theme.NothingDarkCard
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGrayLight
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.theme.Typography
import com.example.util.OemHardeningHelper

@Composable
fun OemSetupDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val isXiaomi = OemHardeningHelper.isXiaomiOrHyperOs()
    val isBatteryIgnored = OemHardeningHelper.isIgnoringBatteryOptimizations(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDarkCard,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NothingBorder, RoundedCornerShape(24.dp)),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixHeading(
                        text = if (isXiaomi) "HYPEROS HARDENING" else "OEM HARDENING",
                        accentDot = true,
                        fontSize = 18
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NothingGray)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ensure background recording & alarms work reliably when screen is locked.",
                    style = Typography.bodySmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DottedDivider(modifier = Modifier.padding(vertical = 8.dp))

                OemChecklistItem(
                    stepNumber = "01",
                    title = "Battery Optimization",
                    desc = "Exclude Essence from Android doze mode so alarms fire on time.",
                    isDone = isBatteryIgnored,
                    icon = Icons.Default.Power,
                    actionLabel = if (isBatteryIgnored) "OPTIMIZED" else "DISABLE",
                    onAction = { OemHardeningHelper.openBatteryOptimizationSettings(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OemChecklistItem(
                    stepNumber = "02",
                    title = "Battery Saver: No Restrictions",
                    desc = "Set MIUI / HyperOS battery saver mode to 'No restrictions'.",
                    icon = Icons.Default.Security,
                    actionLabel = "OPEN SETTINGS",
                    onAction = { OemHardeningHelper.openNoRestrictionsBatterySaver(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OemChecklistItem(
                    stepNumber = "03",
                    title = "Enable Autostart",
                    desc = "Allow app to boot after reboot to restore alarms.",
                    icon = Icons.Default.Refresh,
                    actionLabel = "AUTOSTART",
                    onAction = { OemHardeningHelper.openAutostartSettings(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OemChecklistItem(
                    stepNumber = "04",
                    title = "Background Pop-ups & Lockscreen",
                    desc = "Allow display pop-ups while running in background and on lock screen.",
                    icon = Icons.Default.Security,
                    actionLabel = "PERMISSIONS",
                    onAction = { OemHardeningHelper.openBackgroundPopupsSettings(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OemChecklistItem(
                    stepNumber = "05",
                    title = "Lock in Recents Screen",
                    desc = "Open Recents screen, press & hold Essence card, tap the Padlock icon.",
                    icon = Icons.Default.Lock,
                    actionLabel = "GUIDE",
                    onAction = { /* Informational step */ }
                )
            }
        },
        confirmButton = {
            NothingButton(
                text = "DONE",
                isPrimary = true,
                onClick = {
                    onComplete()
                    onDismiss()
                }
            )
        }
    )
}

@Composable
fun OemChecklistItem(
    stepNumber: String,
    title: String,
    desc: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    isDone: Boolean = false
) {
    NothingCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NothingBlack
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isDone) NothingWhite else NothingBorder),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NothingBlack, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            text = stepNumber,
                            style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 9.sp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title.uppercase(),
                    style = Typography.titleSmall.copy(fontSize = 13.sp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = desc,
                style = Typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp)),
                color = NothingDarkCard,
                onClick = onAction
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = actionLabel,
                        style = Typography.labelSmall.copy(color = NothingWhite, fontSize = 10.sp, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NothingGray, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
