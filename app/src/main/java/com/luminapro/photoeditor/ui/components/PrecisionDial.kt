package com.luminapro.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminapro.photoeditor.ui.theme.AmberBright
import com.luminapro.photoeditor.ui.theme.AmberGold
import com.luminapro.photoeditor.ui.theme.DarkroomBg
import com.luminapro.photoeditor.ui.theme.StudioBorder
import com.luminapro.photoeditor.ui.theme.TextMuted
import com.luminapro.photoeditor.ui.theme.TextPrimary
import com.luminapro.photoeditor.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PrecisionDial(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    defaultValue: Float = 0f,
    unitSuffix: String = "",
    spectrumColors: List<Color>? = null,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentValState by rememberUpdatedState(value)
    val currentCallback by rememberUpdatedState(onValueChange)
    var lastHapticStep by remember { mutableFloatStateOf(value.roundToInt().toFloat()) }

    val isModified = abs(value - defaultValue) >= 0.5f
    val intDisplay = value.roundToInt()
    val formatted = buildString {
        if (valueRange.start < 0f && intDisplay > 0) append("+")
        append(intDisplay)
        append(unitSuffix)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                if (isModified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AmberGold)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isModified) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onValueChange(defaultValue)
                            }
                            .padding(2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isModified) AmberGold.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = if (isModified) AmberGold.copy(alpha = 0.45f) else StudioBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onValueChange(defaultValue) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatted,
                        color = if (isModified) AmberBright else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C0E14))
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val span = valueRange.endInclusive - valueRange.start
                        val sensitivity = span / size.width.coerceAtLeast(1)
                        val updated = (currentValState + dragAmount * sensitivity * 1.15f)
                            .coerceIn(valueRange.start, valueRange.endInclusive)
                        if (abs(updated.roundToInt() - lastHapticStep) >= 5f) {
                            lastHapticStep = updated.roundToInt().toFloat()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentCallback(updated)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padX = 14.dp.toPx()
                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                val span = valueRange.endInclusive - valueRange.start
                val normVal = ((value - valueRange.start) / span).coerceIn(0f, 1f)
                val normZero = ((defaultValue - valueRange.start) / span).coerceIn(0f, 1f)
                val centerY = size.height / 2f

                // Optional spectrum track (Temperature / Tint / Hue)
                if (spectrumColors != null && spectrumColors.size >= 2) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = spectrumColors,
                            startX = padX,
                            endX = padX + usableW
                        ),
                        topLeft = Offset(padX, centerY - 3.dp.toPx()),
                        size = Size(usableW, 6.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        alpha = 0.55f
                    )
                } else {
                    // Active delta bar between defaultValue and current value
                    val startX = padX + minOf(normZero, normVal) * usableW
                    val barW = abs(normVal - normZero) * usableW
                    if (barW > 1f) {
                        drawRoundRect(
                            color = AmberGold.copy(alpha = 0.75f),
                            topLeft = Offset(startX, centerY - 1.5.dp.toPx()),
                            size = Size(barW, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }

                // 41 Hardware ruler ticks
                val ticks = 40
                for (i in 0..ticks) {
                    val t = i.toFloat() / ticks
                    val tx = padX + t * usableW
                    val isZeroTick = abs(t - normZero) < 0.015f
                    val isMajor = i % 5 == 0
                    val inActiveSpan = t >= minOf(normZero, normVal) && t <= maxOf(normZero, normVal)

                    val tickHalfH = when {
                        isZeroTick -> 9.dp.toPx()
                        isMajor -> 6.5.dp.toPx()
                        else -> 3.8.dp.toPx()
                    }

                    val tickColor = when {
                        isZeroTick -> TextPrimary
                        inActiveSpan && isMajor -> AmberBright
                        inActiveSpan -> AmberGold.copy(alpha = 0.65f)
                        isMajor -> Color(0xFF6B7280)
                        else -> Color(0xFF374151)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(tx, centerY - tickHalfH),
                        end = Offset(tx, centerY + tickHalfH),
                        strokeWidth = if (isZeroTick) 2.dp.toPx() else 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Glowing Amber Needle Indicator
                val needleX = padX + normVal * usableW
                drawLine(
                    color = AmberGold,
                    start = Offset(needleX, 4.dp.toPx()),
                    end = Offset(needleX, size.height - 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = AmberBright,
                    radius = 5.dp.toPx(),
                    center = Offset(needleX, 7.dp.toPx())
                )
                drawCircle(
                    color = DarkroomBg,
                    radius = 2.dp.toPx(),
                    center = Offset(needleX, 7.dp.toPx())
                )
            }
        }
    }
}
