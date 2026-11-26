package com.luminapro.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.luminapro.photoeditor.model.EditorState
import com.luminapro.photoeditor.model.EditorTool
import com.luminapro.photoeditor.ui.theme.AmberBright
import com.luminapro.photoeditor.ui.theme.AmberGold
import com.luminapro.photoeditor.ui.theme.DarkroomBg
import com.luminapro.photoeditor.ui.theme.StudioBorder
import kotlin.math.pow
import kotlin.math.roundToInt

fun buildStudioColorMatrix(state: EditorState): ColorMatrix {
    val gradeLayerOn = state.layers.find { it.id == "grade" }?.visible ?: true
    if (!gradeLayerOn) return ColorMatrix()

    val expGain = 2f.pow((state.exposure + if (state.aiAutoEnhance) 8f else 0f) / 75f)
    val cDelta = (state.contrast + state.clarity * 0.3f) / 100f
    val contrastScale = (1f + cDelta * 0.75f).coerceIn(0.35f, 2.2f)
    val translate = 128f * (1f - contrastScale) + state.shadows * 0.25f - state.blacks * 0.2f + state.fade * 0.45f

    val temp = state.temperature / 100f
    val tint = state.tint / 100f

    val rScale = expGain * contrastScale * (1f + temp * 0.18f + tint * 0.05f)
    val gScale = expGain * contrastScale * (1f - tint * 0.12f)
    val bScale = expGain * contrastScale * (1f - temp * 0.18f + tint * 0.05f)

    val scaleMatrix = ColorMatrix(
        floatArrayOf(
            rScale, 0f, 0f, 0f, translate,
            0f, gScale, 0f, 0f, translate,
            0f, 0f, bScale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val satTotal = (1f + (state.saturation + state.vibrance * 0.65f) / 100f).coerceIn(0f, 2.5f)
    val satMatrix = ColorMatrix().apply { setToSaturation(satTotal) }
    scaleMatrix.timesAssign(satMatrix)
    return scaleMatrix
}

@Composable
fun StudioViewportCanvas(
    state: EditorState,
    onSplitPositionChange: (Float) -> Unit,
    onRelightMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxW by remember { mutableIntStateOf(1) }
    var boxH by remember { mutableIntStateOf(1) }
    val latestSplitCallback by rememberUpdatedState(onSplitPositionChange)
    val latestRelightCallback by rememberUpdatedState(onRelightMove)

    val frameEnabled = state.exifBorder || (state.layers.find { it.id == "frame" }?.visible == true && state.exifBorder)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkroomBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (frameEnabled) 6.dp else 16.dp))
                .background(if (frameEnabled) Color(0xFFF9FAFB) else Color(0xFF0E1016))
                .border(
                    width = 1.dp,
                    color = if (frameEnabled) Color.White.copy(alpha = 0.8f) else StudioBorder,
                    shape = RoundedCornerShape(if (frameEnabled) 6.dp else 16.dp)
                )
                .padding(if (frameEnabled) 10.dp else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(if (frameEnabled) 2.dp else 16.dp))
                    .onSizeChanged {
                        boxW = it.width.coerceAtLeast(1)
                        boxH = it.height.coerceAtLeast(1)
                    }
                    .pointerInput(state.showSplitComparison, state.activeTool, state.aiRelightEnabled) {
                        detectDragGestures { change, _ ->
                            val nx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                            val ny = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                            if (state.showSplitComparison) {
                                change.consume()
                                latestSplitCallback(nx)
                            } else if (state.activeTool == EditorTool.AI_LAB && state.aiRelightEnabled) {
                                change.consume()
                                latestRelightCallback(nx, ny)
                            }
                        }
                    }
            ) {
                val colorMatrix = remember(state) { buildStudioColorMatrix(state) }

                if (state.pickedImageUri != null) {
                    // Base unedited photo (visible on left side when Split Before/After is on)
                    AsyncImage(
                        model = state.pickedImageUri,
                        contentDescription = "Original Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = state.rotationQuarter * 90f + state.straightenAngle
                                scaleX = if (state.flipHorizontal) -1f else 1f
                            }
                    )

                    // Graded photo clipped from splitPosition..1f (or full width)
                    val clipLeftFraction = if (state.showSplitComparison) state.splitPosition else 0f
                    AsyncImage(
                        model = state.pickedImageUri,
                        contentDescription = "Edited Photo",
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(left = size.width * clipLeftFraction) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                            .graphicsLayer {
                                rotationZ = state.rotationQuarter * 90f + state.straightenAngle
                                scaleX = if (state.flipHorizontal) -1f else 1f
                            }
                    )
                } else {
                    // Rich built-in HDR Studio RAW Scene Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = state.rotationQuarter * 90f + state.straightenAngle
                                scaleX = if (state.flipHorizontal) -1f else 1f
                            }
                    ) {
                        if (state.showSplitComparison) {
                            val splitX = size.width * state.splitPosition
                            clipRect(right = splitX) {
                                drawProceduralRawScene(state.activeSample.sceneIndex, graded = false, state = state)
                            }
                            clipRect(left = splitX) {
                                drawProceduralRawScene(state.activeSample.sceneIndex, graded = true, state = state)
                            }
                        } else {
                            drawProceduralRawScene(state.activeSample.sceneIndex, graded = true, state = state)
                        }
                    }
                }

                // AI Relight & Vignette Overlays
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val aiOn = state.layers.find { it.id == "ai" }?.visible ?: true
                    if (aiOn && state.aiRelightEnabled && state.aiRelightIntensity > 0f) {
                        val center = Offset(size.width * state.aiRelightX, size.height * state.aiRelightY)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFB74D).copy(alpha = (state.aiRelightIntensity / 100f) * 0.42f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension * 0.62f
                            ),
                            radius = size.maxDimension * 0.62f,
                            center = center
                        )
                    }

                    if (state.vignette > 0f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = (state.vignette / 100f) * 0.68f)
                                ),
                                center = center,
                                radius = size.maxDimension * 0.68f
                            )
                        )
                    }

                    // Crop Rule-of-Thirds Grid
                    if (state.activeTool == EditorTool.CROP) {
                        for (i in 1..2) {
                            val gx = size.width * (i / 3f)
                            val gy = size.height * (i / 3f)
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(gx, 0f), Offset(gx, size.height), 1.2f)
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, gy), Offset(size.width, gy), 1.2f)
                        }
                        drawRect(
                            color = AmberGold,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Floating Live RGB Waveform Histogram (Top-Left)
                if (state.showHistogram) {
                    HistogramOverlay(
                        exposureShift = state.exposure,
                        contrastShift = state.contrast,
                        warmthShift = state.temperature,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }

                // Format / Bit-depth Badge (Top-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC0A0C12))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.pickedImageUri != null) "GALLERY · 16-BIT" else state.activeSample.formatBadge,
                        color = AmberBright,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                // Draggable AI Key-Light Sun Handle when in AI Lab
                if (state.activeTool == EditorTool.AI_LAB && state.aiRelightEnabled) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (boxW * state.aiRelightX).roundToInt() - 18.dp.roundToPx(),
                                    y = (boxH * state.aiRelightY).roundToInt() - 18.dp.roundToPx()
                                )
                            }
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberGold.copy(alpha = 0.25f))
                            .border(1.5.dp, AmberBright, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = "3D Key Light",
                            tint = AmberBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Interactive Split-Screen Before/After Comparison Divider
                if (state.showSplitComparison) {
                    val splitOffsetPx = (boxW * state.splitPosition).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(splitOffsetPx - 1, 0) }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = splitOffsetPx - 17.dp.roundToPx(),
                                    y = (boxH * 0.5f).roundToInt() - 17.dp.roundToPx()
                                )
                            }
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0B0D13))
                            .border(1.5.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CompareArrows,
                            contentDescription = "Split Wipe",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ViewportPillTag("BEFORE · RAW", Color.White.copy(alpha = 0.85f))
                        ViewportPillTag("AFTER · GRADED", AmberBright)
                    }
                }

                // Bottom EXIF Metadata Strip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xBF090B10))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${state.activeSample.cameraSpec}  ·  ${state.activeSample.lensExif}",
                        color = Color(0xFFD1D5DB),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }

            // Leica / Studio EXIF Gallery Frame Footer
            if (frameEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = state.activeSample.cameraSpec,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = state.activeSample.lensExif,
                            color = Color(0xFF6B7280),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48))
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewportPillTag(label: String, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC090A0F))
            .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

