package com.lumacraft.studio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FaceRetouchingNatural
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumacraft.studio.model.Adjustments
import kotlin.math.roundToInt
import com.lumacraft.studio.model.ALL_FILTERS
import com.lumacraft.studio.model.AVAILABLE_FONTS
import com.lumacraft.studio.model.CurveChannel
import com.lumacraft.studio.model.EMOJI_PALETTE
import com.lumacraft.studio.model.FilterDef
import com.lumacraft.studio.model.FILTER_CATEGORIES
import com.lumacraft.studio.model.FrameStyle
import com.lumacraft.studio.model.ExportFormat
import com.lumacraft.studio.model.TextLayer
import com.lumacraft.studio.ui.components.BalancePad
import com.lumacraft.studio.ui.components.BusyOverlay
import com.lumacraft.studio.ui.components.ChipRow
import com.lumacraft.studio.ui.components.CurveEditor
import com.lumacraft.studio.ui.components.HistogramView
import com.lumacraft.studio.ui.components.LumaSliderRow
import com.lumacraft.studio.ui.components.LumaSwitchRow
import com.lumacraft.studio.ui.components.LumaTextInputDialog
import com.lumacraft.studio.ui.components.SectionCard
import com.lumacraft.studio.vm.BrushTool
import com.lumacraft.studio.vm.StudioViewModel
import com.lumacraft.studio.vm.TapTool
import kotlinx.coroutines.launch

private enum class Module(val label: String, val icon: ImageVector) {
    ADJUST("Adjust", Icons.Rounded.Tune),
    COLOR("Color", Icons.Rounded.Palette),
    EFFECTS("Filters", Icons.Rounded.AutoFixHigh),
    PORTRAIT("Portrait", Icons.Rounded.FaceRetouchingNatural),
    RETOUCH("Retouch", Icons.Rounded.Healing),
    MASK("Mask", Icons.Rounded.Brush),
    AI("AI", Icons.Rounded.AutoAwesome),
    TEXT("Text", Icons.Rounded.TextFields),
    GEOMETRY("Geometry", Icons.Rounded.Crop)
}

private val DRAW_COLORS = listOf(0xFFFF5470, 0xFFFFD166, 0xFF7BD8C2, 0xFF5B7CFA, 0xFFFFFFFF, 0xFF101114)
private val BG_REPLACE_COLORS = listOf<Long?>(null, 0xFF101114, 0xFFFFFFFF, 0xFF5B7CFA, 0xFF7BD8C2, 0xFFF5B65A, 0xFFFF7A9E)

