package studio.velox.atelier.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CameraRoll
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.CropOriginal
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.LensBlur
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.velox.atelier.engine.BitmapGradeEngine
import studio.velox.atelier.model.CurveChannelId
import studio.velox.atelier.model.DarkroomModule
import studio.velox.atelier.model.DarkroomState
import studio.velox.atelier.model.ExportFormat
import studio.velox.atelier.model.FILM_STOCK_RECIPES
import studio.velox.atelier.model.MaskType
import studio.velox.atelier.model.PrintBorderStyle
import studio.velox.atelier.model.RgbScopeData
import studio.velox.atelier.model.WheelGrade
import studio.velox.atelier.ui.components.ColorGradingWheels
import studio.velox.atelier.ui.components.KnurledRotaryDial
import studio.velox.atelier.ui.components.ProSplineCurve
import studio.velox.atelier.ui.components.RgbParadeScope
import studio.velox.atelier.ui.theme.ChalkWhite
import studio.velox.atelier.ui.theme.ChampagneBrass
import studio.velox.atelier.ui.theme.CineMint
import studio.velox.atelier.ui.theme.CobaltMask
import studio.velox.atelier.ui.theme.GraphiteIsland
import studio.velox.atelier.ui.theme.KnurledCapsule
import studio.velox.atelier.ui.theme.LeicaVermilion
import studio.velox.atelier.ui.theme.MachinedBevel
import studio.velox.atelier.ui.theme.ObsidianStage
import studio.velox.atelier.ui.theme.PewterMuted
import studio.velox.atelier.ui.theme.SilverLabel
import studio.velox.atelier.ui.theme.TitaniumChassis
import studio.velox.atelier.ui.theme.VermilionSoft
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkroomEditorScreen(
    state: DarkroomState,
    sourceBitmap: Bitmap?,
    canUndo: Boolean,
    canRedo: Boolean,
    stepNumber: Int,
    onUpdateState: (DarkroomState) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var gradedImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var rawImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var liveScope by remember { mutableStateOf(RgbScopeData()) }

    var showExportModal by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Pinch-to-zoom state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextZoom = (zoomScale * zoomChange).coerceIn(1f, 4f)
        zoomScale = nextZoom
        panOffset = if (nextZoom <= 1.02f) Offset.Zero else panOffset + panChange
    }

    // Off-main-thread real pixel pipeline execution
    LaunchedEffect(sourceBitmap, state) {
        val base = sourceBitmap ?: return@LaunchedEffect
        val result = withContext(Dispatchers.Default) {
            BitmapGradeEngine.processPreview(base, state, targetMaxDim = 680)
        }
        gradedImage = result.gradedBitmap.asImageBitmap()
        rawImage = result.rawAlignedBitmap.asImageBitmap()
        liveScope = result.scope
    }

    var stageW by remember { mutableIntStateOf(1) }
    var stageH by remember { mutableIntStateOf(1) }
    val latestState by rememberUpdatedState(state)
    val latestOnUpdate by rememberUpdatedState(onUpdateState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianStage)
            .statusBarsPadding()
    ) {
        // 1. Floating Machined Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                HudCapsuleIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    active = false,
                    onClick = onBack
                )
                HudCapsuleIcon(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    active = false,
                    enabled = canUndo,
                    onClick = onUndo
                )
                HudCapsuleIcon(
                    icon = Icons.AutoMirrored.Rounded.Redo,
                    active = false,
                    enabled = canRedo,
                    onClick = onRedo
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GraphiteIsland)
                        .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                        .clickable {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (zoomScale > 1.05f) "${(zoomScale * 100).roundToInt()}%" else "STEP 0$stepNumber".takeLast(7),
                        color = ChampagneBrass,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HudCapsuleIcon(
                    icon = Icons.Rounded.BarChart,
                    active = state.showScope,
                    onClick = { onUpdateState(state.copy(showScope = !state.showScope)) }
                )
                HudCapsuleIcon(
                    icon = Icons.Rounded.Compare,
                    active = state.showSplitWipe,
                    onClick = { onUpdateState(state.copy(showSplitWipe = !state.showSplitWipe)) }
                )

                // Iconic Vermilion Shutter Export Capsule
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(LeicaVermilion, VermilionSoft))
                        )
                        .clickable { showExportModal = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Export",
                        tint = ChalkWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "EXPORT",
                        color = ChalkWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Calibrated Reference Darkroom Stage
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B0C10))
                .border(1.dp, MachinedBevel, RoundedCornerShape(20.dp))
                .onSizeChanged {
                    stageW = it.width.coerceAtLeast(1)
                    stageH = it.height.coerceAtLeast(1)
                }
                .then(
                    if (!state.showSplitWipe && state.activeModule != DarkroomModule.MASK) {
                        Modifier.transformable(state = transformState)
                    } else {
                        Modifier
                    }
                )
                .pointerInput(state.showSplitWipe, state.activeModule, state.maskType) {
                    detectDragGestures { change, _ ->
                        val nx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                        val ny = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                        if (latestState.showSplitWipe) {
                            change.consume()
                            latestOnUpdate(latestState.copy(splitFraction = nx))
                        } else if (latestState.activeModule == DarkroomModule.MASK &&
                            latestState.maskType != MaskType.NONE
                        ) {
                            change.consume()
                            latestOnUpdate(latestState.copy(maskCenterX = nx, maskCenterY = ny))
                        }
                    }
                }
        ) {
            val curGraded = gradedImage
            val curRaw = rawImage

            if (curGraded != null) {
                if (state.showSplitWipe && curRaw != null) {
                    Image(
                        bitmap = curRaw,
                        contentDescription = "RAW Plate",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        bitmap = curGraded,
                        contentDescription = "Graded Plate",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(left = size.width * state.splitFraction) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                } else {
                    Image(
                        bitmap = curGraded,
                        contentDescription = "Master Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    )
                }
            } else {
                CircularProgressIndicator(color = LeicaVermilion, strokeWidth = 2.dp)
            }

            // Crop Rule-of-Thirds & Golden Frame Guide
            if (state.activeModule == DarkroomModule.CROP) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (g in 1..2) {
                        val gx = size.width * (g / 3f)
                        val gy = size.height * (g / 3f)
                        drawLine(Color.White.copy(alpha = 0.28f), Offset(gx, 0f), Offset(gx, size.height), 1f)
                        drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, gy), Offset(size.width, gy), 1f)
                    }
                    drawRect(color = ChampagneBrass.copy(alpha = 0.75f), style = Stroke(width = 1.5.dp.toPx()))
                }
            }

            // Selective Mask Interactive Pin Overlay
            if (state.activeModule == DarkroomModule.MASK && state.maskType != MaskType.NONE) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (stageW * state.maskCenterX).roundToInt() - 16.dp.roundToPx(),
                                y = (stageH * state.maskCenterY).roundToInt() - 16.dp.roundToPx()
                            )
                        }
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CobaltMask.copy(alpha = 0.25f))
                        .border(2.dp, CobaltMask, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ChalkWhite)
                    )
                }
            }

            // Split-Screen Before/After Wipe Bar
            if (state.showSplitWipe) {
                val sxPx = (stageW * state.splitFraction).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(sxPx - 1, 0) }
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(ChalkWhite)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = sxPx - 18.dp.roundToPx(),
                                y = (stageH * 0.5f).roundToInt() - 18.dp.roundToPx()
                            )
                        }
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TitaniumChassis)
                        .border(1.5.dp, LeicaVermilion, CircleShape)
                ) {
                    Text(
                        text = "A|B",
                        color = ChalkWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    )
                }
            }

            // Top-Left Live RGB Parade Scope
            if (state.showScope) {
                RgbParadeScope(
                    scope = liveScope,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }

            // Bottom Lens Barrel EXIF Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC08080A))
                    .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${state.exif.cameraModel}  ·  ${state.exif.exposureSummary}",
                    color = ChampagneBrass,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp
                )
            }
        }

        // 3. Machined Control Island + Floating Material 3 Expressive Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TitaniumChassis)
                .border(1.dp, MachinedBevel, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 8.dp)
        ) {
            when (state.activeModule) {
                DarkroomModule.FILM -> FilmStocksModule(state, onUpdateState)
                DarkroomModule.LIGHT -> LightToneModule(state, onUpdateState)
                DarkroomModule.CURVES -> ProSplineCurve(
                    activeChannel = state.activeCurveChannel,
                    curves = state.curves,
                    scope = liveScope,
                    onSelectChannel = { onUpdateState(state.copy(activeCurveChannel = it)) },
                    onUpdateChannel = { ch, pts ->
                        val nextMap = state.curves.toMutableMap().apply { put(ch, pts) }
                        onUpdateState(state.copy(curves = nextMap))
                    },
                    onResetChannel = { ch ->
                        val nextMap = state.curves.toMutableMap().apply {
                            put(ch, listOf(0f, 0.25f, 0.5f, 0.75f, 1f))
                        }
                        onUpdateState(state.copy(curves = nextMap))
                    }
                )
                DarkroomModule.WHEELS -> ColorGradingWheels(
                    shadows = state.shadowWheel,
                    midtones = state.midtoneWheel,
                    highlights = state.highlightWheel,
                    onShadowsChange = { onUpdateState(state.copy(shadowWheel = it)) },
                    onMidtonesChange = { onUpdateState(state.copy(midtoneWheel = it)) },
                    onHighlightsChange = { onUpdateState(state.copy(highlightWheel = it)) },
                    onResetAll = {
                        onUpdateState(
                            state.copy(
                                shadowWheel = WheelGrade(),
                                midtoneWheel = WheelGrade(),
                                highlightWheel = WheelGrade()
                            )
                        )
                    }
                )
                DarkroomModule.HSL -> HslEqualizerModule(state, onUpdateState)
                DarkroomModule.ANALOG -> AnalogEmulsionModule(state, onUpdateState)
                DarkroomModule.MASK -> SelectiveMaskModule(state, onUpdateState)
                DarkroomModule.CROP -> GeometryCropModule(state, onUpdateState)
                DarkroomModule.PRINT -> GalleryPrintModule(state, onUpdateState)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Floating Material 3 Expressive Morphing Tool Capsule Bar
            val moduleIcons: Map<DarkroomModule, ImageVector> = mapOf(
                DarkroomModule.FILM to Icons.Rounded.CameraRoll,
                DarkroomModule.LIGHT to Icons.Rounded.WbSunny,
                DarkroomModule.CURVES to Icons.Rounded.Timeline,
                DarkroomModule.WHEELS to Icons.Rounded.Palette,
                DarkroomModule.HSL to Icons.Rounded.Tune,
                DarkroomModule.ANALOG to Icons.Rounded.Flare,
                DarkroomModule.MASK to Icons.Rounded.LensBlur,
                DarkroomModule.CROP to Icons.Rounded.CropRotate,
                DarkroomModule.PRINT to Icons.Rounded.CropOriginal
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(DarkroomModule.entries) { mod ->
                    val active = state.activeModule == mod
                    val bgCol by animateColorAsState(
                        targetValue = if (active) LeicaVermilion else GraphiteIsland,
                        label = "ModuleCapsuleBg"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bgCol)
                            .border(
                                1.dp,
                                if (active) LeicaVermilion else MachinedBevel,
                                RoundedCornerShape(50)
                            )
                            .clickable { onUpdateState(state.copy(activeModule = mod)) }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = moduleIcons[mod] ?: Icons.Rounded.Tune,
                            contentDescription = mod.label,
                            tint = if (active) ChalkWhite else SilverLabel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mod.label,
                            color = if (active) ChalkWhite else SilverLabel,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Real MediaStore Export & Native Share Bottom Sheet
    if (showExportModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var chosenFormat by remember { mutableStateOf(ExportFormat.JPEG) }
        var qualityVal by remember { mutableFloatStateOf(96f) }

        ModalBottomSheet(
            onDismissRequest = { if (!isExporting) showExportModal = false },
            sheetState = sheetState,
            containerColor = TitaniumChassis,
            contentColor = ChalkWhite
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
                            text = "Develop & Export Master",
                            color = ChalkWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Saves directly to /Pictures/VeloxAtelier",
                            color = SilverLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CineMint.copy(alpha = 0.16f))
                            .border(1.dp, CineMint, RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "REAL BITMAP",
                            color = CineMint,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Format Capsules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { fmt ->
                        val active = chosenFormat == fmt
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) LeicaVermilion.copy(alpha = 0.18f) else GraphiteIsland)
                                .border(1.dp, if (active) LeicaVermilion else MachinedBevel, RoundedCornerShape(12.dp))
                                .clickable { chosenFormat = fmt }
                                .padding(vertical = 11.dp)
                        ) {
                            Text(
                                text = fmt.label,
                                color = if (active) ChalkWhite else SilverLabel,
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
                        Text("MASTER QUALITY", color = PewterMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${qualityVal.roundToInt()}%",
                            color = ChampagneBrass,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = qualityVal,
                        onValueChange = { qualityVal = it },
                        valueRange = 70f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = LeicaVermilion,
                            activeTrackColor = LeicaVermilion
                        )
                    )
                }

                // Dual Action Row: Save to Gallery + Native Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(LeicaVermilion, VermilionSoft)))
                            .clickable(enabled = !isExporting && sourceBitmap != null) {
                                val src = sourceBitmap ?: return@clickable
                                isExporting = true
                                coroutineScope.launch {
                                    val savedUri = withContext(Dispatchers.IO) {
                                        BitmapGradeEngine.exportMasterToGallery(
                                            context = context,
                                            source = src,
                                            state = state,
                                            format = chosenFormat,
                                            quality = qualityVal.roundToInt(),
                                            shareAfterSave = false
                                        )
                                    }
                                    isExporting = false
                                    showExportModal = false
                                    Toast.makeText(
                                        context,
                                        if (savedUri != null) "Saved to Gallery (/Pictures/VeloxAtelier)"
                                        else "Export failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = ChalkWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = if (isExporting) "Developing..." else "Save to Gallery",
                            color = ChalkWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KnurledCapsule)
                            .border(1.dp, ChampagneBrass, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isExporting && sourceBitmap != null) {
                                val src = sourceBitmap ?: return@clickable
                                isExporting = true
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        BitmapGradeEngine.exportMasterToGallery(
                                            context = context,
                                            source = src,
                                            state = state,
                                            format = chosenFormat,
                                            quality = qualityVal.roundToInt(),
                                            shareAfterSave = true
                                        )
                                    }
                                    isExporting = false
                                    showExportModal = false
                                }
                            }
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = ChampagneBrass, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = ChampagneBrass, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudCapsuleIcon(
    icon: ImageVector,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (active) LeicaVermilion.copy(alpha = 0.2f) else GraphiteIsland)
            .border(
                1.dp,
                if (active) LeicaVermilion else MachinedBevel,
                CircleShape
            )
            .clickable(enabled = enabled) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> PewterMuted.copy(alpha = 0.35f)
                active -> LeicaVermilion
                else -> ChalkWhite
            },
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FilmStocksModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILM_STOCK_RECIPES) { stock ->
                val selected = state.activeRecipeId == stock.id
                Column(
                    modifier = Modifier
                        .width(102.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) KnurledCapsule else GraphiteIsland)
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) LeicaVermilion else MachinedBevel,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            val nextCurves = state.curves.toMutableMap().apply {
                                put(CurveChannelId.LUMA, stock.lumaCurve)
                            }
                            onUpdate(
                                state.copy(
                                    activeRecipeId = stock.id,
                                    exposure = stock.exposure,
                                    contrast = stock.contrast,
                                    highlights = stock.highlights,
                                    shadows = stock.shadows,
                                    temperature = stock.temperature,
                                    vibrance = stock.vibrance,
                                    saturation = stock.saturation,
                                    halation = stock.halation,
                                    bloom = stock.bloom,
                                    grain = stock.grain,
                                    matteFade = stock.matteFade,
                                    shadowWheel = stock.shadowWheel,
                                    highlightWheel = stock.highlightWheel,
                                    curves = nextCurves
                                )
                            )
                        }
                        .padding(10.dp)
                ) {
                    Text(
                        text = stock.code,
                        color = stock.accent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stock.name,
                        color = ChalkWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Text(
                        text = stock.category,
                        color = PewterMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        KnurledRotaryDial(
            label = "Emulsion Strength",
            value = state.recipeIntensity,
            range = 0f..100f,
            defaultValue = 85f,
            unit = "%",
            onValueChange = { onUpdate(state.copy(recipeIntensity = it)) }
        )
    }
}

@Composable
private fun LightToneModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
    val items = listOf(
        "Exposure" to state.exposure,
        "Contrast" to state.contrast,
        "Highlights" to state.highlights,
        "Shadows" to state.shadows,
        "Whites" to state.whites,
        "Blacks" to state.blacks
    )
    val curVal = items.find { it.first == state.activeLightParam }?.second ?: 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        ParameterChipRow(
            items = items,
            selectedKey = state.activeLightParam,
            onSelect = { onUpdate(state.copy(activeLightParam = it)) }
        )
        Spacer(modifier = Modifier.height(4.dp))
        KnurledRotaryDial(
            label = state.activeLightParam,
            value = curVal,
            onValueChange = { v ->
                onUpdate(
                    when (state.activeLightParam) {
                        "Exposure" -> state.copy(exposure = v)
                        "Contrast" -> state.copy(contrast = v)
                        "Highlights" -> state.copy(highlights = v)
                        "Shadows" -> state.copy(shadows = v)
                        "Whites" -> state.copy(whites = v)
                        else -> state.copy(blacks = v)
                    }
                )
            }
        )
    }
}