/**
 * Draws lush atmospheric RAW test scenes so the editor viewport looks rich even before picking a gallery image.
 */
fun DrawScope.drawProceduralRawScene(sceneIndex: Int, graded: Boolean, state: EditorState) {
    val w = size.width
    val h = size.height

    val expBoost = if (graded) (state.exposure / 100f) * 0.25f else 0f
    val warmBoost = if (graded) (state.temperature / 100f) * 0.22f else 0f
    val mono = graded && state.saturation <= -85f

    fun adjust(c: Color): Color {
        if (!graded) return c
        var r = (c.red + expBoost + warmBoost).coerceIn(0f, 1f)
        var g = (c.green + expBoost).coerceIn(0f, 1f)
        var b = (c.blue + expBoost - warmBoost).coerceIn(0f, 1f)
        if (mono) {
            val l = 0.299f * r + 0.587f * g + 0.114f * b
            r = l; g = l; b = l
        }
        return Color(r, g, b, c.alpha)
    }

    when (sceneIndex % 4) {
        0 -> {
            // Scene 0: Editorial Golden Hour Studio Portrait Silhouette & Bokeh
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        adjust(Color(0xFF1A1423)),
                        adjust(Color(0xFF4A2525)),
                        adjust(Color(0xFFD97736)),
                        adjust(Color(0xFF1E1926))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            // Golden window light beam
            val beam = Path().apply {
                moveTo(w * 0.1f, 0f)
                lineTo(w * 0.68f, 0f)
                lineTo(w * 0.95f, h)
                lineTo(w * 0.28f, h)
                close()
            }
            drawPath(
                path = beam,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        adjust(Color(0x66FBBF24)),
                        adjust(Color(0x1AF59E0B))
                    )
                )
            )
            // Soft background f/1.4 bokeh orbs
            drawCircle(adjust(Color(0x55FDE68A)), radius = w * 0.14f, center = Offset(w * 0.24f, h * 0.28f))
            drawCircle(adjust(Color(0x40FB923C)), radius = w * 0.10f, center = Offset(w * 0.78f, h * 0.22f))
            drawCircle(adjust(Color(0x3338BDF8)), radius = w * 0.09f, center = Offset(w * 0.82f, h * 0.64f))

            // Subject portrait rim-lit silhouette
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(adjust(Color(0xFFF59E0B)), adjust(Color(0xFF231820))),
                    center = Offset(w * 0.46f, h * 0.36f),
                    radius = w * 0.20f
                ),
                radius = w * 0.155f,
                center = Offset(w * 0.50f, h * 0.38f)
            )
            val shoulders = Path().apply {
                moveTo(w * 0.20f, h)
                quadraticTo(w * 0.26f, h * 0.56f, w * 0.50f, h * 0.56f)
                quadraticTo(w * 0.74f, h * 0.56f, w * 0.80f, h)
                close()
            }
            drawPath(
                path = shoulders,
                brush = Brush.horizontalGradient(
                    colors = listOf(adjust(Color(0xFF2D1B22)), adjust(Color(0xFF141118)))
                )
            )
        }

        1 -> {
            // Scene 1: Tokyo Cyber Rain Neon Alley
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        adjust(Color(0xFF090D1A)),
                        adjust(Color(0xFF161B33)),
                        adjust(Color(0xFF231538)),
                        adjust(Color(0xFF0A0E17))
                    )
                )
            )
            // Glowing neon signage & wet street reflections
            drawRect(adjust(Color(0x9906B6D4)), topLeft = Offset(w * 0.14f, h * 0.18f), size = Size(w * 0.08f, h * 0.28f))
            drawRect(adjust(Color(0x99EC4899)), topLeft = Offset(w * 0.76f, h * 0.14f), size = Size(w * 0.09f, h * 0.32f))
            drawCircle(adjust(Color(0x66F59E0B)), radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.54f))
            // Wet asphalt reflection bands
            for (r in 0..8) {
                val ry = h * (0.64f + r * 0.04f)
                drawLine(
                    color = adjust(if (r % 2 == 0) Color(0x6622D3EE) else Color(0x66F472B6)),
                    start = Offset(w * (0.15f + r * 0.02f), ry),
                    end = Offset(w * (0.85f - r * 0.02f), ry),
                    strokeWidth = 6f
                )
            }
        }

        2 -> {
            // Scene 2: Dolomites Alpenglow Sunset & Mirror Lake
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        adjust(Color(0xFF1E293B)),
                        adjust(Color(0xFF7C2D12)),
                        adjust(Color(0xFFF97316)),
                        adjust(Color(0xFF0F766E)),
                        adjust(Color(0xFF0B1320))
                    )
                )
            )
            val peaks = Path().apply {
                moveTo(0f, h * 0.58f)
                lineTo(w * 0.24f, h * 0.28f)
                lineTo(w * 0.42f, h * 0.44f)
                lineTo(w * 0.64f, h * 0.20f)
                lineTo(w * 0.84f, h * 0.38f)
                lineTo(w, h * 0.30f)
                lineTo(w, h * 0.58f)
                close()
            }
            drawPath(peaks, adjust(Color(0xFF1E1B2E)))
        }

        else -> {
            // Scene 3: Atmospheric Architectural Shadows
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        adjust(Color(0xFF18181B)),
                        adjust(Color(0xFF3F3F46)),
                        adjust(Color(0xFFD4D4D8)),
                        adjust(Color(0xFF09090B))
                    )
                )
            )
            drawCircle(adjust(Color(0x44FBBF24)), radius = w * 0.28f, center = Offset(w * 0.65f, h * 0.35f))
        }
    }
}
