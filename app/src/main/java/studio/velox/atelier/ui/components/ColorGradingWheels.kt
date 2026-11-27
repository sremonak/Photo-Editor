package studio.velox.atelier.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.velox.atelier.model.WheelGrade
import studio.velox.atelier.ui.theme.ChalkWhite
import studio.velox.atelier.ui.theme.ChampagneBrass
import studio.velox.atelier.ui.theme.KnurledCapsule
import studio.velox.atelier.ui.theme.LeicaVermilion
import studio.velox.atelier.ui.theme.MachinedBevel
import studio.velox.atelier.ui.theme.ObsidianStage
import studio.velox.atelier.ui.theme.PewterMuted
import studio.velox.atelier.ui.theme.SilverLabel
import kotlin.math.hypot

@Composable
fun ColorGradingWheels(
    shadows: WheelGrade,
    midtones: WheelGrade,
    highlights: WheelGrade,
    onShadowsChange: (WheelGrade) -> Unit,
    onMidtonesChange: (WheelGrade) -> Unit,
    onHighlightsChange: (WheelGrade) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = "3-WAY CINEMA COLOR BALANCE",
                color = SilverLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KnurledCapsule)
                    .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                    .clickable { onResetAll() }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "ZERO WHEELS",
                    color = ChampagneBrass,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleTrackballWheel(
                title = "SHADOWS",
                grade = shadows,
                onChange = onShadowsChange,
                modifier = Modifier.weight(1f)
            )
            SingleTrackballWheel(
                title = "MIDTONES",
                grade = midtones,
                onChange = onMidtonesChange,
                modifier = Modifier.weight(1f)
            )
            SingleTrackballWheel(
                title = "HIGHLIGHTS",
                grade = highlights,
                onChange = onHighlightsChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SingleTrackballWheel(
    title: String,
    grade: WheelGrade,
    onChange: (WheelGrade) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestGrade by rememberUpdatedState(grade)
    val latestOnChange by rememberUpdatedState(onChange)
    val active = hypot(grade.offsetX, grade.offsetY) > 0.03f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (active) ChampagneBrass.copy(alpha = 0.7f) else MachinedBevel,
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val r = size.width * 0.5f
                            var nx = (pos.x - r) / r
                            var ny = (pos.y - r) / r
                            val dist = hypot(nx, ny)
                            if (dist > 1f) {
                                nx /= dist
                                ny /= dist
                            }
                            latestOnChange(latestGrade.copy(offsetX = nx, offsetY = ny))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val r = size.width * 0.5f
                            var nx = (change.position.x - r) / r
                            var ny = (change.position.y - r) / r
                            val dist = hypot(nx, ny)
                            if (dist > 1f) {
                                nx /= dist
                                ny /= dist
                            }
                            latestOnChange(latestGrade.copy(offsetX = nx, offsetY = ny))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension * 0.5f
                val centerPt = Offset(size.width * 0.5f, size.height * 0.5f)

                // Sweep chromatic spectrum ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF4D2D), // 0 rad: Warm Red-Orange
                            Color(0xFFFACC15), // Yellow
                            Color(0xFF34D399), // Green
                            Color(0xFF38BDF8), // Cyan/Teal
                            Color(0xFF6366F1), // Blue/Indigo
                            Color(0xFFEC4899), // Magenta
                            Color(0xFFFF4D2D)
                        ),
                        center = centerPt
                    ),
                    radius = radius,
                    center = centerPt,
                    alpha = 0.48f
                )

                // Desaturate center smoothly into dark neutral darkroom core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xF2121319),
                            Color(0x99121319),
                            Color.Transparent
                        ),
                        center = centerPt,
                        radius = radius
                    ),
                    radius = radius,
                    center = centerPt
                )

                // Precision crosshair axes
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, centerPt.y),
                    end = Offset(size.width, centerPt.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(centerPt.x, 0f),
                    end = Offset(centerPt.x, size.height),
                    strokeWidth = 1f
                )

                // Vector line + Draggable Puck
                val puckCenter = Offset(
                    x = centerPt.x + grade.offsetX * (radius * 0.82f),
                    y = centerPt.y + grade.offsetY * (radius * 0.82f)
                )
                drawLine(
                    color = ChampagneBrass.copy(alpha = 0.65f),
                    start = centerPt,
                    end = puckCenter,
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(
                    color = ObsidianStage,
                    radius = 6.dp.toPx(),
                    center = puckCenter
                )
                drawCircle(
                    color = if (active) LeicaVermilion else ChalkWhite,
                    radius = 6.dp.toPx(),
                    center = puckCenter,
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = title,
            color = if (active) ChalkWhite else PewterMuted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            modifier = Modifier.clickable { onChange(WheelGrade()) }
        )
    }
}