@Composable
private fun HslEqualizerModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(50))
                .background(GraphiteIsland)
                .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                .padding(3.dp)
        ) {
            listOf("White Balance", "8-Zone Color EQ").forEachIndexed { idx, label ->
                val sel = state.colorTabIndex == idx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) KnurledCapsule else Color.Transparent)
                        .clickable { onUpdate(state.copy(colorTabIndex = idx)) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (sel) ChampagneBrass else SilverLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (state.colorTabIndex == 0) {
            KnurledRotaryDial(
                label = "Kelvin Temp",
                value = state.temperature,
                onValueChange = { onUpdate(state.copy(temperature = it)) }
            )
            KnurledRotaryDial(
                label = "Skin-Safe Vibrance",
                value = state.vibrance,
                onValueChange = { onUpdate(state.copy(vibrance = it)) }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                state.hslZones.forEachIndexed { i, zone ->
                    val sel = state.selectedHslZone == i
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(zone.color.copy(alpha = if (sel) 1f else 0.42f))
                            .border(if (sel) 2.5.dp else 0.dp, ChalkWhite, CircleShape)
                            .clickable { onUpdate(state.copy(selectedHslZone = i)) }
                    )
                }
            }
            val z = state.hslZones[state.selectedHslZone]
            KnurledRotaryDial(
                label = "${z.label} · Saturation",
                value = z.sat,
                onValueChange = { ns ->
                    val next = state.hslZones.toMutableList().apply {
                        this[state.selectedHslZone] = z.copy(sat = ns)
                    }
                    onUpdate(state.copy(hslZones = next))
                }
            )
            KnurledRotaryDial(
                label = "${z.label} · Luminance",
                value = z.lum,
                onValueChange = { nl ->
                    val next = state.hslZones.toMutableList().apply {
                        this[state.selectedHslZone] = z.copy(lum = nl)
                    }
                    onUpdate(state.copy(hslZones = next))
                }
            )
        }
    }
}

