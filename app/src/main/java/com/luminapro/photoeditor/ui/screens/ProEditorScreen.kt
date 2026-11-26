package com.luminapro.photoeditor.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoFilter
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminapro.photoeditor.model.EditorState
import com.luminapro.photoeditor.model.EditorTool
import com.luminapro.photoeditor.model.STUDIO_LUTS
import com.luminapro.photoeditor.ui.components.PrecisionDial
import com.luminapro.photoeditor.ui.components.StudioViewportCanvas
import com.luminapro.photoeditor.ui.components.ToneCurveEditor
import com.luminapro.photoeditor.ui.components.drawProceduralRawScene
import com.luminapro.photoeditor.ui.theme.AmberBright
import com.luminapro.photoeditor.ui.theme.AmberGold
import com.luminapro.photoeditor.ui.theme.DarkroomBg
import com.luminapro.photoeditor.ui.theme.EmeraldOk
import com.luminapro.photoeditor.ui.theme.NeuralCyan
import com.luminapro.photoeditor.ui.theme.StudioBorder
import com.luminapro.photoeditor.ui.theme.StudioCard
import com.luminapro.photoeditor.ui.theme.StudioElevated
import com.luminapro.photoeditor.ui.theme.StudioSurface
import com.luminapro.photoeditor.ui.theme.TextMuted
import com.luminapro.photoeditor.ui.theme.TextPrimary
import com.luminapro.photoeditor.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProEditorScreen(
    state: EditorState,
    canUndo: Boolean,
    canRedo: Boolean,
    historyCount: Int,
    onUpdateState: (EditorState) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit
) {
    var showExportSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkroomBg)
            .statusBarsPadding()
    ) {
        // 1. Top Precision Darkroom Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopBarIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    active = false,
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(6.dp))
                TopBarIconButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    active = false,
                    enabled = canUndo,
                    onClick = onUndo
                )
                Spacer(modifier = Modifier.width(4.dp))
                TopBarIconButton(
                    icon = Icons.AutoMirrored.Rounded.Redo,
                    active = false,
                    enabled = canRedo,
                    onClick = onRedo
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCard)
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$historyCount STEPS",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live RGB Waveform Histogram Toggle
                TopBarIconButton(
                    icon = Icons.Rounded.BarChart,
                    active = state.showHistogram,
                    onClick = { onUpdateState(state.copy(showHistogram = !state.showHistogram)) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Split-Screen Before / After Comparison Toggle
                TopBarIconButton(
                    icon = Icons.Rounded.Compare,
                    active = state.showSplitComparison,
                    onClick = {
                        onUpdateState(state.copy(showSplitComparison = !state.showSplitComparison))
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Export Primary CTA Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AmberGold, AmberBright))
                        )
                        .clickable { showExportSheet = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.IosShare,
                        contentDescription = "Export",
                        tint = DarkroomBg,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Export",
                        color = DarkroomBg,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 2. Center Interactive Photo Viewport
        StudioViewportCanvas(
            state = state,
            onSplitPositionChange = { pos ->
                onUpdateState(state.copy(splitPosition = pos))
            },
            onRelightMove = { rx, ry ->
                onUpdateState(state.copy(aiRelightX = rx, aiRelightY = ry))
            },
            modifier = Modifier.weight(1f)
        )

        // 3. Elevated Darkroom Studio Workspace + Bottom Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .border(1.dp, StudioBorder)
                .navigationBarsPadding()
        ) {
            // Active Tool Controls Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                when (state.activeTool) {
                    EditorTool.LUTS -> LutPresetPanel(state = state, onUpdateState = onUpdateState)
                    EditorTool.LIGHT -> LightAdjustmentPanel(state = state, onUpdateState = onUpdateState)
                    EditorTool.CURVES -> ToneCurveEditor(
                        activeChannel = state.selectedCurveChannel,
                        curves = state.curves,
                        onSelectChannel = { ch ->
                            onUpdateState(state.copy(selectedCurveChannel = ch))
                        },
                        onUpdateChannelPoints = { ch, pts ->
                            val updatedMap = state.curves.toMutableMap().apply { put(ch, pts) }
                            onUpdateState(state.copy(curves = updatedMap))
                        },
                        onResetChannel = { ch ->
                            val updatedMap = state.curves.toMutableMap().apply {
                                put(ch, listOf(0f, 0.25f, 0.5f, 0.75f, 1f))
                            }
                            onUpdateState(state.copy(curves = updatedMap))
                        }
                    )
                    EditorTool.HSL -> HslColorPanel(state = state, onUpdateState = onUpdateState)
                    EditorTool.OPTICS -> OpticsEffectsPanel(state = state, onUpdateState = onUpdateState)
                    EditorTool.AI_LAB -> AiNeuralLabPanel(state = state, onUpdateState = onUpdateState)
                    EditorTool.CROP -> CropTransformPanel(state = state, onUpdateState = onUpdateState)
                    EditorTool.LAYERS -> NonDestructiveLayersPanel(state = state, onUpdateState = onUpdateState)
                }
            }

            // 8-Module Studio Tool Dock
            val toolIcons: Map<EditorTool, ImageVector> = mapOf(
                EditorTool.LUTS to Icons.Rounded.PhotoFilter,
                EditorTool.LIGHT to Icons.Rounded.WbSunny,
                EditorTool.CURVES to Icons.Rounded.Timeline,
                EditorTool.HSL to Icons.Rounded.ColorLens,
                EditorTool.OPTICS to Icons.Rounded.Tune,
                EditorTool.AI_LAB to Icons.Rounded.AutoAwesome,
                EditorTool.CROP to Icons.Rounded.CropRotate,
                EditorTool.LAYERS to Icons.Rounded.Layers
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0E14))
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(EditorTool.entries) { tool ->
                    val selected = state.activeTool == tool
                    val icon = toolIcons[tool] ?: Icons.Rounded.Tune
                    val tint = when {
                        selected && tool == EditorTool.AI_LAB -> NeuralCyan
                        selected -> AmberBright
                        else -> TextMuted
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent
                            )
                            .clickable { onUpdateState(state.copy(activeTool = tool)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.label,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tool.label,
                            color = if (selected) TextPrimary else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(if (selected) tint else Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    // Native Material 3 Export Bottom Sheet
    if (showExportSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedFormat by remember { mutableStateOf("JPG") }
        var selectedColorSpace by remember { mutableStateOf("Display P3") }
        var exportQuality by remember { mutableFloatStateOf(95f) }

        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = StudioSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export Master Render",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "6000 × 4000 px · ~6.8 MB Estimated",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldOk.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "16-BIT READY",
                            color = EmeraldOk,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Format Selector
                Text("FORMAT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("JPG", "PNG 16-bit", "WEBP", "DNG RAW").forEach { fmt ->
                        val active = selectedFormat == fmt
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) AmberGold.copy(alpha = 0.18f) else StudioCard)
                                .border(
                                    1.dp,
                                    if (active) AmberGold else StudioBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedFormat = fmt }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = fmt,
                                color = if (active) AmberBright else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Color Space Selector
                Text("COLOR PROFILE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("sRGB", "Display P3", "Adobe RGB").forEach { space ->
                        val active = selectedColorSpace == space
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) NeuralCyan.copy(alpha = 0.16f) else StudioCard)
                                .border(
                                    1.dp,
                                    if (active) NeuralCyan else StudioBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedColorSpace = space }
                                .padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = space,
                                color = if (active) NeuralCyan else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Quality Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("COMPRESSION QUALITY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${exportQuality.roundToInt()}%",
                            color = AmberBright,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = exportQuality,
                        onValueChange = { exportQuality = it },
                        valueRange = 60f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberBright,
                            activeTrackColor = AmberGold
                        )
                    )
                }

                // Leica EXIF Frame Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioCard)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Leica Camera EXIF Gallery Border", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Embed lens focal length, aperture & ISO footer", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = state.exifBorder,
                        onCheckedChange = { onUpdateState(state.copy(exifBorder = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkroomBg,
                            checkedTrackColor = AmberGold
                        )
                    )
                }

                // Save Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(AmberGold, AmberBright)))
                        .clickable {
                            showExportSheet = false
                            Toast.makeText(
                                context,
                                "Saved $selectedFormat ($selectedColorSpace) to /DCIM/LuminaPro",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = DarkroomBg,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Master to Gallery",
                        color = DarkroomBg,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) AmberGold.copy(alpha = 0.18f) else StudioElevated)
            .border(
                width = 1.dp,
                color = if (active) AmberGold else StudioBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(enabled = enabled) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> TextMuted.copy(alpha = 0.35f)
                active -> AmberBright
                else -> TextPrimary
            },
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun LutPresetPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(STUDIO_LUTS) { lut ->
                val active = state.activePresetId == lut.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) StudioCard else StudioElevated)
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) AmberGold else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onUpdateState(
                                state.copy(
                                    activePresetId = lut.id,
                                    exposure = lut.exposure,
                                    contrast = lut.contrast,
                                    temperature = lut.warmth,
                                    saturation = lut.saturation,
                                    vignette = lut.vignette,
                                    fade = lut.fade
                                )
                            )
                        }
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        val previewState = state.copy(
                            exposure = lut.exposure,
                            temperature = lut.warmth,
                            saturation = lut.saturation
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawProceduralRawScene(state.activeSample.sceneIndex, graded = true, state = previewState)
                        }
                        if (lut.badge != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xCC090A0D))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = lut.badge,
                                    color = lut.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = lut.name,
                        color = if (active) AmberBright else TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        PrecisionDial(
            label = "LUT Strength",
            value = state.presetStrength,
            valueRange = 0f..100f,
            defaultValue = 85f,
            unitSuffix = "%",
            onValueChange = { onUpdateState(state.copy(presetStrength = it)) }
        )
    }
}

