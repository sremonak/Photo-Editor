package studio.velox.atelier.ui.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.velox.atelier.model.CurveChannelId
import studio.velox.atelier.model.RgbScopeData
import studio.velox.atelier.ui.theme.ChalkWhite
import studio.velox.atelier.ui.theme.ChampagneBrass
import studio.velox.atelier.ui.theme.KnurledCapsule
import studio.velox.atelier.ui.theme.MachinedBevel
import studio.velox.atelier.ui.theme.ObsidianStage
import studio.velox.atelier.ui.theme.SilverLabel
import kotlin.math.abs

@Composable
fun ProSplineCurve(
    activeChannel: CurveChannelId,
    curves: Map<CurveChannelId, List<Float>>,
    scope: RgbScopeData,
    onSelectChannel: (CurveChannelId) -> Unit,
    onUpdateChannel: (CurveChannelId, List<Float>) -> Unit,
    onResetChannel: (CurveChannelId) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePts = curves[activeChannel] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    val latestPts by rememberUpdatedState(activePts)
    val latestCh by rememberUpdatedState(activeChannel)
    val latestCallback by rememberUpdatedState(onUpdateChannel)

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
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF0C0D12))
                    .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                CurveChannelId.entries.forEach { ch ->
                    val selected = ch == activeChannel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) KnurledCapsule else Color.Transparent)
                            .clickable { onSelectChannel(ch) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ch.strokeColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = ch.title,
                            color = if (selected) ChalkWhite else SilverLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KnurledCapsule)
                    .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                    .clickable { onResetChannel(activeChannel) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "RESET",
                    color = ChampagneBrass,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B0C10))
                .border(1.dp, MachinedBevel, RoundedCornerShape(14.dp))
                .pointerInput(activeChannel) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val rx = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val ry = (1f - pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val xs = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                            val idx = xs.indices.minByOrNull { abs(xs[it] - rx) } ?: 2
                            val next = latestPts.toMutableList().apply { this[idx] = ry }
                            latestCallback(latestCh, next)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val rx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val ry = (1f - change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val xs = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                            val idx = xs.indices.minByOrNull { abs(xs[it] - rx) } ?: 2
                            val next = latestPts.toMutableList().apply { this[idx] = ry }
                            latestCallback(latestCh, next)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Live Histogram Silhouette from Real Pixel Engine!
                val bins = when (activeChannel) {
                    CurveChannelId.RED -> scope.rBins
                    CurveChannelId.GREEN -> scope.gBins
                    CurveChannelId.BLUE -> scope.bBins
                    CurveChannelId.LUMA -> scope.lumaBins
                }
                if (bins.isNotEmpty()) {
                    val histPath = Path().apply {
                        moveTo(0f, h)
                        bins.forEachIndexed { idx, amp ->
                            val hx = (idx.toFloat() / (bins.size - 1)) * w
                            val hy = h - amp * (h * 0.80f)
                            lineTo(hx, hy)
                        }
                        lineTo(w, h)
                        close()
                    }
                    drawPath(histPath, color = activeChannel.strokeColor.copy(alpha = 0.12f))
                }

                // Rule of quarters grid
                for (q in 1..3) {
                    val f = q / 4f
                    drawLine(Color.White.copy(alpha = 0.06f), Offset(w * f, 0f), Offset(w * f, h), 1f)
                    drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, h * f), Offset(w, h * f), 1f)
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                fun splinePath(pts: List<Float>): Path {
                    val path = Path()
                    val steps = 64
                    for (s in 0..steps) {
                        val t = s.toFloat() / steps
                        val seg = (t * 4f).toInt().coerceIn(0, 3)
                        val lt = ((t - seg * 0.25f) / 0.25f).coerceIn(0f, 1f)
                        val p0 = pts[(seg - 1).coerceAtLeast(0)]
                        val p1 = pts[seg]
                        val p2 = pts[(seg + 1).coerceAtMost(4)]
                        val p3 = pts[(seg + 2).coerceAtMost(4)]
                        val lt2 = lt * lt
                        val lt3 = lt2 * lt
                        val yn = (0.5f * (
                            2f * p1 +
                                (-p0 + p2) * lt +
                                (2f * p0 - 5f * p1 + 4f * p2 - p3) * lt2 +
                                (-p0 + 3f * p1 - 3f * p2 + p3) * lt3
                            )).coerceIn(0f, 1f)
                        val px = t * w
                        val py = h - yn * h
                        if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    return path
                }

                // Ghost curves
                CurveChannelId.entries.filter { it != activeChannel }.forEach { ch ->
                    val gPts = curves[ch] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    drawPath(
                        path = splinePath(gPts),
                        color = ch.strokeColor.copy(alpha = 0.22f),
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Active curve
                drawPath(
                    path = splinePath(activePts),
                    color = activeChannel.strokeColor,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                val xs = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                xs.forEachIndexed { idx, nx ->
                    val cx = nx * w
                    val cy = h - activePts[idx].coerceIn(0f, 1f) * h
                    drawCircle(ObsidianStage, radius = 5.5.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(
                        activeChannel.strokeColor,
                        radius = 5.5.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val curvePresets = listOf(
            "Linear" to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            "Cinema S" to listOf(0.01f, 0.17f, 0.51f, 0.83f, 0.99f),
            "Silver Paper" to listOf(0.08f, 0.25f, 0.50f, 0.76f, 0.94f),
            "Shoulder Roll" to listOf(0.02f, 0.23f, 0.53f, 0.79f, 0.93f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            curvePresets.forEach { (label, pts) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(KnurledCapsule)
                        .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                        .clickable { onUpdateChannel(activeChannel, pts) }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = SilverLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