@Composable
private fun AnalogEmulsionModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
    val items = listOf(
        "Halation" to state.halation,
        "Bloom" to state.bloom,
        "35mm Grain" to state.grain,
        "Clarity" to state.clarity,
        "Matte Lift" to state.matteFade,
        "Vignette" to state.vignette
    )
    val curVal = items.find { it.first == state.activeAnalogParam }?.second ?: 0f
    val range = if (state.activeAnalogParam == "Clarity") -100f..100f else 0f..100f

    Column(modifier = Modifier.fillMaxWidth()) {
        ParameterChipRow(
            items = items,
            selectedKey = state.activeAnalogParam,
            onSelect = { onUpdate(state.copy(activeAnalogParam = it)) }
        )
        Spacer(modifier = Modifier.height(4.dp))
        KnurledRotaryDial(
            label = "${state.activeAnalogParam} (Analog Emulsion)",
            value = curVal,
            range = range,
            defaultValue = 0f,
            onValueChange = { v ->
                onUpdate(
                    when (state.activeAnalogParam) {
                        "Halation" -> state.copy(halation = v)
                        "Bloom" -> state.copy(bloom = v)
                        "35mm Grain" -> state.copy(grain = v)
                        "Clarity" -> state.copy(clarity = v)
                        "Matte Lift" -> state.copy(matteFade = v)
                        else -> state.copy(vignette = v)
                    }
                )
            }
        )
    }
}