@Composable
private fun LightAdjustmentPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    val params = listOf(
        "Exposure" to state.exposure,
        "Contrast" to state.contrast,
        "Highlights" to state.highlights,
        "Shadows" to state.shadows,
        "Whites" to state.whites,
        "Blacks" to state.blacks
    )

    val activeVal = params.find { it.first == state.selectedLightParam }?.second ?: 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(params) { (name, v) ->
                val selected = state.selectedLightParam == name
                val nonZero = v.roundToInt() != 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AmberGold.copy(alpha = 0.16f) else StudioElevated)
                        .border(
                            1.dp,
                            if (selected) AmberGold else StudioBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onUpdateState(state.copy(selectedLightParam = name)) }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = name,
                        color = if (selected) AmberBright else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (nonZero) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (v > 0) "+${v.roundToInt()}" else "${v.roundToInt()}",
                            color = AmberBright,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        PrecisionDial(
            label = state.selectedLightParam,
            value = activeVal,
            valueRange = -100f..100f,
            defaultValue = 0f,
            onValueChange = { next ->
                val updated = when (state.selectedLightParam) {
                    "Exposure" -> state.copy(exposure = next)
                    "Contrast" -> state.copy(contrast = next)
                    "Highlights" -> state.copy(highlights = next)
                    "Shadows" -> state.copy(shadows = next)
                    "Whites" -> state.copy(whites = next)
                    else -> state.copy(blacks = next)
                }
                onUpdateState(updated)
            }
        )
    }
}

