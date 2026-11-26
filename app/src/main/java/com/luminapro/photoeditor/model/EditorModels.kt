package com.luminapro.photoeditor.model

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class AppScreen {
    HOME,
    EDITOR
}

enum class HomeNavTab(val label: String) {
    STUDIO("Studio"),
    PROJECTS("Drafts"),
    LUT_LAB("LUT Lab"),
    ENGINE("Engine")
}

enum class EditorTool(val label: String) {
    LUTS("LUTs"),
    LIGHT("Light"),
    CURVES("Curves"),
    HSL("Color"),
    OPTICS("Optics"),
    AI_LAB("AI Lab"),
    CROP("Crop"),
    LAYERS("Layers")
}

enum class CurveChannel(val label: String, val color: Color) {
    RGB("RGB", Color(0xFFF9FAFB)),
    RED("Red", Color(0xFFF87171)),
    GREEN("Green", Color(0xFF4ADE80)),
    BLUE("Blue", Color(0xFF60A5FA))
}

data class HslChannelValue(
    val key: String,
    val name: String,
    val swatch: Color,
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val luminance: Float = 0f
)

data class LayerStackItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val blendMode: String,
    val opacity: Float = 1f,
    val visible: Boolean = true
)

data class RawProjectSample(
    val id: String,
    val title: String,
    val formatBadge: String,
    val cameraSpec: String,
    val lensExif: String,
    val editedAgo: String,
    val steps: Int,
    val sceneIndex: Int
)

data class LutPreset(
    val id: String,
    val name: String,
    val category: String,
    val badge: String?,
    val accent: Color,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val warmth: Float = 0f,
    val saturation: Float = 0f,
    val vignette: Float = 0f,
    val fade: Float = 0f
)

data class EditorState(
    val activeSample: RawProjectSample = SAMPLE_PROJECTS.first(),
    val pickedImageUri: Uri? = null,
    val activeTool: EditorTool = EditorTool.LIGHT,

    // Preset
    val activePresetId: String = "raw_zero",
    val presetStrength: Float = 85f,

    // Light
    val selectedLightParam: String = "Exposure",
    val exposure: Float = 12f,
    val contrast: Float = 16f,
    val highlights: Float = -18f,
    val shadows: Float = 22f,
    val whites: Float = 8f,
    val blacks: Float = -6f,

    // Curves (5 control nodes x = 0, 0.25, 0.5, 0.75, 1.0)
    val selectedCurveChannel: CurveChannel = CurveChannel.RGB,
    val curves: Map<CurveChannel, List<Float>> = mapOf(
        CurveChannel.RGB to listOf(0.02f, 0.22f, 0.52f, 0.79f, 0.98f),
        CurveChannel.RED to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannel.GREEN to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannel.BLUE to listOf(0.03f, 0.26f, 0.5f, 0.74f, 0.96f)
    ),

    // Color & 8-Band HSL
    val colorSubTab: Int = 0, // 0 = WB, 1 = 8-Band HSL
    val temperature: Float = 14f,
    val tint: Float = 4f,
    val vibrance: Float = 20f,
    val saturation: Float = 6f,
    val selectedHslIndex: Int = 1, // Orange skin-tone default
    val hslBands: List<HslChannelValue> = DEFAULT_HSL_BANDS,

    // Optics & Detail
    val selectedOpticsParam: String = "Clarity",
    val clarity: Float = 18f,
    val sharpen: Float = 24f,
    val dehaze: Float = 10f,
    val vignette: Float = 20f,
    val grain: Float = 12f,
    val fade: Float = 0f,

    // AI Studio
    val aiAutoEnhance: Boolean = false,
    val aiRelightEnabled: Boolean = true,
    val aiRelightX: Float = 0.34f,
    val aiRelightY: Float = 0.30f,
    val aiRelightIntensity: Float = 46f,
    val aiBokehAperture: Float = 32f,
    val aiSuperRes: Boolean = false,

    // Crop & Transform
    val aspectRatio: String = "Original",
    val straightenAngle: Float = 0f,
    val rotationQuarter: Int = 0,
    val flipHorizontal: Boolean = false,
    val exifBorder: Boolean = false,

    // Viewport overlays
    val showHistogram: Boolean = true,
    val showSplitComparison: Boolean = false,
    val splitPosition: Float = 0.52f,

    // Layers
    val layers: List<LayerStackItem> = DEFAULT_LAYERS
)