@Composable
private fun SelectiveMaskModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
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
                MaskType.entries.forEach { mt ->
                    val sel = state.maskType == mt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) CobaltMask.copy(alpha = 0.2f) else GraphiteIsland)
                            .border(1.dp, if (sel) CobaltMask else MachinedBevel, RoundedCornerShape(50))
                            .clickable { onUpdate(state.copy(maskType = mt)) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mt.label,
                            color = if (sel) CobaltMask else SilverLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ruby Mask Isolation Overlay Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (state.showRubyMaskOverlay) LeicaVermilion else GraphiteIsland)
                    .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                    .clickable {
                        onUpdate(state.copy(showRubyMaskOverlay = !state.showRubyMaskOverlay))
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = "Ruby Mask",
                    tint = ChalkWhite,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("RUBY", color = ChalkWhite, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        KnurledRotaryDial(
            label = "Local Mask Exposure (Drag Pin on Photo)",
            value = state.maskExposure,
            onValueChange = { onUpdate(state.copy(maskExposure = it)) }
        )
    }
}

@Composable
private fun GeometryCropModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
    val ratios = listOf("Original", "1:1", "4:5", "3:2", "16:9", "9:16")
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
                ratios.forEach { r ->
                    val sel = state.aspectRatio == r
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) KnurledCapsule else GraphiteIsland)
                            .border(1.dp, if (sel) ChampagneBrass else MachinedBevel, RoundedCornerShape(50))
                            .clickable { onUpdate(state.copy(aspectRatio = r)) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = r,
                            color = if (sel) ChampagneBrass else SilverLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudCapsuleIcon(
                    icon = Icons.Rounded.Rotate90DegreesCcw,
                    active = state.quarterTurns != 0,
                    onClick = { onUpdate(state.copy(quarterTurns = (state.quarterTurns + 1) % 4)) }
                )
                HudCapsuleIcon(
                    icon = Icons.Rounded.Flip,
                    active = state.mirrorHorizontal,
                    onClick = { onUpdate(state.copy(mirrorHorizontal = !state.mirrorHorizontal)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        KnurledRotaryDial(
            label = "Horizon Angle",
            value = state.straightenDegrees,
            range = -45f..45f,
            unit = "°",
            onValueChange = { onUpdate(state.copy(straightenDegrees = it)) }
        )
    }
}

@Composable
private fun GalleryPrintModule(state: DarkroomState, onUpdate: (DarkroomState) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PrintBorderStyle.entries.forEach { style ->
                val sel = state.printStyle == style
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) KnurledCapsule else GraphiteIsland)
                        .border(1.dp, if (sel) LeicaVermilion else MachinedBevel, RoundedCornerShape(50))
                        .clickable { onUpdate(state.copy(printStyle = style)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = style.label,
                        color = if (sel) ChalkWhite else SilverLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.signatureText,
            onValueChange = { onUpdate(state.copy(signatureText = it)) },
            placeholder = {
                Text(
                    "Custom print title / artist stamp (defaults to Camera EXIF)",
                    fontSize = 11.sp,
                    color = PewterMuted
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChampagneBrass,
                unfocusedBorderColor = MachinedBevel,
                focusedTextColor = ChalkWhite,
                unfocusedTextColor = ChalkWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ParameterChipRow(
    items: List<Pair<String, Float>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(items) { (name, v) ->
            val sel = selectedKey == name
            val iv = v.roundToInt()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) KnurledCapsule else GraphiteIsland)
                    .border(
                        1.dp,
                        if (sel) ChampagneBrass else MachinedBevel,
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelect(name) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = name,
                    color = if (sel) ChalkWhite else SilverLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (iv != 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (iv > 0) "+$iv" else "$iv",
                        color = ChampagneBrass,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
