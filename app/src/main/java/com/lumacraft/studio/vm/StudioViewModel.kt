package com.lumacraft.studio.vm

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumacraft.studio.engine.ImageEditEngine
import com.lumacraft.studio.engine.LocalStores
import com.lumacraft.studio.engine.SegMask
import com.lumacraft.studio.engine.Segmenter
import com.lumacraft.studio.model.Adjustments
import com.lumacraft.studio.model.AppSettings
import com.lumacraft.studio.model.BatchConfig
import com.lumacraft.studio.model.BrushMode
import com.lumacraft.studio.model.BrushStroke
import com.lumacraft.studio.model.CollageConfig
import com.lumacraft.studio.model.COLLAGE_TEMPLATES
import com.lumacraft.studio.model.CurveChannel
import com.lumacraft.studio.model.DrawStroke
import com.lumacraft.studio.model.EditState
import com.lumacraft.studio.model.EmojiLayer
import com.lumacraft.studio.model.ExportFormat
import com.lumacraft.studio.model.ExportOptions
import com.lumacraft.studio.model.FilterDef
import com.lumacraft.studio.model.ALL_FILTERS
import com.lumacraft.studio.model.HealSpot
import com.lumacraft.studio.model.PresetItem
import com.lumacraft.studio.model.RecentItem
import com.lumacraft.studio.model.Screen
import com.lumacraft.studio.model.TapFix
import com.lumacraft.studio.model.TextLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TapTool { NONE, HEAL, RED_EYE, TEETH }
enum class BrushTool { NONE, DRAW, BLUR, PIXELATE, ERASE }

data class StudioUiState(
    val screen: Screen = Screen.HOME,
    // photo
    val source: Bitmap? = null,
    val sourceUri: Uri? = null,
    val photoName: String = "Untitled",
    val photoInfo: String = "",
    val photoDims: String = "",
    // editing
    val state: EditState = EditState(),
    val seg: SegMask? = null,
    val preview: ImageBitmap? = null,
    val originalPreview: ImageBitmap? = null,
    val hist: IntArray = IntArray(32),
    val clipShadow: Boolean = false,
    val clipHighlight: Boolean = false,
    val filterThumbs: Map<String, ImageBitmap> = emptyMap(),
    // meta
    val busy: String? = null,
    val toast: String? = null,
    val compareHold: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // tool modes
    val tapTool: TapTool = TapTool.NONE,
    val brushTool: BrushTool = BrushTool.NONE,
    val drawColor: Long = 0xFFFF5470,
    val drawWidth: Float = 0.008f,
    val brushRadius: Float = 0.05f,
    val livePoints: List<Pair<Float, Float>> = emptyList(),
    // data
    val userPresets: List<PresetItem> = emptyList(),
    val recents: List<RecentItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val exportOptions: ExportOptions = ExportOptions(),
    // collage
    val collage: CollageConfig = CollageConfig(),
    val collagePreview: ImageBitmap? = null,
    val collageActiveCell: Int = 0,
    // batch
    val batch: BatchConfig = BatchConfig(),
    val batchRunning: Boolean = false,
    val batchDone: Int = 0
)

class StudioViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(StudioUiState())
    val ui: StateFlow<StudioUiState> = _ui

    private val history = ArrayDeque<EditState>()
    private val redoStack = ArrayDeque<EditState>()
    private var previewJob: Job? = null
    private var collageJob: Job? = null
    private var sessionJob: Job? = null
    private var lastHistoryKey: String? = null
    private var lastHistoryAt: Long = 0
    @Volatile private var batchCancel = false

    init {
        val s = LocalStores.loadSettings(app)
        _ui.update {
            it.copy(
                settings = s,
                recents = LocalStores.loadRecents(app),
                userPresets = LocalStores.loadUserPresets(app),
                exportOptions = ExportOptions(format = s.defaultFormat, quality = s.defaultQuality, stripExif = s.stripExifDefault),
                collage = CollageConfig(bgColor = 0xFF0E0F12)
            )
        }
    }

    private fun ctx() = getApplication<Application>()

    fun consumeToast() {
        _ui.update { it.copy(toast = null) }
    }

    fun notify(msg: String) {
        toast(msg)
    }

    private fun toast(msg: String) {
        _ui.update { it.copy(toast = msg) }
    }

    // ------------------------------------------------------------ navigation

    fun goScreen(screen: Screen) {
        if (_ui.value.screen == Screen.EDITOR && screen != Screen.EDITOR) {
            saveSessionNow()
        }
        _ui.update { it.copy(screen = screen) }
        if (screen == Screen.COLLAGE) refreshCollagePreview()
    }

    // ------------------------------------------------------------ photo open / close

    fun openPhoto(uri: Uri?, name: String, restore: Boolean = true, sampleKind: Int = 0) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = "Loading photo…") }
            val loaded = withContext(Dispatchers.IO) {
                ImageEditEngine.decode(ctx(), uri, 2048, sampleKind)
            }
            if (loaded == null) {
                _ui.update { it.copy(busy = null) }
                toast("Could not open that photo")
                return@launch
            }
            val restored = if (restore && uri != null) {
                withContext(Dispatchers.IO) { LocalStores.loadSession(ctx(), uri.toString()) }
            } else null

            history.clear()
            redoStack.clear()
            _ui.update {
                it.copy(
                    source = loaded.bitmap,
                    sourceUri = uri,
                    photoName = name.ifEmpty { "Photo ${System.currentTimeMillis() % 10000}" },
                    photoInfo = loaded.info,
                    photoDims = loaded.dims,
                    state = restored ?: EditState(),
                    preview = null,
                    originalPreview = null,
                    filterThumbs = emptyMap(),
                    tapTool = TapTool.NONE,
                    brushTool = BrushTool.NONE,
                    busy = null,
                    screen = Screen.EDITOR
                )
            }
            if (uri != null) {
                withContext(Dispatchers.IO) { LocalStores.addRecent(ctx(), uri.toString(), _ui.value.photoName) }
                _ui.update { it.copy(recents = LocalStores.loadRecents(ctx())) }
            }

            // original (compare) preview
            viewModelScope.launch(Dispatchers.Default) {
                val orig = ImageEditEngine.render(loaded.bitmap, EditState(), null, 760, overlays = false)
                _ui.update { it.copy(originalPreview = orig.bitmap.asImageBitmap()) }
            }
            // segmentation (offline ML Kit)
            viewModelScope.launch(Dispatchers.Default) {
                val src = _ui.value.source ?: return@launch
                val small = if (maxOf(src.width, src.height) > 640) {
                    val sc = 640f / maxOf(src.width, src.height)
                    Bitmap.createScaledBitmap(src, (src.width * sc).toInt().coerceAtLeast(1), (src.height * sc).toInt().coerceAtLeast(1), true)
                } else src
                val mask = Segmenter.segment(small)
                _ui.update { it.copy(seg = mask) }
            }
            // filter thumbnails
            viewModelScope.launch(Dispatchers.Default) { buildFilterThumbs(loaded.bitmap) }

            runPreview()
        }
    }

    fun closeEditor() {
        saveSessionNow()
        _ui.update { it.copy(screen = Screen.HOME) }
    }

    private suspend fun buildFilterThumbs(src: Bitmap) {
        val small = if (maxOf(src.width, src.height) > 140) {
            val sc = 140f / maxOf(src.width, src.height)
            Bitmap.createScaledBitmap(src, (src.width * sc).toInt().coerceAtLeast(1), (src.height * sc).toInt().coerceAtLeast(1), true)
        } else src
        ALL_FILTERS.forEach { f ->
            val frame = ImageEditEngine.render(small, EditState(adjust = f.adjust, split = f.split ?: com.lumacraft.studio.model.SplitTone()), null, 140, overlays = false)
            _ui.update { it.copy(filterThumbs = it.filterThumbs + (f.id to frame.bitmap.asImageBitmap())) }
        }
    }

    // ------------------------------------------------------------ history / state edits

    private fun pushHistory(key: String? = null) {
        val now = System.currentTimeMillis()
        if (key != null && key == lastHistoryKey && now - lastHistoryAt < 900) {
            lastHistoryAt = now
            return
        }
        lastHistoryKey = key
        lastHistoryAt = now
        history.addLast(_ui.value.state)
        if (history.size > 40) history.removeFirst()
        redoStack.clear()
        _ui.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        val last = history.removeLastOrNull() ?: return
        redoStack.addLast(_ui.value.state)
        _ui.update { it.copy(state = last, canUndo = history.isNotEmpty(), canRedo = true) }
        runPreview()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        history.addLast(_ui.value.state)
        _ui.update { it.copy(state = next, canUndo = true, canRedo = redoStack.isNotEmpty()) }
        runPreview()
    }

    /** Mutate the edit state. [historyKey] coalesces rapid slider drags into one undo step. */
    fun edit(historyKey: String? = null, rerender: Boolean = true, mutate: (EditState) -> EditState) {
        pushHistory(historyKey)
        _ui.update { it.copy(state = mutate(it.state)) }
        if (rerender) runPreview() else scheduleSessionSave()
    }

    fun resetAdjust() {
        edit { it.copy(adjust = Adjustments()) }
    }

    fun resetAll() {
        edit { EditState(filterId = null) }
        toast("All edits reset")
    }

    // ------------------------------------------------------------ preview pipeline

    private fun runPreview() {
        val src = _ui.value.source ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            delay(140) // debounce slider storms
            val st = _ui.value
            val frame = ImageEditEngine.render(src, st.state, st.seg, 760, overlays = true)
            val ib = frame.bitmap.asImageBitmap()
            _ui.update {
                it.copy(
                    preview = ib,
                    hist = frame.hist,
                    clipShadow = frame.clipShadow,
                    clipHighlight = frame.clipHighlight
                )
            }
            scheduleSessionSave()
        }
    }

    private fun scheduleSessionSave() {
        val uri = _ui.value.sourceUri?.toString() ?: return
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            LocalStores.saveSession(ctx(), uri, _ui.value.state)
        }
    }

    fun saveSessionNow() {
        val uri = _ui.value.sourceUri?.toString() ?: return
        val st = _ui.value.state
        viewModelScope.launch(Dispatchers.IO) { LocalStores.saveSession(ctx(), uri, st) }
    }

    fun setCompareHold(hold: Boolean) {
        _ui.update { it.copy(compareHold = hold) }
    }

    // ------------------------------------------------------------ tool modes

    fun setTapTool(t: TapTool) = _ui.update { it.copy(tapTool = t, brushTool = BrushTool.NONE) }
    fun setBrushTool(b: BrushTool) = _ui.update { it.copy(brushTool = b, tapTool = TapTool.NONE) }
    fun setDrawColor(c: Long) = _ui.update { it.copy(drawColor = c) }
    fun setDrawWidth(w: Float) = _ui.update { it.copy(drawWidth = w) }
    fun setBrushRadius(r: Float) = _ui.update { it.copy(brushRadius = r) }

    /** Tap on canvas with the active tap tool. */
    fun onCanvasTap(nx: Float, ny: Float) {
        val tool = _ui.value.tapTool
        when (tool) {
            TapTool.NONE -> return
            TapTool.HEAL -> edit(rerender = true) { it.copy(healSpots = it.healSpots + HealSpot(nx, ny, _ui.value.brushRadius)) }
            TapTool.RED_EYE -> edit { it.copy(redEyeSpots = it.redEyeSpots + TapFix(nx, ny, 0.045f)) }
            TapTool.TEETH -> edit { it.copy(teethSpots = it.teethSpots + TapFix(nx, ny, 0.05f)) }
        }
    }

    fun onStrokeStart(nx: Float, ny: Float) {
        val tool = _ui.value.brushTool
        if (tool == BrushTool.NONE) return
        _ui.update { it.copy(livePoints = listOf(nx to ny)) }
    }

    fun onStrokeMove(nx: Float, ny: Float) {
        if (_ui.value.brushTool == BrushTool.NONE) return
        _ui.update { it.copy(livePoints = it.livePoints + (nx to ny)) }
    }

    fun onStrokeEnd() {
        val st = _ui.value
        val tool = st.brushTool
        if (tool == BrushTool.NONE || st.livePoints.isEmpty()) return
        val pts = st.livePoints
        _ui.update { it.copy(livePoints = emptyList()) }
        when (tool) {
            BrushTool.DRAW -> edit(rerender = true) {
                it.copy(drawStrokes = it.drawStrokes + DrawStroke(st.drawColor, st.drawWidth, pts))
            }
            BrushTool.BLUR -> edit {
                it.copy(brushStrokes = it.brushStrokes + BrushStroke(BrushMode.BLUR, st.brushRadius, pts))
            }
            BrushTool.PIXELATE -> edit {
                it.copy(brushStrokes = it.brushStrokes + BrushStroke(BrushMode.PIXELATE, st.brushRadius, pts))
            }
            BrushTool.ERASE -> edit {
                it.copy(brushStrokes = it.brushStrokes + BrushStroke(BrushMode.REMOVE, st.brushRadius, pts))
            }
            BrushTool.NONE -> {}
        }
    }

    fun clearHealSpots() = edit { it.copy(healSpots = emptyList(), brushStrokes = emptyList(), redEyeSpots = emptyList(), teethSpots = emptyList(), drawStrokes = emptyList()) }

    // ------------------------------------------------------------ filters & presets

    fun applyFilter(f: FilterDef) {
        edit {
            val i = _ui.value.state.filterIntensity / 100f
            val scaled = if (i >= 0.999f) f.adjust else scaleAdjust(f.adjust, i)
            it.copy(
                adjust = scaled,
                split = f.split ?: it.split,
                curves = f.masterCurve?.let { c ->
                    it.curves + (CurveChannel.MASTER to lerpCurve(it.curves[CurveChannel.MASTER]!!, c, i))
                } ?: it.curves,
                filterId = f.id
            )
        }
    }

    fun setFilterIntensity(v: Float) {
        val fid = _ui.value.state.filterId
        val f = ALL_FILTERS.firstOrNull { it.id == fid }
        edit(historyKey = "filterIntensity") {
            if (f != null) {
                it.copy(filterIntensity = v, adjust = scaleAdjust(f.adjust, v / 100f))
            } else {
                it.copy(filterIntensity = v)
            }
        }
    }

    private fun scaleAdjust(a: Adjustments, k: Float): Adjustments = a.copy(
        exposure = a.exposure * k, brightness = a.brightness * k, contrast = a.contrast * k,
        highlights = a.highlights * k, shadows = a.shadows * k, whites = a.whites * k,
        blacks = a.blacks * k, saturation = a.saturation * k, vibrance = a.vibrance * k,
        temperature = a.temperature * k, tint = a.tint * k, hueShift = a.hueShift * k,
        sharpness = a.sharpness * k, clarity = a.clarity * k, texture = a.texture * k,
        dehaze = a.dehaze * k, fade = a.fade * k, vignette = a.vignette * k, grain = a.grain * k
    )

    private fun lerpCurve(base: List<Float>, target: List<Float>, k: Float): List<Float> =
        base.indices.map { i -> base[i] + (target[i] - base[i]) * k }

    fun applyPreset(p: PresetItem) {
        edit { p.state.copy(geo = it.geo) } // keep current crop/rotation, take the look
        toast("Preset \"${p.name}\" applied")
    }

    fun applyBuiltInPreset(name: String, state: EditState) {
        edit { state.copy(geo = it.geo) }
        toast("Preset \"$name\" applied")
    }

    // ------------------------------------------------------------ AI tools

    fun runAiTool(id: String) {
        when (id) {
            "enhance" -> {
                val src = _ui.value.source ?: return
                viewModelScope.launch(Dispatchers.Default) {
                    _ui.update { it.copy(busy = "Analyzing exposure & color…") }
                    val adj = ImageEditEngine.autoEnhance(src)
                    _ui.update { it.copy(busy = null) }
                    edit { s -> s.copy(adjust = adj, filterId = null) }
                }
            }
            "restore" -> edit { s ->
                s.copy(adjust = boost(s.adjust, Adjustments(shadows = 16f, dehaze = 12f, noiseReduction = 34f, sharpness = 20f, saturation = 6f)))
            }
            "oldphoto" -> edit { s ->
                s.copy(
                    adjust = boost(s.adjust, Adjustments(exposure = 6f, contrast = 10f, shadows = 18f, fade = 0f, noiseReduction = 38f, sharpness = 22f, temperature = 8f)),
                    split = s.split.copy(shadowHue = 40f, shadowSat = 12f, highlightHue = 45f, highlightSat = 10f)
                )
            }
            "portrait" -> edit { s ->
                s.copy(skinSmooth = (s.skinSmooth + 30f).coerceAtMost(80f), naturalEnhance = (s.naturalEnhance + 25f).coerceAtMost(70f))
            }
            "upscale" -> {
                _ui.update { it.copy(exportOptions = it.exportOptions.copy(upscale2x = !_ui.value.exportOptions.upscale2x)) }
                toast(if (_ui.value.exportOptions.upscale2x) "2× upscale enabled (applies on export)" else "2× upscale disabled")
            }
            "cutout" -> edit { s -> s.copy(ai = s.ai.copy(cutout = !s.ai.cutout)) }
            "colorize" -> toast("AI Colorization needs a cloud model — not included (on-device privacy). Use \"Enhance\" instead.")
        }
    }

    private fun boost(base: Adjustments, extra: Adjustments): Adjustments = base.copy(
        exposure = base.exposure + extra.exposure, contrast = base.contrast + extra.contrast,
        shadows = base.shadows + extra.shadows, highlights = base.highlights + extra.highlights,
        saturation = base.saturation + extra.saturation, sharpness = (base.sharpness + extra.sharpness).coerceAtMost(100f),
        noiseReduction = (base.noiseReduction + extra.noiseReduction).coerceAtMost(100f),
        dehaze = (base.dehaze + extra.dehaze).coerceAtMost(100f), temperature = base.temperature + extra.temperature,
        fade = extra.fade
    )

    // ------------------------------------------------------------ presets management

    fun savePreset(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = LocalStores.saveUserPreset(ctx(), name, _ui.value.state)
            _ui.update { it.copy(userPresets = list) }
            toast("Preset \"${name.trim().ifEmpty { "My Preset" }}\" saved")
        }
    }

    fun renamePreset(id: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(userPresets = LocalStores.renameUserPreset(ctx(), id, newName)) }
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(userPresets = LocalStores.deleteUserPreset(ctx(), id)) }
        }
    }

    fun importPreset(json: String) {
        val st = com.lumacraft.studio.engine.StateJson.fromJson(json)
        if (st == null) {
            toast("Invalid preset file")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(userPresets = LocalStores.saveUserPreset(ctx(), "Imported Preset", st)) }
            toast("Preset imported")
        }
    }

    fun exportPresetJson(p: PresetItem): String = com.lumacraft.studio.engine.StateJson.toJson(p.state)

    // ------------------------------------------------------------ export

    fun setExportOptions(mutate: (ExportOptions) -> ExportOptions) {
        _ui.update { it.copy(exportOptions = mutate(it.exportOptions)) }
    }

    fun exportPhoto() {
        val st = _ui.value
        val src = st.source ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = "Rendering full-resolution master…") }
            val result = withContext(Dispatchers.IO) {
                try {
                    var exportState = st.state
                    if (st.exportOptions.resolutionMode == "Square 1080") {
                        exportState = exportState.copy(geo = exportState.geo.copy(aspect = "1:1"))
                    }
                    val bmp = ImageEditEngine.renderForExport(src, exportState, st.seg, st.exportOptions)
                    val fmt = if (exportState.ai.cutout) ExportFormat.PNG else st.exportOptions.format
                    ImageEditEngine.saveToGallery(
                        ctx(), bmp, fmt, st.exportOptions.quality, st.exportOptions.stripExif,
                        st.exportOptions.shareAfter, st.photoInfo
                    )
                } catch (e: Exception) {
                    null
                }
            }
            _ui.update { it.copy(busy = null) }
            if (result != null) {
                toast("Saved to Pictures/LumaCraft")
                saveSessionNow()
            } else {
                toast("Export failed — try a smaller resolution")
            }
        }
    }

    // ------------------------------------------------------------ collage

    fun addCollagePhotos(uris: List<Uri>) {
        _ui.update { it.copy(collage = it.collage.copy(photos = (it.collage.photos + uris).take(9)), screen = Screen.COLLAGE) }
        refreshCollagePreview()
    }

    fun updateCollage(mutate: (CollageConfig) -> CollageConfig) {
        _ui.update { it.copy(collage = mutate(it.collage)) }
        refreshCollagePreview()
    }

    fun setCollageActiveCell(i: Int) {
        _ui.update { it.copy(collageActiveCell = i) }
    }

    fun panCollageCell(dx: Float, dy: Float) {
        val active = _ui.value.collageActiveCell
        _ui.update {
            val cur = it.collage.cellOffsets[active] ?: (0f to 0f)
            val nx = (cur.first + dx).coerceIn(-0.5f, 0.5f)
            val ny = (cur.second + dy).coerceIn(-0.5f, 0.5f)
            it.copy(collage = it.collage.copy(cellOffsets = it.collage.cellOffsets + (active to (nx to ny))))
        }
        refreshCollagePreview()
    }

    private fun refreshCollagePreview() {
        val cfg = _ui.value.collage
        if (cfg.photos.isEmpty()) return
        collageJob?.cancel()
        collageJob = viewModelScope.launch(Dispatchers.Default) {
            delay(220)
            val cells = COLLAGE_TEMPLATES.getOrNull(cfg.templateIdx)?.cells ?: return@launch
            val bmp = ImageEditEngine.composeCollage(ctx(), cfg.copy(), cells) ?: return@launch
            _ui.update { it.copy(collagePreview = bmp.asImageBitmap()) }
        }
    }

    fun exportCollage() {
        val cfg = _ui.value.collage
        viewModelScope.launch {
            _ui.update { it.copy(busy = "Composing collage…") }
            val uri = withContext(Dispatchers.IO) {
                val cells = COLLAGE_TEMPLATES.getOrNull(cfg.templateIdx)?.cells ?: return@withContext null
                val bmp = ImageEditEngine.composeCollage(ctx(), cfg.copy(), cells) ?: return@withContext null
                ImageEditEngine.saveToGallery(ctx(), bmp, ExportFormat.JPEG, 92, true, false, "")
            }
            _ui.update { it.copy(busy = null) }
            toast(if (uri != null) "Collage saved to Pictures/LumaCraft" else "Collage export failed")
        }
    }

    // ------------------------------------------------------------ batch

    fun addBatchPhotos(uris: List<Uri>) {
        _ui.update { it.copy(batch = it.batch.copy(uris = (it.batch.uris + uris).distinct()), screen = Screen.BATCH) }
    }

    fun updateBatch(mutate: (BatchConfig) -> BatchConfig) {
        _ui.update { it.copy(batch = mutate(it.batch)) }
    }

    fun startBatch() {
        if (_ui.value.batchRunning || _ui.value.batch.uris.isEmpty()) return
        batchCancel = false
        val look = _ui.value.state
        val cfg = _ui.value.batch
        _ui.update { it.copy(batchRunning = true, batchDone = 0) }
        viewModelScope.launch(Dispatchers.IO) {
            var ok = 0
            cfg.uris.forEachIndexed { i, uri ->
                if (batchCancel) return@forEachIndexed
                try {
                    val loaded = ImageEditEngine.decode(ctx(), uri, cfg.maxDim)
                    if (loaded != null) {
                        val st = if (cfg.applyCurrentLook) look else EditState()
                        val bmp = ImageEditEngine.render(loaded.bitmap, st, null, cfg.maxDim, overlays = false).bitmap
                        val saved = ImageEditEngine.saveToGallery(ctx(), bmp, cfg.format, cfg.quality, true, false, "")
                        if (saved != null) ok++
                    }
                } catch (_: Exception) {
                }
                _ui.update { it.copy(batchDone = i + 1) }
            }
            _ui.update { it.copy(batchRunning = false) }
            toast(if (batchCancel) "Batch cancelled — $ok saved" else "Batch complete — $ok photos saved")
        }
    }

    fun cancelBatch() {
        batchCancel = true
    }

    // ------------------------------------------------------------ settings

    fun updateSettings(mutate: (AppSettings) -> AppSettings) {
        val s = mutate(_ui.value.settings)
        LocalStores.saveSettings(ctx(), s)
        _ui.update { it.copy(settings = s) }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            LocalStores.clearPreviewCache(ctx())
            _ui.update { it.copy(recents = LocalStores.loadRecents(ctx())) }
            toast("Saved sessions cleared")
        }
    }

    fun deleteRecent(item: RecentItem) {
        viewModelScope.launch(Dispatchers.IO) {
            LocalStores.deleteSession(ctx(), item.uri)
            _ui.update { it.copy(recents = LocalStores.loadRecents(ctx())) }
        }
    }
}