@Composable
private fun HslColorPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Sub-mode switcher: White Balance vs 8-Band HSL Mixer
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0C0E14))
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                .padding(3.dp)
        ) {
            listOf("White Balance & Sat", "8-Band HSL Mixer").forEachIndexed { idx, title ->
                val active = state.colorSubTab == idx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (active) StudioCard else Color.Transparent)
                        .clickable { onUpdateState(state.copy(colorSubTab = idx)) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = if (active) AmberBright else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (state.colorSubTab == 0) {
            PrecisionDial(
                label = "Temp (Kelvin)",
                value = state.temperature,
                spectrumColors = listOf(Color(0xFF3B82F6), Color(0xFFF3F4F6), Color(0xFFF59E0B)),
                onValueChange = { onUpdateState(state.copy(temperature = it)) }
            )
            PrecisionDial(
                label = "Vibrance",
                value = state.vibrance,
                onValueChange = { onUpdateState(state.copy(vibrance = it)) }
            )
        } else {
            // 8 Chromatic Color Orbs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                state.hslBands.forEachIndexed { index, band ->
                    val selected = state.selectedHslIndex == index
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(band.swatch.copy(alpha = if (selected) 1f else 0.45f))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onUpdateState(state.copy(selectedHslIndex = index)) }
                    ) {}
                }
            }

            val currentBand = state.hslBands[state.selectedHslIndex]
            PrecisionDial(
                label = "${currentBand.name} · Hue",
                value = currentBand.hue,
                onValueChange = { nextHue ->
                    val list = state.hslBands.toMutableList()
                    list[state.selectedHslIndex] = currentBand.copy(hue = nextHue)
                    onUpdateState(state.copy(hslBands = list))
                }
            )
            PrecisionDial(
                label = "${currentBand.name} · Saturation",
                value = currentBand.saturation,
                onValueChange = { nextSat ->
                    val list = state.hslBands.toMutableList()
                    list[state.selectedHslIndex] = currentBand.copy(saturation = nextSat)
                    onUpdateState(state.copy(hslBands = list))
                }
            )
        }
    }
}

