package com.lumacraft.studio.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------- slider row

@Composable
fun LumaSliderRow(
    label: String,
    value: Float,
    defaultValue: Float = 0f,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null
) {
    val changed = abs(value - defaultValue) > 0.01f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (changed && onReset != null) {
                IconButton(onClick = onReset, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Rounded.Refresh, "Reset $label", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                }
            }
            Text(
                text = value.roundToInt().toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (changed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(34.dp),
                fontSize = 12.sp
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth().height(30.dp)
        )
    }
}

// ---------------------------------------------------------------- switch row

@Composable
fun LumaSwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------------------------------------------------------- section card

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        content()
    }
}

// ---------------------------------------------------------------- chip row

@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        items(options) { opt ->
            val sel = opt == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label(opt),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// ---------------------------------------------------------------- histogram

@Composable
fun HistogramView(bins: IntArray, clipShadow: Boolean, clipHighlight: Boolean, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onSurface
    val warn = MaterialTheme.colorScheme.error
    Canvas(modifier = modifier.clip(RoundedCornerShape(10.dp))) {
        val n = bins.size.coerceAtLeast(1)
        val maxV = (bins.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val barW = size.width / n
        // subtle grid
        for (i in 1..3) {
            drawLine(
                line.copy(alpha = 0.12f),
                Offset(size.width * i / 4f, 0f),
                Offset(size.width * i / 4f, size.height),
                1f
            )
        }
        for (i in bins.indices) {
            val hNorm = bins[i] / maxV
            val barH = hNorm * size.height
            drawRect(
                color = line.copy(alpha = 0.62f),
                topLeft = Offset(i * barW, size.height - barH),
                size = androidx.compose.ui.geometry.Size(barW * 0.82f, barH)
            )
        }
        if (clipShadow) drawRect(color = warn, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(6f, 6f))
        if (clipHighlight) drawRect(color = warn, topLeft = Offset(size.width - 6f, 0f), size = androidx.compose.ui.geometry.Size(6f, 6f))
    }
}

// ---------------------------------------------------------------- curve editor

@Composable
fun CurveEditor(
    points: List<Float>,
    strokeColor: Color,
    onChange: (List<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIdx by remember { mutableStateOf(-1) }
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { off ->
                            val idx = nearestPoint(points, off.x, off.y, size.width, size.height)
                            draggingIdx = idx
                        },
                        onDrag = { change, _ ->
                            val i = draggingIdx
                            if (i in 0..4) {
                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                val ny = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                val newPts = points.toMutableList()
                                // keep monotonic x positions (interior points constrained)
                                val clampedX = when (i) {
                                    0 -> 0f
                                    4 -> 1f
                                    else -> nx.coerceIn(points[i - 1] + 0.02f, points[i + 1] - 0.02f)
                                }
                                newPts[i] = clampedX
                                onChange(newPts)
                            }
                        },
                        onDragEnd = { draggingIdx = -1 }
                    )
                }
        ) {
            // grid
            for (i in 1..3) {
                drawLine(Color.White.copy(alpha = 0.10f), Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), 1f)
                drawLine(Color.White.copy(alpha = 0.10f), Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 1f)
            }
            // spline through 5 points
            val p = points
            val path = Path()
            val steps = 60
            for (s in 0..steps) {
                val t = s / steps.toFloat()
                val seg = (t * 4f).toInt().coerceIn(0, 3)
                val lt = ((t - seg * 0.25f) / 0.25f).coerceIn(0f, 1f)
                val p0 = p[(seg - 1).coerceAtLeast(0)]
                val p1 = p[seg]
                val p2 = p[(seg + 1).coerceAtMost(4)]
                val p3 = p[(seg + 2).coerceAtMost(4)]
                val lt2 = lt * lt
                val lt3 = lt2 * lt
                val x = seg * 0.25f + lt * 0.25f
                val y = 0.5f * (
                    2f * p1 + (-p0 + p2) * lt +
                        (2f * p0 - 5f * p1 + 4f * p2 - p3) * lt2 +
                        (-p0 + 3f * p1 - 3f * p2 + p3) * lt3
                    )
                val px = x * size.width
                val py = (1f - y.coerceIn(0f, 1f)) * size.height
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, strokeColor, style = Stroke(3f))
            // control points
            p.forEachIndexed { i, v ->
                val cx = i / 4f * size.width
                val cy = (1f - v.coerceIn(0f, 1f)) * size.height
                drawCircle(strokeColor, radius = if (i == draggingIdx) 9f else 6f, center = Offset(cx, cy), style = Fill)
            }
        }
    }
}

private fun nearestPoint(points: List<Float>, x: Float, y: Float, w: Int, h: Int): Int {
    var best = 0
    var bestD = Float.MAX_VALUE
    points.forEachIndexed { i, v ->
        val d = abs(x - i / 4f * w) + abs(y - (1f - v) * h)
        if (d < bestD) { bestD = d; best = i }
    }
    return best
}

// ---------------------------------------------------------------- color balance pad

@Composable
fun BalancePad(
    label: String,
    x: Float,
    y: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val nx = ((change.position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                            val ny = (1f - (change.position.y / size.height) * 2f).coerceIn(-1f, 1f)
                            onChange(nx, ny)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                drawLine(Color.White.copy(alpha = 0.14f), Offset(c.x, 0f), Offset(c.x, size.height), 1f)
                drawLine(Color.White.copy(alpha = 0.14f), Offset(0f, c.y), Offset(size.width, c.y), 1f)
                drawCircle(Color.White.copy(alpha = 0.16f), radius = size.minDimension / 4f, center = c, style = Stroke(1f))
                val dotX = (x / 2f + 0.5f) * size.width
                val dotY = (1f - (y / 2f + 0.5f)) * size.height
                drawCircle(Color(0xFF5B7CFA), radius = 8f, center = Offset(dotX, dotY), style = Fill)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
fun LumaTextInputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BusyOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
