package studio.velox.atelier.ui.components

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
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.velox.atelier.ui.theme.ChalkWhite
import studio.velox.atelier.ui.theme.ChampagneBrass
import studio.velox.atelier.ui.theme.KnurledCapsule
import studio.velox.atelier.ui.theme.LeicaVermilion
import studio.velox.atelier.ui.theme.MachinedBevel
import studio.velox.atelier.ui.theme.ObsidianStage
import studio.velox.atelier.ui.theme.PewterMuted
import studio.velox.atelier.ui.theme.SilverLabel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun KnurledRotaryDial(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = -100f..100f,
    defaultValue: Float = 0f,
    unit: String = "",
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val latestVal by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    var lastDetent by remember { mutableIntStateOf(value.roundToInt() / 4) }

    val modified = abs(value - defaultValue) >= 0.5f
    val displayInt = value.roundToInt()
    val readout = buildString {
        if (range.start < 0f && displayInt > 0) append("+")
        append(displayInt)
        append(unit)
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
                    color = if (modified) ChalkWhite else SilverLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp
                )
                if (modified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(LeicaVermilion)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (modified) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = "Reset",
                        tint = PewterMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onValueChange(defaultValue)
                            }
                            .padding(3.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (modified) LeicaVermilion.copy(alpha = 0.16f) else KnurledCapsule)
                        .border(
                            1.dp,
                            if (modified) LeicaVermilion.copy(alpha = 0.55f) else MachinedBevel,
                            RoundedCornerShape(50)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onValueChange(defaultValue)
                        }
                        .padding(horizontal = 10.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = readout,
                        color = if (modified) ChampagneBrass else SilverLabel,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3D Cylindrical Lens-Barrel Knurled Scrubber
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF07080A),
                            Color(0xFF15171F),
                            Color(0xFF1A1D27),
                            Color(0xFF15171F),
                            Color(0xFF07080A)
                        )
                    )
                )
                .border(1.dp, MachinedBevel, RoundedCornerShape(14.dp))
                .pointerInput(range) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val span = range.endInclusive - range.start
                        // Dragging right increases value smoothly with precision gear ratio
                        val delta = (dragAmount / size.width.coerceAtLeast(1)) * span * 0.95f
                        val next = (latestVal + delta).coerceIn(range.start, range.endInclusive)
                        val curDetent = next.roundToInt() / 4
                        if (curDetent != lastDetent) {
                            lastDetent = curDetent
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        latestOnChange(next)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midX = w * 0.5f
                val midY = h * 0.54f
                val radiusX = w * 0.46f

                val span = (range.endInclusive - range.start).coerceAtLeast(1f)
                val normOffset = (value - defaultValue) / span // -0.5..+0.5

                // Draw cylindrical projected lens-ring ticks
                val totalTicks = 48
                for (i in -totalTicks..totalTicks) {
                    val tickNorm = i.toFloat() / 24f - normOffset * 1.6f
                    val theta = tickNorm * (PI.toFloat() * 0.48f)
                    if (theta < -PI * 0.47 || theta > PI * 0.47) continue

                    val projX = midX + sin(theta) * radiusX
                    val depthCos = cos(theta).coerceIn(0f, 1f)
                    val isZeroOrigin = (i == 0)
                    val isFifth = (i % 5 == 0)

                    val tickHalf = when {
                        isZeroOrigin -> 10.dp.toPx()
                        isFifth -> 7.dp.toPx() * (0.55f + 0.45f * depthCos)
                        else -> 4.dp.toPx() * (0.45f + 0.55f * depthCos)
                    }

                    val tickColor = when {
                        isZeroOrigin -> ChampagneBrass.copy(alpha = (0.4f + 0.6f * depthCos))
                        isFifth -> ChalkWhite.copy(alpha = 0.58f * depthCos)
                        else -> SilverLabel.copy(alpha = 0.28f * depthCos)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(projX, midY - tickHalf),
                        end = Offset(projX, midY + tickHalf),
                        strokeWidth = if (isZeroOrigin) 2.2.dp.toPx() else 1.3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Fixed Center Vermilion Lens-Index Pointer & Needle
                drawLine(
                    color = LeicaVermilion,
                    start = Offset(midX, 6.dp.toPx()),
                    end = Offset(midX, h - 5.dp.toPx()),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val pointerTriangle = Path().apply {
                    moveTo(midX - 5.dp.toPx(), 0f)
                    lineTo(midX + 5.dp.toPx(), 0f)
                    lineTo(midX, 7.dp.toPx())
                    close()
                }
                drawPath(pointerTriangle, color = LeicaVermilion)

                // Soft edge shadow vignette for physical barrel depth
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ObsidianStage.copy(alpha = 0.78f),
                            Color.Transparent,
                            Color.Transparent,
                            ObsidianStage.copy(alpha = 0.78f)
                        )
                    )
                )
            }
        }
    }
}