@Composable
fun EditorScreen(vm: StudioViewModel) {
    val st by vm.ui.collectAsState()
    BackHandler { vm.closeEditor() }

    var module by remember { mutableStateOf(Module.ADJUST) }
    var showExport by remember { mutableStateOf(false) }

    var zoom by remember { mutableStateOf(1f) }
    var ox by remember { mutableStateOf(0f) }
    var oy by remember { mutableStateOf(0f) }
    var selText by remember { mutableIntStateOf(-1) }
    var selEmoji by remember { mutableIntStateOf(-1) }

    val mode = when {
        st.brushTool != BrushTool.NONE -> "draw"
        st.tapTool != TapTool.NONE -> "tap"
        selText >= 0 || selEmoji >= 0 -> "move"
        else -> "view"
    }
    val modeState = rememberUpdatedState(mode)

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // ---------------- top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = { vm.closeEditor() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text(st.photoName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "${st.photoDims} · ${st.photoInfo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = { vm.undo() }, enabled = st.canUndo) {
                Icon(Icons.Rounded.Undo, "Undo", tint = if (st.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = { vm.redo() }, enabled = st.canRedo) {
                Icon(Icons.Rounded.Redo, "Redo", tint = if (st.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = { vm.copyLook() }, enabled = st.source != null) {
                Icon(Icons.Rounded.ContentCopy, "Copy look", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.pasteLook() }) {
                Icon(Icons.Rounded.ContentPaste, "Paste look", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showExport = true }) {
                Icon(Icons.Rounded.SaveAlt, "Export", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // ---------------- canvas
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val img: ImageBitmap? = if (st.compareHold) st.originalPreview ?: st.preview else st.preview
            if (img != null) {
                androidx.compose.foundation.Image(
                    bitmap = img,
                    contentDescription = "Edited preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = ox
                            translationY = oy
                        }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // live stroke overlay
            if (st.livePoints.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tool = st.brushTool
                    if (tool == BrushTool.DRAW) {
                        if (st.livePoints.size >= 2) {
                            val path = Path()
                            path.moveTo(st.livePoints[0].first * size.width, st.livePoints[0].second * size.height)
                            st.livePoints.drop(1).forEach { p ->
                                path.lineTo(p.first * size.width, p.second * size.height)
                            }
                            drawPath(
                                path,
                                Color(st.drawColor),
                                style = Stroke(width = (st.drawWidth * size.width).coerceAtLeast(4f), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    } else {
                        val last = st.livePoints.last()
                        val circleColor = when (st.brushTool) {
                            BrushTool.MASK_PAINT, BrushTool.MASK_ERASE -> Color(0xFFFF6EC7)
                            else -> Color.White
                        }
                        drawCircle(
                            circleColor,
                            radius = st.brushRadius * size.width,
                            center = Offset(last.first * size.width, last.second * size.height),
                            style = Stroke(2f)
                        )
                    }
                }
            }

            // gesture handling
            val modifier = Modifier
                .fillMaxSize()
                .pointerInput(mode) {
                    when (mode) {
                        "tap" -> detectTapGestures { off ->
                            vm.onCanvasTap(off.x / size.width, off.y / size.height)
                        }
                        "draw" -> detectDragGestures(
                            onDragStart = { off -> vm.onStrokeStart(off.x / size.width, off.y / size.height) },
                            onDrag = { change, _ ->
                                change.consume()
                                vm.onStrokeMove(change.position.x / size.width, change.position.y / size.height)
                            },
                            onDragEnd = { vm.onStrokeEnd() },
                            onDragCancel = { vm.onStrokeEnd() }
                        )
                        "move" -> detectDragGestures { change, _ ->
                            change.consume()
                            val dx = (change.position.x - change.previousPosition.x) / size.width
                            val dy = (change.position.y - change.previousPosition.y) / size.height
                            val tIdx = selText
                            val eIdx = selEmoji
                            if (tIdx >= 0) {
                                vm.edit(historyKey = "moveText") { s ->
                                    val layers = s.textLayers.toMutableList()
                                    if (tIdx < layers.size) {
                                        val l = layers[tIdx]
                                        layers[tIdx] = l.copy(
                                            x = (l.x + dx).coerceIn(0.02f, 0.98f),
                                            y = (l.y + dy).coerceIn(0.04f, 0.98f)
                                        )
                                    }
                                    s.copy(textLayers = layers)
                                }
                            } else if (eIdx >= 0) {
                                vm.edit(historyKey = "moveEmoji") { s ->
                                    val layers = s.emojiLayers.toMutableList()
                                    if (eIdx < layers.size) {
                                        val l = layers[eIdx]
                                        layers[eIdx] = l.copy(
                                            x = (l.x + dx).coerceIn(0.02f, 0.98f),
                                            y = (l.y + dy).coerceIn(0.04f, 0.98f)
                                        )
                                    }
                                    s.copy(emojiLayers = layers)
                                }
                            }
                        }
                        else -> detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                            zoom = newZoom
                            if (newZoom > 1f) {
                                ox = (ox + pan.x).coerceIn(-size.width.toFloat(), size.width.toFloat())
                                oy = (oy + pan.y).coerceIn(-size.height.toFloat(), size.height.toFloat())
                            } else {
                                ox = 0f
                                oy = 0f
                            }
                        }
                    }
                }
                .pointerInput(mode, "cmp") {
                    detectTapGestures(
                        onPress = {
                            if (modeState.value == "view") vm.setCompareHold(true)
                            tryAwaitRelease()
                            vm.setCompareHold(false)
                        },
                        onDoubleTap = {
                            if (modeState.value == "view") {
                                zoom = 1f; ox = 0f; oy = 0f
                            }
                        }
                    )
                }
            Box(modifier = modifier)

            // histogram + clipping indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            ) {
                HistogramView(
                    bins = st.hist,
                    clipShadow = st.clipShadow,
                    clipHighlight = st.clipHighlight,
                    modifier = Modifier.size(width = 130.dp, height = 46.dp).background(Color.Black.copy(alpha = 0.45f))
                )
            }
            if (st.compareHold) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("ORIGINAL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            val hint = when {
                st.tapTool == TapTool.HEAL -> "Tap a blemish to heal it"
                st.tapTool == TapTool.RED_EYE -> "Tap the red eye to fix it"
                st.tapTool == TapTool.TEETH -> "Tap teeth area to whiten"
                st.brushTool == BrushTool.MASK_PAINT -> "Paint the mask, then adjust it below"
                st.brushTool == BrushTool.MASK_ERASE -> "Paint to erase parts of the mask"
                st.brushTool == BrushTool.BLUR -> "Paint to blur (selective blur brush)"
                st.brushTool == BrushTool.PIXELATE -> "Paint to pixelate"
                st.brushTool == BrushTool.ERASE -> "Paint over an object to erase it"
                st.brushTool == BrushTool.DRAW -> "Draw on the photo"
                selText >= 0 -> "Dragging moves the selected text"
                selEmoji >= 0 -> "Dragging moves the selected sticker"
                else -> null
            }
            if (hint != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(hint, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (st.busy != null) BusyOverlay(st.busy!!)
        }

        // ---------------- module panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (module) {
                Module.ADJUST -> AdjustPanel(st, vm)
                Module.COLOR -> ColorPanel(st, vm)
                Module.EFFECTS -> EffectsPanel(st, vm)
                Module.PORTRAIT -> PortraitPanel(st, vm)
                Module.RETOUCH -> RetouchPanel(st, vm)
                Module.MASK -> MaskPanel(st, vm)
                Module.AI -> AiPanel(st, vm)
                Module.TEXT -> TextPanel(st, vm, onSelText = { selText = it }, selText = selText, onSelEmoji = { selEmoji = it }, selEmoji = selEmoji)
                Module.GEOMETRY -> GeometryPanel(st, vm)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------------- module tabs
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(vertical = 8.dp)
        ) {
            Module.entries.forEach { m ->
                val sel = module == m
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { module = m }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        m.icon, m.label,
                        tint = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        m.label,
                        fontSize = 9.sp,
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showExport) {
        ExportDialog(st, vm, onDismiss = { showExport = false })
    }
}

// ================================================================ panels

@Composable
private fun AdjSlider(
    vm: StudioViewModel,
    a: Adjustments,
    label: String,
    get: (Adjustments) -> Float,
    set: (Adjustments, Float) -> Adjustments,
    range: ClosedFloatingPointRange<Float> = -100f..100f
) {
    LumaSliderRow(
        label = label,
        value = get(a),
        defaultValue = 0f,
        range = range,
        onChange = { v -> vm.edit(historyKey = "adj") { s -> s.copy(adjust = set(s.adjust, v)) } },
        onReset = { vm.edit { s -> s.copy(adjust = set(s.adjust, 0f)) } }
    )
}

@Composable
private fun AdjustPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    val a = st.state.adjust
    SectionCard("Light") {
        AdjSlider(vm, a, "Exposure", { it.exposure }, { adj, v -> adj.copy(exposure = v) }, -100f..100f)
        AdjSlider(vm, a, "Brightness", { it.brightness }, { adj, v -> adj.copy(brightness = v) })
        AdjSlider(vm, a, "Contrast", { it.contrast }, { adj, v -> adj.copy(contrast = v) })
        AdjSlider(vm, a, "Highlights", { it.highlights }, { adj, v -> adj.copy(highlights = v) })
        AdjSlider(vm, a, "Shadows", { it.shadows }, { adj, v -> adj.copy(shadows = v) })
        AdjSlider(vm, a, "Whites", { it.whites }, { adj, v -> adj.copy(whites = v) })
        AdjSlider(vm, a, "Blacks", { it.blacks }, { adj, v -> adj.copy(blacks = v) })
    }
    SectionCard("Color") {
        AdjSlider(vm, a, "Saturation", { it.saturation }, { adj, v -> adj.copy(saturation = v) })
        AdjSlider(vm, a, "Vibrance", { it.vibrance }, { adj, v -> adj.copy(vibrance = v) })
        AdjSlider(vm, a, "Temperature", { it.temperature }, { adj, v -> adj.copy(temperature = v) })
        AdjSlider(vm, a, "Tint", { it.tint }, { adj, v -> adj.copy(tint = v) })
    }
    SectionCard("Detail") {
        AdjSlider(vm, a, "Sharpness", { it.sharpness }, { adj, v -> adj.copy(sharpness = v) }, 0f..100f)
        AdjSlider(vm, a, "Clarity", { it.clarity }, { adj, v -> adj.copy(clarity = v) })
        AdjSlider(vm, a, "Texture", { it.texture }, { adj, v -> adj.copy(texture = v) })
        AdjSlider(vm, a, "Dehaze", { it.dehaze }, { adj, v -> adj.copy(dehaze = v) }, 0f..100f)
        AdjSlider(vm, a, "Noise Reduction", { it.noiseReduction }, { adj, v -> adj.copy(noiseReduction = v) }, 0f..100f)
    }
    SectionCard("Effects") {
        AdjSlider(vm, a, "Fade", { it.fade }, { adj, v -> adj.copy(fade = v) }, 0f..100f)
        AdjSlider(vm, a, "Vignette", { it.vignette }, { adj, v -> adj.copy(vignette = v) }, 0f..100f)
        AdjSlider(vm, a, "Grain", { it.grain }, { adj, v -> adj.copy(grain = v) }, 0f..100f)
    }
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Button(onClick = { vm.runAiTool("enhance") }) { Text("AI Auto Enhance") }
        Spacer(modifier = Modifier.width(10.dp))
        OutlinedButton(onClick = { vm.resetAdjust() }) { Text("Reset sliders") }
    }
}

@Composable
private fun ColorPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    val s = st.state
    var curveChannel by remember { mutableStateOf(CurveChannel.MASTER) }
    var hslZone by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val lutSaveScope = androidx.compose.runtime.rememberCoroutineScope()

    val lutPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            if (text != null) vm.importLut(uri.lastPathSegment ?: "lut.cube", text)
        }
    }
    val lutSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            lutSaveScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val content = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { vm.exportLutContent() }
                if (content != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os -> os.write(content.toByteArray()) }
                }
            }
        }
    }

    SectionCard("White Balance") {
        AdjSlider(vm, s.adjust, "Temperature", { it.temperature }, { adj, v -> adj.copy(temperature = v) })
        AdjSlider(vm, s.adjust, "Tint", { it.tint }, { adj, v -> adj.copy(tint = v) })
        AdjSlider(vm, s.adjust, "Hue", { it.hueShift }, { adj, v -> adj.copy(hueShift = v) }, -180f..180f)
    }
    SectionCard("Tone Curves") {
        ChipRow(
            options = CurveChannel.entries,
            selected = curveChannel,
            label = { it.label },
            onSelect = { curveChannel = it }
        )
        CurveEditor(
            points = s.curves[curveChannel] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            strokeColor = curveChannel.stroke,
            onChange = { pts ->
                vm.edit(historyKey = "curve") { st2 -> st2.copy(curves = st2.curves + (curveChannel to pts)) }
            },
            modifier = Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    SectionCard("HSL Color Mixer") {
        ChipRow(
            options = s.hsl.indices.toList(),
            selected = hslZone,
            label = { s.hsl[it].label },
            onSelect = { hslZone = it }
        )
        val z = s.hsl[hslZone]
        LumaSliderRow("Hue", z.hue, 0f, -100f..100f, onChange = { v -> vm.edit("hsl") { st2 -> st2.copy(hsl = st2.hsl.mapIndexed { i, zz -> if (i == hslZone) zz.copy(hue = v) else zz }) } }, onReset = { vm.edit { st2 -> st2.copy(hsl = st2.hsl.mapIndexed { i, zz -> if (i == hslZone) zz.copy(hue = 0f) else zz }) } })
        LumaSliderRow("Saturation", z.sat, 0f, -100f..100f, onChange = { v -> vm.edit("hsl") { st2 -> st2.copy(hsl = st2.hsl.mapIndexed { i, zz -> if (i == hslZone) zz.copy(sat = v) else zz }) } }, onReset = { vm.edit { st2 -> st2.copy(hsl = st2.hsl.mapIndexed { i, zz -> if (i == hslZone) zz.copy(sat = 0f) else zz }) } })
        LumaSliderRow("Luminance", z.lum, 0f, -100f..100f, onChange = { v -> vm.edit("hsl") { st2 -> st2.copy(hsl = st2.hsl.mapIndexed { i, zz -> if (i == hslZone) zz.copy(lum = v) else zz }) } }, onReset = { vm.edit { st2 -> st2.copy(hsl = st2.hsl.mapIndexed { i, zz -> if (i == hslZone) zz.copy(lum = 0f) else zz }) } })
    }
    SectionCard("Split Toning") {
        LumaSliderRow("Shadow hue", s.split.shadowHue, 0f, 0f..360f, onChange = { v -> vm.edit("split") { st2 -> st2.copy(split = st2.split.copy(shadowHue = v)) } })
        LumaSliderRow("Shadow strength", s.split.shadowSat, 0f, 0f..100f, onChange = { v -> vm.edit("split") { st2 -> st2.copy(split = st2.split.copy(shadowSat = v)) } })
        LumaSliderRow("Highlight hue", s.split.highlightHue, 0f, 0f..360f, onChange = { v -> vm.edit("split") { st2 -> st2.copy(split = st2.split.copy(highlightHue = v)) } })
        LumaSliderRow("Highlight strength", s.split.highlightSat, 0f, 0f..100f, onChange = { v -> vm.edit("split") { st2 -> st2.copy(split = st2.split.copy(highlightSat = v)) } })
        LumaSliderRow("Balance", s.split.balance, 0f, -100f..100f, onChange = { v -> vm.edit("split") { st2 -> st2.copy(split = st2.split.copy(balance = v)) } })
    }
    SectionCard("3-Way Color Balance") {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            BalancePad("Shadows", s.shadowWheel.x, s.shadowWheel.y, onChange = { x, y -> vm.edit("wheelSh") { st2 -> st2.copy(shadowWheel = com.lumacraft.studio.model.WheelGrade(x, y)) } })
            BalancePad("Midtones", s.midWheel.x, s.midWheel.y, onChange = { x, y -> vm.edit("wheelMid") { st2 -> st2.copy(midWheel = com.lumacraft.studio.model.WheelGrade(x, y)) } })
            BalancePad("Highlights", s.highWheel.x, s.highWheel.y, onChange = { x, y -> vm.edit("wheelHi") { st2 -> st2.copy(highWheel = com.lumacraft.studio.model.WheelGrade(x, y)) } })
        }
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Button(onClick = { vm.autoWhiteBalance() }) { Text("Auto White Balance") }
        }
    }
    SectionCard(".cube LUT — Import / Export") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { lutPick.launch(arrayOf("*/*")) }) { Text("Import .cube") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { lutSave.launch(vm.exportLutSuggestedName()) },
                enabled = st.source != null
            ) { Text("Export grade → .cube") }
        }
        if (s.lutName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Active LUT: ${s.lutName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LumaSliderRow(
                "LUT intensity",
                s.lutIntensity,
                100f,
                0f..100f,
                onChange = { v -> vm.edit("lutK") { st2 -> st2.copy(lutIntensity = v) } },
                onReset = { vm.edit { st2 -> st2.copy(lutIntensity = 100f) } }
            )
        }
        Text(
            "3D trilinear engine (up to 33³). Export generates a real .cube from your current grade — usable in Lightroom & video editors.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EffectsPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    var category by remember { mutableStateOf(FILTER_CATEGORIES.first()) }
    SectionCard("Filters — ${ALL_FILTERS.size} original looks") {
        ChipRow(options = FILTER_CATEGORIES, selected = category, label = { it }, onSelect = { category = it })
        Spacer(modifier = Modifier.height(10.dp))
        val inCat = ALL_FILTERS.filter { it.category == category }
        inCat.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                pair.forEach { f ->
                    FilterCard(
                        f = f,
                        thumb = st.filterThumbs[f.id],
                        selected = st.state.filterId == f.id,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.applyFilter(f) }
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        LumaSliderRow(
            "Filter intensity",
            st.state.filterIntensity,
            100f,
            0f..100f,
            onChange = { vm.setFilterIntensity(it) },
            onReset = { vm.setFilterIntensity(100f) }
        )
        TextButton(onClick = { vm.edit { s -> s.copy(filterId = null, adjust = Adjustments()) } }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Clear filter & adjustments")
        }
    }
    SectionCard("Frame") {
        ChipRow(options = FrameStyle.entries, selected = st.state.frame, label = { it.label }, onSelect = { vm.edit { s -> s.copy(frame = it) } })
    }
    SectionCard("Watermark") {
        androidx.compose.material3.OutlinedTextField(
            value = st.state.watermark,
            onValueChange = { vm.edit(historyKey = "wm") { s -> s.copy(watermark = it) } },
            placeholder = { Text("@yourname — bottom right") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FilterCard(f: FilterDef, thumb: ImageBitmap?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                2.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(10.dp))) {
            if (thumb != null) {
                androidx.compose.foundation.Image(bitmap = thumb, contentDescription = f.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(f.accent.copy(alpha = 0.4f)))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(f.name, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun PortraitPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    SectionCard("Natural Portrait Enhancement") {
        LumaSliderRow("Skin smoothing", st.state.skinSmooth, 0f, 0f..100f, onChange = { v -> vm.edit("skin") { s -> s.copy(skinSmooth = v) } }, onReset = { vm.edit { s -> s.copy(skinSmooth = 0f) } })
        LumaSliderRow("Natural enhance", st.state.naturalEnhance, 0f, 0f..100f, onChange = { v -> vm.edit("nat") { s -> s.copy(naturalEnhance = v) } }, onReset = { vm.edit { s -> s.copy(naturalEnhance = 0f) } })
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Button(onClick = { vm.runAiTool("portrait") }) { Text("AI Portrait Boost") }
        }
        Text(
            "Skin smoothing uses an edge-aware skin-tone blend — keeps pores & hair sharp, softens only skin. Blemishes: use the Retouch tab's Heal tool.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    SectionCard("Quick Fixes") {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(onClick = { vm.setTapTool(TapTool.RED_EYE) }) { Text("Red-Eye Fix") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { vm.setTapTool(TapTool.TEETH) }) { Text("Teeth Whitening") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { vm.setTapTool(TapTool.NONE) }) { Text("Done") }
        }
    }
}

@Composable
private fun RetouchPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    SectionCard("Tap Tools") {
        ChipRow(
            options = listOf(TapTool.NONE, TapTool.HEAL, TapTool.RED_EYE, TapTool.TEETH),
            selected = st.tapTool,
            label = { when (it) { TapTool.NONE -> "Off"; TapTool.HEAL -> "Spot Heal"; TapTool.RED_EYE -> "Red-Eye"; TapTool.TEETH -> "Teeth" } },
            onSelect = { vm.setTapTool(it) }
        )
        LumaSliderRow("Tool size", st.brushRadius * 100f, 5f, 2f..15f, onChange = { vm.setBrushRadius(it / 100f) })
    }
    SectionCard("Brushes") {
        ChipRow(
            options = listOf(BrushTool.NONE, BrushTool.BLUR, BrushTool.PIXELATE, BrushTool.ERASE, BrushTool.DRAW),
            selected = st.brushTool,
            label = { when (it) { BrushTool.NONE -> "Off"; BrushTool.BLUR -> "Blur Brush"; BrushTool.PIXELATE -> "Pixelate"; BrushTool.ERASE -> "Object Eraser"; BrushTool.DRAW -> "Draw" } },
            onSelect = { vm.setBrushTool(it) }
        )
        if (st.brushTool == BrushTool.DRAW) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                DRAW_COLORS.forEach { c ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(2.dp, if (st.drawColor == c) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            .clickable { vm.setDrawColor(c) }
                    )
                }
            }
            LumaSliderRow("Brush width", st.drawWidth * 1000f, 8f, 3f..30f, onChange = { vm.setDrawWidth(it / 1000f) })
        }
        TextButton(onClick = { vm.clearHealSpots() }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Clear all retouch strokes & spots")
        }
    }
}

@Composable
private fun MaskPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    SectionCard("Painted Selective Mask") {
        ChipRow(
            options = listOf(BrushTool.NONE, BrushTool.MASK_PAINT, BrushTool.MASK_ERASE),
            selected = st.brushTool,
            label = { when (it) { BrushTool.MASK_PAINT -> "Paint Mask"; BrushTool.MASK_ERASE -> "Erase Mask"; else -> "Off" } },
            onSelect = { vm.setBrushTool(it) }
        )
        LumaSliderRow("Brush size", st.brushRadius * 100f, 5f, 2f..15f, onChange = { v -> vm.setBrushRadius(v / 100f) })
        Text(
            "Paint over the photo, then use the sliders below — adjustments apply only inside your painted mask. Fully non-destructive.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    SectionCard("Mask Adjustments") {
        LumaSliderRow("Mask exposure", st.state.maskExposure, 0f, -100f..100f, onChange = { v -> vm.edit("mExp") { s -> s.copy(maskExposure = v) } }, onReset = { vm.edit { s -> s.copy(maskExposure = 0f) } })
        LumaSliderRow("Mask warmth", st.state.maskTemp, 0f, -100f..100f, onChange = { v -> vm.edit("mTem") { s -> s.copy(maskTemp = v) } }, onReset = { vm.edit { s -> s.copy(maskTemp = 0f) } })
        LumaSliderRow("Mask saturation", st.state.maskSat, 0f, -100f..100f, onChange = { v -> vm.edit("mSat") { s -> s.copy(maskSat = v) } }, onReset = { vm.edit { s -> s.copy(maskSat = 0f) } })
        LumaSliderRow("Mask clarity", st.state.maskClarity, 0f, -100f..100f, onChange = { v -> vm.edit("mCla") { s -> s.copy(maskClarity = v) } }, onReset = { vm.edit { s -> s.copy(maskClarity = 0f) } })
        LumaSwitchRow("Show mask overlay", "Highlight the painted area", st.state.showMaskOverlay) { v -> vm.edit { s -> s.copy(showMaskOverlay = v) } }
        TextButton(onClick = { vm.clearMask() }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Clear mask")
        }
    }
}

@Composable
private fun AiPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    val ai = st.state.ai
    SectionCard("On-Device AI (offline ML Kit)") {
        LumaSliderRow("Background blur (portrait lens)", ai.bgBlur, 0f, 0f..100f, onChange = { v -> vm.edit("bgBlur") { s -> s.copy(ai = s.ai.copy(bgBlur = v)) } }, onReset = { vm.edit { s -> s.copy(ai = s.ai.copy(bgBlur = 0f)) } })
        LumaSliderRow("Subject pop", ai.subjectPop, 0f, -50f..50f, onChange = { v -> vm.edit("pop") { s -> s.copy(ai = s.ai.copy(subjectPop = v)) } }, onReset = { vm.edit { s -> s.copy(ai = s.ai.copy(subjectPop = 0f)) } })
        LumaSliderRow("Background dim", ai.bgDim, 0f, 0f..100f, onChange = { v -> vm.edit("bgDim") { s -> s.copy(ai = s.ai.copy(bgDim = v)) } }, onReset = { vm.edit { s -> s.copy(ai = s.ai.copy(bgDim = 0f)) } })
    }
    SectionCard("Background") {
        Text("Replace with color:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            BG_REPLACE_COLORS.forEach { c ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (c == null) MaterialTheme.colorScheme.outline else Color(c!!))
                        .border(2.dp, if (ai.bgColor == c) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                        .clickable { vm.edit { s -> s.copy(ai = s.ai.copy(bgColor = c)) } }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (ai.bgColor == null) "keep original" else "solid fill", style = MaterialTheme.typography.labelSmall)
        }
        LumaSwitchRow(
            "Transparent PNG cutout",
            "Remove background entirely — exports as PNG with alpha",
            ai.cutout
        ) { vm.edit { s -> s.copy(ai = s.ai.copy(cutout = it)) } }
        LumaSwitchRow(
            "Show subject mask",
            "Visualize the AI-selected subject",
            ai.showMask
        ) { vm.edit { s -> s.copy(ai = s.ai.copy(showMask = it)) } }
        if (ai.showMask) {
            LumaSwitchRow("Invert selection", "Select the background instead", ai.maskInvert) { vm.edit { s -> s.copy(ai = s.ai.copy(maskInvert = it)) } }
        }
    }
    SectionCard("One-Tap AI") {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { vm.runAiTool("enhance") }) { Text("Enhance") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { vm.runAiTool("restore") }) { Text("Restore") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { vm.runAiTool("oldphoto") }) { Text("Old Photo") }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LumaSwitchRow(
            "2× AI upscale on export",
            "Bicubic 2× + smart sharpening (software upscale)",
            st.exportOptions.upscale2x
        ) { vm.runAiTool("upscale") }
    }
    SectionCard("Not available offline") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Colorization", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Requires a cloud AI service. LumaCraft is privacy-first and ships no cloud calls — this tool is intentionally disabled rather than faked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TextPanel(
    st: com.lumacraft.studio.vm.StudioUiState,
    vm: StudioViewModel,
    selText: Int,
    onSelText: (Int) -> Unit,
    selEmoji: Int,
    onSelEmoji: (Int) -> Unit
) {
    var addText by remember { mutableStateOf(false) }
    var editTextIdx by remember { mutableIntStateOf(-1) }

    SectionCard("Text Overlays") {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { addText = true }) { Icon(Icons.Rounded.AddAPhoto, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text("Add Text") }
        }
        st.state.textLayers.forEachIndexed { i, t ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selText == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelText(if (selText == i) -1 else i) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("\"${t.text}\"", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                TextButton(onClick = { editTextIdx = i }) { Text("Edit") }
                TextButton(onClick = { vm.edit { s -> s.copy(textLayers = s.textLayers.filterIndexed { idx, _ -> idx != i }) }; onSelText(-1) }) { Text("Del") }
            }
        }
        if (selText >= 0 && selText < st.state.textLayers.size) {
            val t = st.state.textLayers[selText]
            ChipRow(options = AVAILABLE_FONTS.indices.toList(), selected = t.fontIdx, label = { AVAILABLE_FONTS[it].first }, onSelect = { fi -> vm.edit("font") { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(fontIdx = fi); s.copy(textLayers = l) } })
            LumaSliderRow("Size", t.sizeRel * 100f, 7f, 2f..20f, onChange = { v -> vm.edit("tsz") { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(sizeRel = v / 100f); s.copy(textLayers = l) } })
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                DRAW_COLORS.forEach { c ->
                    Box(modifier = Modifier.padding(4.dp).size(24.dp).clip(CircleShape).background(Color(c)).border(2.dp, if (t.color == (c.toLong() or 0xFF000000)) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape).clickable { vm.edit { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(color = c.toLong() or 0xFF000000); s.copy(textLayers = l) } })
                }
            }
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Bold"); Switch(checked = t.bold, onCheckedChange = { vm.edit { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(bold = it); s.copy(textLayers = l) } }, modifier = Modifier.padding(horizontal = 6.dp))
                Text("Italic"); Switch(checked = t.italic, onCheckedChange = { vm.edit { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(italic = it); s.copy(textLayers = l) } }, modifier = Modifier.padding(horizontal = 6.dp))
                Text("Center"); Switch(checked = t.centered, onCheckedChange = { vm.edit { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(centered = it); s.copy(textLayers = l) } }, modifier = Modifier.padding(horizontal = 6.dp))
            }
            LumaSliderRow("Opacity", t.opacity * 100f, 100f, 20f..100f, onChange = { v -> vm.edit("top") { s -> val l = s.textLayers.toMutableList(); l[selText] = l[selText].copy(opacity = v / 100f); s.copy(textLayers = l) } })
        }
    }
    SectionCard("Emoji Stickers") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            items(EMOJI_PALETTE.size) { i ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { vm.edit { s -> s.copy(emojiLayers = s.emojiLayers + com.lumacraft.studio.model.EmojiLayer(EMOJI_PALETTE[i])) }; onSelEmoji(st.state.emojiLayers.size) }
                ) {
                    Text(EMOJI_PALETTE[i], fontSize = 20.sp)
                }
            }
        }
        st.state.emojiLayers.forEachIndexed { i, e ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selEmoji == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelEmoji(if (selEmoji == i) -1 else i) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${e.emoji} sticker", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.edit { s -> s.copy(emojiLayers = s.emojiLayers.filterIndexed { idx, _ -> idx != i }) }; onSelEmoji(-1) }) { Text("Del") }
            }
        }
        Text(
            "Select a layer, then drag on the photo to position it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    if (addText) {
        LumaTextInputDialog("New text layer", "", onDismiss = { addText = false }, onConfirm = { txt ->
            if (txt.isNotBlank()) vm.edit { s -> s.copy(textLayers = s.textLayers + TextLayer(text = txt)) }
            addText = false
        })
    }
    if (editTextIdx >= 0 && editTextIdx < st.state.textLayers.size) {
        val current = st.state.textLayers[editTextIdx]
        LumaTextInputDialog("Edit text", current.text, onDismiss = { editTextIdx = -1 }, onConfirm = { txt ->
            vm.edit { s -> val l = s.textLayers.toMutableList(); l[editTextIdx] = l[editTextIdx].copy(text = txt); s.copy(textLayers = l) }
            editTextIdx = -1
        })
    }
}

