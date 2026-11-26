package com.luminapro.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminapro.photoeditor.ui.theme.CrimsonClip
import com.luminapro.photoeditor.ui.theme.NeuralCyan
import com.luminapro.photoeditor.ui.theme.StudioBorder
import com.luminapro.photoeditor.ui.theme.TextSecondary
import kotlin.math.exp
import kotlin.math.pow

@Composable
fun HistogramOverlay(
    exposureShift: Float,
    contrastShift: Float,
    warmthShift: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD90A0C12))
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.width(124.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RGB WAVEFORM",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (exposureShift < -35f) NeuralCyan else Color(0xFF374151))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (exposureShift > 35f) CrimsonClip else Color(0xFF374151))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(modifier = Modifier.size(width = 124.dp, height = 40.dp)) {
            val w = size.width
            val h = size.height

            // Grid quarters
            for (q in 1..3) {
                val gx = w * (q / 4f)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 1f
                )
            }

            val centerShift = (exposureShift / 100f) * 0.22f
            val spread = (0.20f - (contrastShift / 100f) * 0.06f).coerceIn(0.09f, 0.30f)
            val warmOffset = (warmthShift / 100f) * 0.08f

            fun channelWave(meanA: Float, meanB: Float, color: Color) {
                val path = Path().apply {
                    moveTo(0f, h)
                    val steps = 36
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps
                        val g1 = exp(-((t - meanA).pow(2)) / (2f * spread * spread))
                        val g2 = 0.65f * exp(-((t - meanB).pow(2)) / (2f * (spread * 0.8f).pow(2)))
                        val amp = (g1 * 0.72f + g2 * 0.45f).coerceIn(0.03f, 0.96f)
                        lineTo(t * w, h - amp * (h - 2f))
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path = path, color = color)
            }

            channelWave(
                meanA = (0.56f + centerShift + warmOffset).coerceIn(0.1f, 0.9f),
                meanB = (0.30f + centerShift).coerceIn(0.08f, 0.85f),
                color = Color(0x59EF4444)
            )
            channelWave(
                meanA = (0.48f + centerShift).coerceIn(0.1f, 0.9f),
                meanB = (0.68f + centerShift).coerceIn(0.1f, 0.92f),
                color = Color(0x5222C55E)
            )
            channelWave(
                meanA = (0.40f + centerShift - warmOffset).coerceIn(0.08f, 0.9f),
                meanB = (0.64f + centerShift - warmOffset).coerceIn(0.1f, 0.9f),
                color = Color(0x593B82F6)
            )
        }
    }
}