@Composable
private fun OpticsEffectsPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    val opticsParams = listOf(
        "Clarity" to state.clarity,
        "Sharpen" to state.sharpen,
        "Dehaze" to state.dehaze,
        "Vignette" to state.vignette,
        "35mm Grain" to state.grain,
        "Matte Fade" to state.fade
    )
    val currentVal = opticsParams.find { it.first == state.selectedOpticsParam }?.second ?: 0f
    val range = if (state.selectedOpticsParam in listOf("Sharpen", "35mm Grain", "Matte Fade")) {
        0f..100f
    } else {
        -100f..100f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(opticsParams) { (name, value) ->
                val selected = state.selectedOpticsParam == name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AmberGold.copy(alpha = 0.16f) else StudioElevated)
                        .border(1.dp, if (selected) AmberGold else StudioBorder, RoundedCornerShape(10.dp))
                        .clickable { onUpdateState(state.copy(selectedOpticsParam = name)) }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "$name ${value.roundToInt()}",
                        color = if (selected) AmberBright else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        PrecisionDial(
            label = state.selectedOpticsParam,
            value = currentVal,
            valueRange = range,
            defaultValue = 0f,
            onValueChange = { next ->
                val updated = when (state.selectedOpticsParam) {
                    "Clarity" -> state.copy(clarity = next)
                    "Sharpen" -> state.copy(sharpen = next)
                    "Dehaze" -> state.copy(dehaze = next)
                    "Vignette" -> state.copy(vignette = next)
                    "35mm Grain" -> state.copy(grain = next)
                    else -> state.copy(fade = next)
                }
                onUpdateState(updated)
            }
        )
    }
}

@Composable
private fun AiNeuralLabPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiActionToggleChip(
                icon = Icons.Rounded.AutoAwesome,
                title = "AI Auto-Grade",
                active = state.aiAutoEnhance,
                modifier = Modifier.weight(1f),
                onClick = { onUpdateState(state.copy(aiAutoEnhance = !state.aiAutoEnhance)) }
            )
            AiActionToggleChip(
                icon = Icons.Rounded.WbSunny,
                title = "3D Relight",
                active = state.aiRelightEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onUpdateState(state.copy(aiRelightEnabled = !state.aiRelightEnabled)) }
            )
            AiActionToggleChip(
                icon = Icons.Rounded.GraphicEq,
                title = "4K Denoise",
                active = state.aiSuperRes,
                modifier = Modifier.weight(1f),
                onClick = { onUpdateState(state.copy(aiSuperRes = !state.aiSuperRes)) }
            )
        }

        PrecisionDial(
            label = "3D Key-Light Intensity (Drag Sun on Photo)",
            value = state.aiRelightIntensity,
            valueRange = 0f..100f,
            defaultValue = 46f,
            unitSuffix = "%",
            modifier = Modifier.padding(horizontal = 0.dp),
            onValueChange = { onUpdateState(state.copy(aiRelightIntensity = it)) }
        )
    }
}

@Composable
private fun AiActionToggleChip(
    icon: ImageVector,
    title: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) NeuralCyan.copy(alpha = 0.16f) else StudioElevated)
            .border(1.dp, if (active) NeuralCyan else StudioBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 6.dp)
    ) {
        Icon(
            imageVector = if (active) Icons.Rounded.CheckCircle else icon,
            contentDescription = title,
            tint = if (active) NeuralCyan else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            color = if (active) NeuralCyan else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CropTransformPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    val ratios = listOf("Original", "Free", "1:1", "4:5", "16:9", "9:16", "3:2")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ratios.forEach { ratio ->
                    val active = state.aspectRatio == ratio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AmberGold.copy(alpha = 0.18f) else StudioElevated)
                            .border(1.dp, if (active) AmberGold else StudioBorder, RoundedCornerShape(8.dp))
                            .clickable { onUpdateState(state.copy(aspectRatio = ratio)) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = ratio,
                            color = if (active) AmberBright else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TopBarIconButton(
                    icon = Icons.Rounded.Rotate90DegreesCcw,
                    active = state.rotationQuarter != 0,
                    onClick = {
                        onUpdateState(state.copy(rotationQuarter = (state.rotationQuarter + 1) % 4))
                    }
                )
                TopBarIconButton(
                    icon = Icons.Rounded.Flip,
                    active = state.flipHorizontal,
                    onClick = {
                        onUpdateState(state.copy(flipHorizontal = !state.flipHorizontal))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        PrecisionDial(
            label = "Straighten Horizon",
            value = state.straightenAngle,
            valueRange = -45f..45f,
            defaultValue = 0f,
            unitSuffix = "°",
            onValueChange = { onUpdateState(state.copy(straightenAngle = it)) }
        )
    }
}

@Composable
private fun NonDestructiveLayersPanel(
    state: EditorState,
    onUpdateState: (EditorState) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(state.layers) { layer ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(StudioElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (layer.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = "Toggle Layer",
                        tint = if (layer.visible) AmberBright else TextMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                val nextLayers = state.layers.map {
                                    if (it.id == layer.id) it.copy(visible = !it.visible) else it
                                }
                                val borderSync = if (layer.id == "frame") !layer.visible else state.exifBorder
                                onUpdateState(state.copy(layers = nextLayers, exifBorder = borderSync))
                            }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = layer.name,
                            color = if (layer.visible) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${layer.subtitle} · ${(layer.opacity * 100).roundToInt()}%",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCard)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = layer.blendMode.uppercase(),
                        color = NeuralCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