@Composable
private fun GeometryPanel(st: com.lumacraft.studio.vm.StudioUiState, vm: StudioViewModel) {
    val g = st.state.geo
    SectionCard("Aspect Ratio") {
        ChipRow(
            options = listOf("Original", "1:1", "4:5", "3:4", "2:3", "4:3", "3:2", "16:9", "9:16"),
            selected = g.aspect,
            label = { it },
            onSelect = { vm.edit { s -> s.copy(geo = s.geo.copy(aspect = it)) } }
        )
    }
    SectionCard("Rotate & Flip") {
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.edit { s -> s.copy(geo = s.geo.copy(quarterTurns = (s.geo.quarterTurns + 3) % 4)) } }) {
                Icon(Icons.Rounded.Rotate90DegreesCw, null, modifier = Modifier.size(16.dp)); Text(" ⟲ 90°")
            }
            OutlinedButton(onClick = { vm.edit { s -> s.copy(geo = s.geo.copy(quarterTurns = (s.geo.quarterTurns + 1) % 4)) } }) {
                Icon(Icons.Rounded.Rotate90DegreesCw, null, modifier = Modifier.size(16.dp)); Text(" ⟳ 90°")
            }
            OutlinedButton(onClick = { vm.edit { s -> s.copy(geo = s.geo.copy(flipH = !s.geo.flipH)) } }) {
                Icon(Icons.Rounded.Flip, null, modifier = Modifier.size(16.dp)); Text(" Flip H")
            }
            OutlinedButton(onClick = { vm.edit { s -> s.copy(geo = s.geo.copy(flipV = !s.geo.flipV)) } }) {
                Icon(Icons.Rounded.Flip, null, modifier = Modifier.size(16.dp)); Text(" Flip V")
            }
        }
    }
    SectionCard("Straighten & Perspective") {
        LumaSliderRow("Straighten", g.straighten, 0f, -45f..45f, onChange = { v -> vm.edit("str") { s -> s.copy(geo = s.geo.copy(straighten = v)) } }, onReset = { vm.edit { s -> s.copy(geo = s.geo.copy(straighten = 0f)) } })
        LumaSliderRow("Keystone vertical", g.keystoneV, 0f, -100f..100f, onChange = { v -> vm.edit("kv") { s -> s.copy(geo = s.geo.copy(keystoneV = v)) } }, onReset = { vm.edit { s -> s.copy(geo = s.geo.copy(keystoneV = 0f)) } })
        LumaSliderRow("Keystone horizontal", g.keystoneH, 0f, -100f..100f, onChange = { v -> vm.edit("kh") { s -> s.copy(geo = s.geo.copy(keystoneH = v)) } }, onReset = { vm.edit { s -> s.copy(geo = s.geo.copy(keystoneH = 0f)) } })
        TextButton(onClick = { vm.edit { s -> s.copy(geo = com.lumacraft.studio.model.GeoState()) } }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Reset geometry")
        }
    }
}

