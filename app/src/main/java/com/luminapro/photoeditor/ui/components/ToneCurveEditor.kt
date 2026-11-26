package com.luminapro.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminapro.photoeditor.model.CurveChannel
import com.luminapro.photoeditor.ui.theme.AmberBright
import com.luminapro.photoeditor.ui.theme.AmberGold
import com.luminapro.photoeditor.ui.theme.DarkroomBg
import com.luminapro.photoeditor.ui.theme.StudioBorder
import com.luminapro.photoeditor.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun ToneCurveEditor(
    activeChannel: CurveChannel,
    curves: Map<CurveChannel, List<Float>>,
    onSelectChannel: (CurveChannel) -> Unit,
    onUpdateChannelPoints: (CurveChannel, List<Float>) -> Unit,
    onResetChannel: (CurveChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePoints = curves[activeChannel] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    val latestPoints by rememberUpdatedState(activePoints)
    val latestChannel by rememberUpdatedState(activeChannel)
    val latestUpdate by rememberUpdatedState(onUpdateChannelPoints)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Channel selector header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C0E14))
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurveChannel.entries.forEach { ch ->
                    val selected = ch == activeChannel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSelectChannel(ch) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ch.color)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = ch.label,
                            color = if (selected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .clickable { onResetChannel(activeChannel) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Reset",
                    color = AmberBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Curve Graph Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B0D13))
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .pointerInput(activeChannel) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val relX = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val relY = (1f - offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val xs = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                            val nearestIdx = xs.indices.minByOrNull { abs(xs[it] - relX) } ?: 2
                            val updated = latestPoints.toMutableList()
                            updated[nearestIdx] = relY
                            latestUpdate(latestChannel, updated)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val relX = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val relY = (1f - change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val xs = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                            val nearestIdx = xs.indices.minByOrNull { abs(xs[it] - relX) } ?: 2
                            val updated = latestPoints.toMutableList()
                            updated[nearestIdx] = relY
                            latestUpdate(latestChannel, updated)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Quarter rule grid
                for (g in 1..3) {
                    val f = g / 4f
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(w * f, 0f),
                        end = Offset(w * f, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, h * f),
                        end = Offset(w, h * f),
                        strokeWidth = 1f
                    )
                }

                // Reference diagonal
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                fun buildSmoothCurvePath(pts: List<Float>): Path {
                    val path = Path()
                    val samples = 64
                    for (s in 0..samples) {
                        val t = s.toFloat() / samples
                        val seg = ((t * 4f).toInt()).coerceIn(0, 3)
                        val localT = ((t - seg * 0.25f) / 0.25f).coerceIn(0f, 1f)
                        val p0 = pts[(seg - 1).coerceAtLeast(0)]
                        val p1 = pts[seg]
                        val p2 = pts[(seg + 1).coerceAtMost(4)]
                        val p3 = pts[(seg + 2).coerceAtMost(4)]
                        val t2 = localT * localT
                        val t3 = t2 * localT
                        val yNorm = (0.5f * (
                            2f * p1 +
                                (-p0 + p2) * localT +
                                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
                            )).coerceIn(0f, 1f)

                        val px = t * w
                        val py = h - yNorm * h
                        if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    return path
                }

                // Draw ghost curves for inactive channels
                CurveChannel.entries.filter { it != activeChannel }.forEach { ghost ->
                    val gPts = curves[ghost] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    drawPath(
                        path = buildSmoothCurvePath(gPts),
                        color = ghost.color.copy(alpha = 0.24f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Active curve spline
                drawPath(
                    path = buildSmoothCurvePath(activePoints),
                    color = activeChannel.color,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw 5 control handles
                val xs = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                xs.forEachIndexed { index, nx ->
                    val cx = nx * w
                    val cy = h - activePoints[index].coerceIn(0f, 1f) * h
                    drawCircle(
                        color = DarkroomBg,
                        radius = 5.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = activeChannel.color,
                        radius = 5.5.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Quick Curve Presets Strip
        val presets = listOf(
            "Linear" to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            "S-Contrast" to listOf(0f, 0.17f, 0.5f, 0.83f, 1f),
            "Matte Fade" to listOf(0.09f, 0.26f, 0.5f, 0.75f, 0.94f),
            "Film Roll" to listOf(0.03f, 0.22f, 0.52f, 0.79f, 0.95f),
            "Deep Punch" to listOf(0f, 0.14f, 0.48f, 0.86f, 1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (name, pts) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .clickable { onUpdateChannelPoints(activeChannel, pts) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = name,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
