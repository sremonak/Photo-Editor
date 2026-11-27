package studio.velox.atelier.ui.components

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
import studio.velox.atelier.model.RgbScopeData
import studio.velox.atelier.ui.theme.CobaltMask
import studio.velox.atelier.ui.theme.LeicaVermilion
import studio.velox.atelier.ui.theme.MachinedBevel
import studio.velox.atelier.ui.theme.SilverLabel

@Composable
fun RgbParadeScope(
    scope: RgbScopeData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD90A0B0F))
            .border(1.dp, MachinedBevel, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.width(126.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RGB PARADE",
                color = SilverLabel,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.5.dp)
                        .clip(CircleShape)
                        .background(if (scope.clippedShadows) CobaltMask else Color(0xFF2D313F))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.5.dp)
                        .clip(CircleShape)
                        .background(if (scope.clippedHighlights) LeicaVermilion else Color(0xFF2D313F))
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Canvas(modifier = Modifier.size(width = 126.dp, height = 38.dp)) {
            val w = size.width
            val h = size.height
            val thirdW = w / 3f

            drawLine(Color.White.copy(alpha = 0.08f), Offset(thirdW, 0f), Offset(thirdW, h), 1f)
            drawLine(Color.White.copy(alpha = 0.08f), Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, h), 1f)

            fun drawBand(bins: FloatArray, startX: Float, bandW: Float, fill: Color) {
                if (bins.isEmpty()) return
                val p = Path().apply {
                    moveTo(startX, h)
                    bins.forEachIndexed { idx, v ->
                        val bx = startX + (idx.toFloat() / (bins.size - 1)) * (bandW - 2f)
                        val by = h - v.coerceIn(0f, 1f) * (h - 2f)
                        lineTo(bx, by)
                    }
                    lineTo(startX + bandW - 2f, h)
                    close()
                }
                drawPath(p, color = fill)
            }

            drawBand(scope.rBins, 0f, thirdW, Color(0xA6FF4D2D))
            drawBand(scope.gBins, thirdW + 1f, thirdW, Color(0xA634D399))
            drawBand(scope.bBins, thirdW * 2f + 1f, thirdW, Color(0xA660A5FA))
        }
    }
}