// ================================================================ export dialog

@Composable
private fun ExportDialog(
    st: com.lumacraft.studio.vm.StudioUiState,
    vm: StudioViewModel,
    onDismiss: () -> Unit
) {
    val opts = st.exportOptions
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Photo") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                ChipRow(options = ExportFormat.entries, selected = opts.format, label = { it.label }, onSelect = { vm.setExportOptions { o -> o.copy(format = it) } })
                Spacer(modifier = Modifier.height(10.dp))
                Text("Resolution", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                ChipRow(
                    options = listOf("Original", "2048 px", "1080 px", "Square 1080"),
                    selected = opts.resolutionMode,
                    label = { it },
                    onSelect = { vm.setExportOptions { o -> o.copy(resolutionMode = it) } }
                )
                Spacer(modifier = Modifier.height(6.dp))
                LumaSliderRow("Quality", opts.quality.toFloat(), 92f, 1f..100f, onChange = { vm.setExportOptions { o -> o.copy(quality = it.roundToInt()) } })
                Spacer(modifier = Modifier.height(6.dp))
                LumaSwitchRow("Strip EXIF metadata", "Privacy: removes location & camera info", opts.stripExif) { vm.setExportOptions { o -> o.copy(stripExif = it) } }
                LumaSwitchRow("2× upscale", "Bicubic 2× + smart sharpening", opts.upscale2x) { vm.setExportOptions { o -> o.copy(upscale2x = it) } }
                LumaSwitchRow("Share after save", "Open the share sheet when done", opts.shareAfter) { vm.setExportOptions { o -> o.copy(shareAfter = it) } }
                if (st.state.ai.cutout) {
                    Text(
                        "Cutout mode active — export will be PNG with transparency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { vm.exportPhoto(); onDismiss() }) { Text("Export") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
