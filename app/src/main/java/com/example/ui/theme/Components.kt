package com.example.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun View.performTactileHaptic() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

@Composable
fun rememberTactileHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}

@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = NothingBorder,
    dotRadius: Dp = 1.dp,
    spacing: Dp = 6.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val y = size.height / 2
        val dotRadiusPx = dotRadius.toPx()
        val spacingPx = spacing.toPx()
        var currentX = dotRadiusPx
        while (currentX < size.width) {
            drawCircle(
                color = color,
                radius = dotRadiusPx,
                center = Offset(currentX, y)
            )
            currentX += spacingPx
        }
    }
}

@Composable
fun DotMatrixHeading(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NothingWhite,
    fontSize: Int = 20,
    accentDot: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accentDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text.uppercase(),
            style = Typography.headlineMedium.copy(
                fontSize = fontSize.sp,
                color = color,
                letterSpacing = 1.5.sp
            )
        )
    }
}

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = NothingBorder,
    backgroundColor: Color = NothingDarkCard,
    content: @Composable () -> Unit
) {
    val haptic = rememberTactileHaptic()
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = {
                            haptic()
                            onClick()
                        }
                    )
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        content()
    }
}

@Composable
fun NothingTag(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val haptic = rememberTactileHaptic()
    val bg = if (isSelected) NothingWhite else NothingDarkCard
    val contentCol = if (isSelected) NothingBlack else NothingGrayLight
    val borderCol = if (isSelected) NothingWhite else NothingBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptic()
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) NothingRed else NothingGray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text.uppercase(),
                style = Typography.labelSmall.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    color = contentCol,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val haptic = rememberTactileHaptic()
    val bg = when {
        !enabled -> NothingGrayMuted
        isPrimary -> NothingRed
        else -> NothingDarkCard
    }
    val contentColor = when {
        !enabled -> NothingGray
        isPrimary -> NothingWhite
        else -> NothingWhite
    }
    val border = if (isPrimary || !enabled) NothingBorderSubtle else NothingBorder

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, border, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled) {
                haptic()
                onClick()
            },
        color = bg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = Typography.labelLarge.copy(
                    color = contentColor,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