val DEFAULT_HSL_BANDS = listOf(
    HslChannelValue("red", "Red", Color(0xFFEF4444)),
    HslChannelValue("orange", "Orange", Color(0xFFF97316), hue = 4f, saturation = 8f, luminance = 10f),
    HslChannelValue("yellow", "Yellow", Color(0xFFEAB308)),
    HslChannelValue("green", "Green", Color(0xFF22C55E)),
    HslChannelValue("aqua", "Aqua", Color(0xFF06B6D4), hue = -8f, saturation = 18f),
    HslChannelValue("blue", "Blue", Color(0xFF3B82F6)),
    HslChannelValue("purple", "Purple", Color(0xFFA855F7)),
    HslChannelValue("magenta", "Magenta", Color(0xFFEC4899))
)

val DEFAULT_LAYERS = listOf(
    LayerStackItem("frame", "Leica EXIF Gallery Frame", "Metadata Border", "Normal", 1f, false),
    LayerStackItem("grain", "35mm Kodak Halation & Grain", "Film Emulsion", "Overlay", 0.65f, true),
    LayerStackItem("ai", "AI 3D Portrait Key-Light", "Neural Depth Mask", "Soft Light", 0.85f, true),
    LayerStackItem("curves", "RGB Master Spline Curve", "Tone Mapping", "Luminosity", 1f, true),
    LayerStackItem("grade", "8-Band HSL & Color Matrix", "16-Bit Color", "Normal", 1f, true),
    LayerStackItem("raw", "Camera DNG Base Sensor", "6000 × 4000 RAW", "Pass Through", 1f, true)
)

val SAMPLE_PROJECTS = listOf(
    RawProjectSample(
        id = "golden_portrait",
        title = "Golden_Portrait_85mm.DNG",
        formatBadge = "16-BIT DNG",
        cameraSpec = "SONY A7R V · 24MP",
        lensExif = "85mm f/1.4 GM · ISO 100 · 1/320s",
        editedAgo = "Edited 2m ago",
        steps = 12,
        sceneIndex = 0
    ),
    RawProjectSample(
        id = "tokyo_neon",
        title = "Shinjuku_Rain_Neon.ARW",
        formatBadge = "SONY ARW",
        cameraSpec = "SONY A7 IV · 33MP",
        lensExif = "35mm f/1.4 Art · ISO 800 · 1/125s",
        editedAgo = "Edited 18m ago",
        steps = 19,
        sceneIndex = 1
    ),
    RawProjectSample(
        id = "alpine_lake",
        title = "Dolomites_Alpenglow.CR3",
        formatBadge = "CANON CR3",
        cameraSpec = "EOS R5 · 45MP",
        lensExif = "16mm f/2.8L · ISO 64 · 1/60s",
        editedAgo = "Edited 1h ago",
        steps = 8,
        sceneIndex = 2
    ),
    RawProjectSample(
        id = "street_mono",
        title = "Old_Dhaka_Morning.RAF",
        formatBadge = "FUJI RAF",
        cameraSpec = "GFX 100 II · 102MP",
        lensExif = "55mm f/1.7 · ISO 160 · 1/500s",
        editedAgo = "Edited 4h ago",
        steps = 15,
        sceneIndex = 3
    )
)

val STUDIO_LUTS = listOf(
    LutPreset("raw_zero", "RAW Zero", "All", null, Color(0xFF9CA3AF)),
    LutPreset("portra_400", "Portra 400", "Portrait", "PRO", Color(0xFFF59E0B), exposure = 10f, contrast = 14f, warmth = 18f, saturation = -4f, fade = 12f),
    LutPreset("teal_amber", "Teal & Amber", "Cinema", "LUT", Color(0xFF06B6D4), exposure = 4f, contrast = 28f, warmth = -8f, saturation = 16f, vignette = 26f),
    LutPreset("cyber_neon", "Tokyo Cyber", "Cyber", "AI", Color(0xFFEC4899), exposure = 6f, contrast = 32f, warmth = -24f, saturation = 30f, vignette = 30f),
    LutPreset("golden_glow", "Golden Hour", "Portrait", null, Color(0xFFFBBF24), exposure = 14f, contrast = 12f, warmth = 32f, saturation = 18f, vignette = 14f),
    LutPreset("leica_mono", "Leica Mono", "B&W", "RAW", Color(0xFFE5E7EB), exposure = 6f, contrast = 42f, warmth = 0f, saturation = -100f, vignette = 32f),
    LutPreset("nordic_matte", "Nordic Matte", "Cinema", null, Color(0xFF60A5FA), exposure = -4f, contrast = 8f, warmth = -16f, saturation = -20f, fade = 28f)
)
