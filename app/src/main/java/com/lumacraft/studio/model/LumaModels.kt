package com.lumacraft.studio.model

import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class Screen { HOME, EDITOR, COLLAGE, BATCH, PRESETS, SETTINGS }

// ---------------------------------------------------------------- adjustments

/** Every field here is a REAL pixel operation executed by ImageEditEngine. */
data class Adjustments(
    val exposure: Float = 0f,        // -100..100 (stops gain)
    val brightness: Float = 0f,      // -100..100 (additive)
    val contrast: Float = 0f,        // -100..100
    val highlights: Float = 0f,      // -100..100
    val shadows: Float = 0f,         // -100..100
    val whites: Float = 0f,          // -100..100
    val blacks: Float = 0f,          // -100..100
    val saturation: Float = 0f,      // -100..100
    val vibrance: Float = 0f,        // -100..100
    val temperature: Float = 0f,     // -100..100 (warm/cool)
    val tint: Float = 0f,            // -100..100 (green/magenta)
    val hueShift: Float = 0f,        // -180..180
    val sharpness: Float = 0f,       // 0..100 (unsharp mask)
    val clarity: Float = 0f,         // -100..100 (local contrast)
    val texture: Float = 0f,         // -100..100 (fine detail)
    val dehaze: Float = 0f,          // 0..100
    val fade: Float = 0f,            // 0..100 (matte lift)
    val vignette: Float = 0f,        // 0..100
    val grain: Float = 0f,           // 0..100
    val noiseReduction: Float = 0f   // 0..100 (edge-aware smoothing)
) {
    val hasAny: Boolean
        get() = exposure != 0f || brightness != 0f || contrast != 0f || highlights != 0f ||
            shadows != 0f || whites != 0f || blacks != 0f || saturation != 0f || vibrance != 0f ||
            temperature != 0f || tint != 0f || hueShift != 0f || sharpness != 0f || clarity != 0f ||
            texture != 0f || dehaze != 0f || fade != 0f || vignette != 0f || grain != 0f ||
            noiseReduction != 0f
}

data class SplitTone(
    val shadowHue: Float = 0f,       // 0..360
    val shadowSat: Float = 0f,       // 0..100
    val highlightHue: Float = 0f,
    val highlightSat: Float = 0f,
    val balance: Float = 0f          // -100..100
)

enum class CurveChannel(val label: String, val stroke: Color) {
    MASTER("Master", Color(0xFFEAF0FF)),
    RED("Red", Color(0xFFF76E64)),
    GREEN("Green", Color(0xFF6EDC9C)),
    BLUE("Blue", Color(0xFF6E9BF7))
}

data class HslZone(
    val id: String,
    val label: String,
    val swatch: Color,
    val centerHue: Float,
    val hue: Float = 0f,
    val sat: Float = 0f,
    val lum: Float = 0f
)

data class WheelGrade(val x: Float = 0f, val y: Float = 0f) // -1..1 pad offsets

fun defaultHslZones() = listOf(
    HslZone("red", "Red", Color(0xFFF76E64), 0f),
    HslZone("orange", "Skin", Color(0xFFF7A15C), 28f),
    HslZone("yellow", "Yellow", Color(0xFFEFD75C), 58f),
    HslZone("green", "Green", Color(0xFF6EDC9C), 125f),
    HslZone("aqua", "Aqua", Color(0xFF5CD6EF), 185f),
    HslZone("blue", "Blue", Color(0xFF6E9BF7), 230f),
    HslZone("purple", "Purple", Color(0xFFA97BF7), 280f),
    HslZone("magenta", "Magenta", Color(0xFFF76EBE), 330f)
)

// ---------------------------------------------------------------- geometry

data class GeoState(
    val aspect: String = "Original",
    val straighten: Float = 0f,      // -45..45
    val keystoneV: Float = 0f,       // -100..100 architectural vertical
    val keystoneH: Float = 0f,       // -100..100
    val quarterTurns: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false
)

// ---------------------------------------------------------------- retouch & brush

data class HealSpot(val x: Float, val y: Float, val r: Float = 0.045f)

/** Red-eye / teeth-whitening tap fixes. */
data class TapFix(val x: Float, val y: Float, val r: Float = 0.05f)

enum class BrushMode(val label: String) { BLUR("Blur Brush"), PIXELATE("Pixelate"), REMOVE("Object Eraser") }

data class BrushStroke(
    val mode: BrushMode,
    val radius: Float,               // normalized 0.02..0.15
    val points: List<Pair<Float, Float>>
)

/** Painted selective-adjustment mask stroke (Lightroom-style mask brush). */
data class MaskPaintStroke(
    val points: List<Pair<Float, Float>>,
    val radius: Float,
    val erase: Boolean
)

// ---------------------------------------------------------------- overlays

data class TextLayer(
    val text: String,
    val fontIdx: Int = 0,            // index into AVAILABLE_FONTS
    val sizeRel: Float = 0.07f,      // relative to bitmap width
    val color: Long = 0xFFFFFFFF,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val centered: Boolean = true,
    val x: Float = 0.5f,
    val y: Float = 0.85f,
    val opacity: Float = 1f
)

data class EmojiLayer(val emoji: String, val sizeRel: Float = 0.14f, val x: Float = 0.5f, val y: Float = 0.3f)

data class DrawStroke(val color: Long, val widthRel: Float = 0.008f, val points: List<Pair<Float, Float>>)

enum class FrameStyle(val label: String) { NONE("None"), WHITE("White Border"), DARK("Dark Border"), POLAROID("Polaroid") }

// ---------------------------------------------------------------- AI tools

data class AiState(
    val bgBlur: Float = 0f,          // 0..100 portrait lens blur
    val subjectPop: Float = 0f,      // -50..50 subject brightness lift
    val bgDim: Float = 0f,           // 0..100 background darken
    val cutout: Boolean = false,     // transparent PNG cutout
    val bgColor: Long? = null,       // solid background replacement
    val showMask: Boolean = false,   // subject mask overlay
    val maskInvert: Boolean = false  // select background instead of subject
)

// ---------------------------------------------------------------- master edit state

data class EditState(
    val adjust: Adjustments = Adjustments(),
    val split: SplitTone = SplitTone(),
    val curves: Map<CurveChannel, List<Float>> = mapOf(
        CurveChannel.MASTER to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannel.RED to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannel.GREEN to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannel.BLUE to listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    ),
    val hsl: List<HslZone> = defaultHslZones(),
    val shadowWheel: WheelGrade = WheelGrade(),
    val midWheel: WheelGrade = WheelGrade(),
    val highWheel: WheelGrade = WheelGrade(),
    val filterId: String? = null,
    val filterIntensity: Float = 100f,
    val geo: GeoState = GeoState(),
    val skinSmooth: Float = 0f,      // 0..100
    val naturalEnhance: Float = 0f,  // 0..100
    val healSpots: List<HealSpot> = emptyList(),
    val redEyeSpots: List<TapFix> = emptyList(),
    val teethSpots: List<TapFix> = emptyList(),
    val brushStrokes: List<BrushStroke> = emptyList(),
    val ai: AiState = AiState(),
    val textLayers: List<TextLayer> = emptyList(),
    val emojiLayers: List<EmojiLayer> = emptyList(),
    val drawStrokes: List<DrawStroke> = emptyList(),
    // imported .cube LUT (full text stored; parsed+cached by the engine)
    val lutName: String? = null,
    val lutText: String? = null,
    val lutIntensity: Float = 100f,
    // painted selective mask + mask-linked adjustments
    val maskStrokes: List<MaskPaintStroke> = emptyList(),
    val maskExposure: Float = 0f,
    val maskTemp: Float = 0f,
    val maskSat: Float = 0f,
    val maskClarity: Float = 0f,
    val showMaskOverlay: Boolean = false,
    val frame: FrameStyle = FrameStyle.NONE,
    val watermark: String = ""
)

// ---------------------------------------------------------------- filters (40 originals, 10 categories)

data class FilterDef(
    val id: String,
    val name: String,
    val category: String,
    val accent: Color,
    val adjust: Adjustments,
    val split: SplitTone? = null,
    val masterCurve: List<Float>? = null
)

private fun f(
    exp: Float = 0f, bri: Float = 0f, con: Float = 0f, hi: Float = 0f, sh: Float = 0f,
    wh: Float = 0f, bl: Float = 0f, sat: Float = 0f, vib: Float = 0f, tem: Float = 0f,
    tin: Float = 0f, fade: Float = 0f, gra: Float = 0f, vig: Float = 0f, deh: Float = 0f,
    sha: Float = 0f
) = Adjustments(
    exposure = exp, brightness = bri, contrast = con, highlights = hi, shadows = sh,
    whites = wh, blacks = bl, saturation = sat, vibrance = vib, temperature = tem,
    tint = tin, fade = fade, grain = gra, vignette = vig, dehaze = deh, sharpness = sha
)

val FILTER_CATEGORIES = listOf(
    "Cinematic", "Portrait", "Vintage", "Film", "B&W",
    "Landscape", "Moody", "Warm", "Cool", "Social"
)

val ALL_FILTERS: List<FilterDef> = listOf(
    // Cinematic
    FilterDef("teal_ember", "Teal & Ember", "Cinematic", Color(0xFF2EC4B6), f(con = 24f, hi = -18f, sh = 14f, tem = -14f, sat = 12f, vig = 18f, gra = 10f, deh = 10f), SplitTone(195f, 22f, 30f, 18f, 0f)),
    FilterDef("noir_cinema", "Noir Cinema", "Cinematic", Color(0xFF8E9AAF), f(con = 42f, hi = -22f, sh = -8f, sat = -78f, gra = 20f, vig = 22f), SplitTone(210f, 8f, 0f, 0f, -20f), listOf(0.01f, 0.18f, 0.5f, 0.84f, 0.99f)),
    FilterDef("blockbuster", "Blockbuster", "Cinematic", Color(0xFFFFB703), f(con = 30f, hi = -16f, sh = 10f, tem = 10f, sat = 20f, deh = 14f, vig = 18f), SplitTone(215f, 18f, 42f, 16f, 10f)),
    FilterDef("neo_seoul", "Neo Seoul", "Cinematic", Color(0xFFC77DFF), f(con = 26f, hi = -20f, sh = 18f, tem = -22f, tin = 8f, sat = 16f, gra = 8f), SplitTone(265f, 26f, 190f, 14f, 0f)),
    // Portrait
    FilterDef("soft_aura", "Soft Aura", "Portrait", Color(0xFFFFC8DD), f(exp = 8f, con = -6f, hi = -12f, sh = 22f, sat = 4f, fade = 14f, tem = 8f), SplitTone(30f, 12f, 0f, 0f, 0f)),
    FilterDef("porcelain", "Porcelain", "Portrait", Color(0xFFEAF0FF), f(exp = 12f, con = 6f, hi = -10f, sh = 16f, sat = -12f, tem = -6f, fade = 10f), null),
    FilterDef("golden_skin", "Golden Skin", "Portrait", Color(0xFFFFB703), f(exp = 6f, con = 12f, hi = -14f, sh = 16f, tem = 20f, sat = 8f, vib = 14f), SplitTone(36f, 16f, 46f, 12f, 0f)),
    FilterDef("editorial", "Editorial", "Portrait", Color(0xFF33415C), f(con = 20f, hi = -18f, sh = 12f, sat = -8f, gra = 6f), SplitTone(0f, 0f, 210f, 10f, 20f)),
    // Vintage
    FilterDef("retro64", "Retro 64", "Vintage", Color(0xFFE07A5F), f(con = 18f, hi = -20f, sh = 20f, tem = 14f, sat = -18f, fade = 22f, gra = 24f, vig = 14f), SplitTone(40f, 14f, 0f, 0f, 0f), listOf(0.05f, 0.27f, 0.52f, 0.76f, 0.95f)),
    FilterDef("sepia_vault", "Sepia Vault", "Vintage", Color(0xFFB08968), f(con = 14f, sat = -82f, tem = 30f, fade = 18f, gra = 18f), SplitTone(38f, 26f, 40f, 14f, 0f)),
    FilterDef("faded_print", "Faded Print", "Vintage", Color(0xFFA5A58D), f(con = -8f, hi = -18f, sh = 24f, sat = -22f, fade = 30f, gra = 14f), SplitTone(90f, 10f, 50f, 8f, 0f)),
    FilterDef("sunset72", "Sunset '72", "Vintage", Color(0xFFFF8FAB), f(con = 16f, hi = -16f, sh = 18f, tem = 24f, sat = 6f, fade = 20f, gra = 16f), SplitTone(320f, 16f, 35f, 18f, 0f)),
    // Film
    FilterDef("analog400", "Analog 400", "Film", Color(0xFF6070FA), f(exp = 4f, con = 16f, hi = -18f, sh = 14f, tem = 6f, sat = 10f, gra = 22f, vib = 10f), SplitTone(220f, 8f, 45f, 10f, 0f)),
    FilterDef("tungsten_night", "Tungsten Night", "Film", Color(0xFF4CC9F0), f(con = 22f, hi = -16f, sh = 16f, tem = -26f, sat = 12f, gra = 14f), SplitTone(240f, 18f, 35f, 8f, 0f)),
    FilterDef("cross_process", "Cross Process", "Film", Color(0xFF80FFDB), f(con = 34f, hi = -20f, sh = -6f, tem = -14f, tin = -14f, sat = 24f, gra = 12f), SplitTone(170f, 22f, 55f, 18f, 0f)),
    FilterDef("halation800", "Halation 800", "Film", Color(0xFFFF5D5D), f(exp = 6f, con = 20f, hi = -14f, sh = 14f, tem = -10f, sat = 14f, gra = 18f), SplitTone(230f, 14f, 8f, 22f, 0f)),
    // B&W
    FilterDef("classic_mono", "Classic Mono", "B&W", Color(0xFFADB5BD), f(sat = -100f, con = 18f, hi = -12f, sh = 12f, gra = 16f), null, listOf(0.02f, 0.24f, 0.5f, 0.78f, 0.98f)),
    FilterDef("high_key_silver", "High Key Silver", "B&W", Color(0xFFDEE2E6), f(exp = 14f, sat = -100f, con = 6f, hi = -8f, sh = 26f, fade = 12f), null),
    FilterDef("hard_noir", "Hard Noir", "B&W", Color(0xFF495057), f(sat = -100f, con = 44f, hi = -20f, sh = -12f, gra = 24f, vig = 20f), null, listOf(0f, 0.15f, 0.48f, 0.85f, 1f)),
    FilterDef("cold_steel_mono", "Cold Steel Mono", "B&W", Color(0xFF90A2B8), f(sat = -100f, con = 24f, tem = 0f, gra = 12f), SplitTone(210f, 12f, 200f, 6f, 0f)),
    // Landscape
    FilterDef("verdant_valley", "Verdant Valley", "Landscape", Color(0xFF52B788), f(con = 20f, hi = -16f, sh = 14f, sat = 18f, vib = 22f, deh = 16f), null),
    FilterDef("alpine_clarity", "Alpine Clarity", "Landscape", Color(0xFF8ECae6), f(con = 26f, hi = -18f, sh = 10f, sat = 12f, deh = 28f, sha = 24f), null),
    FilterDef("desert_warm", "Desert Warm", "Landscape", Color(0xFFE8A87C), f(con = 16f, hi = -14f, sh = 16f, tem = 22f, sat = 14f, vib = 12f), SplitTone(38f, 14f, 0f, 0f, 0f)),
    FilterDef("coastal_blue", "Coastal Blue", "Landscape", Color(0xFF48CAE4), f(con = 18f, hi = -14f, sh = 18f, tem = -18f, sat = 14f, vib = 16f), SplitTone(200f, 16f, 0f, 0f, 0f)),
    // Moody
    FilterDef("deep_fog", "Deep Fog", "Moody", Color(0xFF6C757D), f(exp = -6f, con = -6f, hi = -16f, sh = 20f, sat = -26f, fade = 26f), SplitTone(200f, 10f, 0f, 0f, 0f)),
    FilterDef("rainy_day", "Rainy Day", "Moody", Color(0xFF5C6B73), f(exp = -4f, con = 10f, hi = -14f, sh = 12f, tem = -16f, sat = -14f, gra = 12f), SplitTone(215f, 14f, 0f, 0f, -10f)),
    FilterDef("twilight_blue", "Twilight Blue", "Moody", Color(0xFF4361EE), f(exp = -8f, con = 22f, hi = -20f, sh = 8f, tem = -24f, sat = 10f, vig = 14f), SplitTone(250f, 20f, 190f, 10f, 0f)),
    FilterDef("charcoal_mood", "Charcoal Mood", "Moody", Color(0xFF343A40), f(exp = -10f, con = 28f, hi = -18f, sh = -6f, sat = -34f, vig = 12f), SplitTone(0f, 0f, 210f, 8f, 0f)),
    // Warm
    FilterDef("amber_hour", "Amber Hour", "Warm", Color(0xFFFFB627), f(exp = 8f, con = 12f, hi = -14f, sh = 18f, tem = 28f, sat = 10f, fade = 10f), SplitTone(40f, 18f, 35f, 12f, 0f)),
    FilterDef("havana_sun", "Havana Sun", "Warm", Color(0xFFF4A261), f(con = 18f, hi = -16f, sh = 16f, tem = 24f, sat = 16f, gra = 10f), SplitTone(30f, 14f, 50f, 14f, 0f)),
    FilterDef("golden_field", "Golden Field", "Warm", Color(0xFFFFD166), f(exp = 6f, con = 14f, hi = -12f, sh = 20f, tem = 18f, sat = 14f, vib = 14f), null),
    FilterDef("peach_pop", "Peach Pop", "Warm", Color(0xFFFFB4A2), f(exp = 8f, con = 10f, hi = -10f, sh = 16f, tem = 14f, sat = 12f, fade = 14f), SplitTone(20f, 12f, 25f, 10f, 0f)),
    // Cool
    FilterDef("arctic", "Arctic", "Cool", Color(0xFFB9F3FF), f(exp = 6f, con = 14f, hi = -12f, sh = 16f, tem = -30f, sat = 8f), SplitTone(195f, 18f, 190f, 10f, 0f)),
    FilterDef("blue_hour", "Blue Hour", "Cool", Color(0xFF64A6FF), f(con = 20f, hi = -16f, sh = 14f, tem = -26f, sat = 12f, vib = 14f), SplitTone(225f, 20f, 0f, 0f, 0f)),
    FilterDef("steel_blue", "Steel Blue", "Cool", Color(0xFF6B9080), f(con = 18f, hi = -14f, sh = 14f, tem = -18f, tin = -8f, sat = -6f, deh = 12f), null),
    FilterDef("ice_mint", "Ice Mint", "Cool", Color(0xFF95D5B2), f(exp = 4f, con = 12f, hi = -12f, sh = 18f, tem = -14f, tin = -10f, sat = 10f, fade = 10f), SplitTone(160f, 14f, 180f, 8f, 0f)),
    // Social
    FilterDef("glossy_pop", "Glossy Pop", "Social", Color(0xFFFF70A6), f(exp = 8f, con = 18f, hi = -12f, sh = 14f, sat = 20f, vib = 20f), SplitTone(330f, 12f, 40f, 10f, 0f)),
    FilterDef("crisp_bright", "Crisp Bright", "Social", Color(0xFFFFF3B0), f(exp = 14f, con = 14f, hi = -10f, sh = 18f, sat = 12f, deh = 10f), null),
    FilterDef("golden_selfie", "Golden Selfie", "Social", Color(0xFFFFC971), f(exp = 10f, con = 10f, hi = -12f, sh = 20f, tem = 22f, sat = 10f, fade = 8f), null),
    FilterDef("urban_vibrant", "Urban Vibrant", "Social", Color(0xFFEF6461), f(con = 24f, hi = -16f, sh = 12f, sat = 22f, vib = 18f, deh = 12f, gra = 8f), SplitTone(210f, 12f, 30f, 14f, 0f))
)

// ---------------------------------------------------------------- export & presets

enum class ExportFormat(val label: String, val ext: String, val mime: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp")
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 92,               // 1..100
    val resolutionMode: String = "Original", // Original / 2048 px / 1080 px / Square 1080
    val stripExif: Boolean = true,       // privacy scrubber on by default
    val shareAfter: Boolean = false,
    val upscale2x: Boolean = false       // 2x software upscale (bicubic + smart sharpen)
)

data class PresetItem(
    val id: String,
    val name: String,
    val createdAt: Long,
    val state: EditState
)

data class RecentItem(
    val uri: String,
    val name: String,
    val timestamp: Long,
    val hasSession: Boolean
)

data class AppSettings(
    val themeMode: String = "System",    // System / Light / Dark
    val haptics: Boolean = true,
    val defaultFormat: ExportFormat = ExportFormat.JPEG,
    val defaultQuality: Int = 92,
    val stripExifDefault: Boolean = true,
    val watermarkDefault: String = ""
)

// ---------------------------------------------------------------- collage

data class CollageTemplate(val id: Int, val name: String, val cells: List<RectF>)

private fun r(l: Float, t: Float, rr: Float, b: Float) = RectF(l, t, rr, b)

val COLLAGE_TEMPLATES: List<CollageTemplate> = listOf(
    CollageTemplate(1, "Side by Side", listOf(r(0f, 0f, 0.5f, 1f), r(0.5f, 0f, 1f, 1f))),
    CollageTemplate(2, "Wide Split", listOf(r(0f, 0f, 0.42f, 1f), r(0.42f, 0f, 1f, 1f))),
    CollageTemplate(3, "Stack", listOf(r(0f, 0f, 1f, 0.5f), r(0f, 0.5f, 1f, 1f))),
    CollageTemplate(4, "Tall Split", listOf(r(0f, 0f, 1f, 0.58f), r(0f, 0.58f, 1f, 1f))),
    CollageTemplate(5, "Feature Left", listOf(r(0f, 0f, 0.6f, 1f), r(0.6f, 0f, 1f, 0.5f), r(0.6f, 0.5f, 1f, 1f))),
    CollageTemplate(6, "Feature Right", listOf(r(0f, 0f, 0.4f, 0.5f), r(0f, 0.5f, 0.4f, 1f), r(0.4f, 0f, 1f, 1f))),
    CollageTemplate(7, "Banner Top", listOf(r(0f, 0f, 1f, 0.45f), r(0f, 0.45f, 0.5f, 1f), r(0.5f, 0.45f, 1f, 1f))),
    CollageTemplate(8, "Banner Bottom", listOf(r(0f, 0f, 0.5f, 0.55f), r(0.5f, 0f, 1f, 0.55f), r(0f, 0.55f, 1f, 1f))),
    CollageTemplate(9, "Grid 2x2", listOf(r(0f, 0f, 0.5f, 0.5f), r(0.5f, 0f, 1f, 0.5f), r(0f, 0.5f, 0.5f, 1f), r(0.5f, 0.5f, 1f, 1f))),
    CollageTemplate(10, "Big Left 3", listOf(r(0f, 0f, 0.62f, 1f), r(0.62f, 0f, 1f, 1f / 3f), r(0.62f, 1f / 3f, 1f, 2f / 3f), r(0.62f, 2f / 3f, 1f, 1f))),
    CollageTemplate(11, "Big Right 3", listOf(r(0f, 0f, 0.38f, 1f / 3f), r(0f, 1f / 3f, 0.38f, 2f / 3f), r(0f, 2f / 3f, 0.38f, 1f), r(0.38f, 0f, 1f, 1f))),
    CollageTemplate(12, "Top Wide 3", listOf(r(0f, 0f, 1f, 0.5f), r(0f, 0.5f, 1f / 3f, 1f), r(1f / 3f, 0.5f, 2f / 3f, 1f), r(2f / 3f, 0.5f, 1f, 1f))),
    CollageTemplate(13, "Bottom Wide 3", listOf(r(0f, 0f, 1f / 3f, 0.5f), r(1f / 3f, 0f, 2f / 3f, 0.5f), r(2f / 3f, 0f, 1f, 0.5f), r(0f, 0.5f, 1f, 1f))),
    CollageTemplate(14, "Five Mix", listOf(r(0f, 0f, 0.55f, 0.5f), r(0.55f, 0f, 1f, 0.5f), r(0f, 0.5f, 1f / 3f, 1f), r(1f / 3f, 0.5f, 2f / 3f, 1f), r(2f / 3f, 0.5f, 1f, 1f))),
    CollageTemplate(15, "Big Left 4", listOf(r(0f, 0f, 0.6f, 1f), r(0.6f, 0f, 1f, 0.25f), r(0.6f, 0.25f, 1f, 0.5f), r(0.6f, 0.5f, 1f, 0.75f), r(0.6f, 0.75f, 1f, 1f))),
    CollageTemplate(16, "Big Top 4", listOf(r(0f, 0f, 1f, 0.6f), r(0f, 0.6f, 0.25f, 1f), r(0.25f, 0.6f, 0.5f, 1f), r(0.5f, 0.6f, 0.75f, 1f), r(0.75f, 0.6f, 1f, 1f))),
    CollageTemplate(17, "Grid 2x3", listOf(r(0f, 0f, 0.5f, 1f / 3f), r(0.5f, 0f, 1f, 1f / 3f), r(0f, 1f / 3f, 0.5f, 2f / 3f), r(0.5f, 1f / 3f, 1f, 2f / 3f), r(0f, 2f / 3f, 0.5f, 1f), r(0.5f, 2f / 3f, 1f, 1f))),
    CollageTemplate(18, "Grid 3x2", listOf(r(0f, 0f, 1f / 3f, 0.5f), r(1f / 3f, 0f, 2f / 3f, 0.5f), r(2f / 3f, 0f, 1f, 0.5f), r(0f, 0.5f, 1f / 3f, 1f), r(1f / 3f, 0.5f, 2f / 3f, 1f), r(2f / 3f, 0.5f, 1f, 1f))),
    CollageTemplate(19, "Big Left 5", listOf(r(0f, 0f, 0.62f, 1f), r(0.62f, 0f, 1f, 0.2f), r(0.62f, 0.2f, 1f, 0.4f), r(0.62f, 0.4f, 1f, 0.6f), r(0.62f, 0.6f, 1f, 0.8f), r(0.62f, 0.8f, 1f, 1f))),
    CollageTemplate(20, "Strip 4", listOf(r(0f, 0f, 0.25f, 1f), r(0.25f, 0f, 0.5f, 1f), r(0.5f, 0f, 0.75f, 1f), r(0.75f, 0f, 1f, 1f))),
    CollageTemplate(21, "Strip Rows 4", listOf(r(0f, 0f, 1f, 0.25f), r(0f, 0.25f, 1f, 0.5f), r(0f, 0.5f, 1f, 0.75f), r(0f, 0.75f, 1f, 1f))),
    CollageTemplate(22, "Center Stage", listOf(r(0.14f, 0.14f, 0.86f, 0.86f), r(0f, 0f, 0.14f, 0.14f), r(0.86f, 0f, 1f, 0.14f), r(0f, 0.86f, 0.14f, 1f), r(0.86f, 0.86f, 1f, 1f), r(0.14f, 0f, 0.86f, 0.14f), r(0.14f, 0.86f, 0.86f, 1f), r(0f, 0.14f, 0.14f, 0.86f), r(0.86f, 0.14f, 1f, 0.86f))),
    CollageTemplate(23, "Film Strip 6", listOf(r(0f, 0f, 1f, 1f / 6f), r(0f, 1f / 6f, 1f, 2f / 6f), r(0f, 2f / 6f, 1f, 3f / 6f), r(0f, 3f / 6f, 1f, 4f / 6f), r(0f, 4f / 6f, 1f, 5f / 6f), r(0f, 5f / 6f, 1f, 1f))),
    CollageTemplate(24, "Grid 3x3", listOf(r(0f, 0f, 1f / 3f, 1f / 3f), r(1f / 3f, 0f, 2f / 3f, 1f / 3f), r(2f / 3f, 0f, 1f, 1f / 3f), r(0f, 1f / 3f, 1f / 3f, 2f / 3f), r(1f / 3f, 1f / 3f, 2f / 3f, 2f / 3f), r(2f / 3f, 1f / 3f, 1f, 2f / 3f), r(0f, 2f / 3f, 1f / 3f, 1f), r(1f / 3f, 2f / 3f, 2f / 3f, 1f), r(2f / 3f, 2f / 3f, 1f, 1f)))
)

val COLLAGE_BG_COLORS = listOf(0xFFFFFFFF, 0xFF0E0F12, 0xFFF4F1EA, 0xFFD9D2C0, 0xFF1B2432, 0xFF3A2E39)
val COLLAGE_ASPECTS = listOf("1:1", "4:5", "3:4", "2:3", "4:3", "3:2", "16:9")

data class CollageConfig(
    val photos: List<Uri> = emptyList(),
    val templateIdx: Int = 8,
    val spacingRel: Float = 0.02f,
    val cornerRel: Float = 0.0f,
    val bgColor: Long = 0xFF0E0F12,
    val aspect: String = "1:1",
    val cellOffsets: Map<Int, Pair<Float, Float>> = emptyMap(), // per-cell pan (norm)
    val caption: String = ""
)

// ---------------------------------------------------------------- batch

data class BatchConfig(
    val uris: List<Uri> = emptyList(),
    val applyCurrentLook: Boolean = true,
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 90,
    val maxDim: Int = 2048,
    val prefix: String = "LumaCraft_Batch"
)

// ---------------------------------------------------------------- built-in pro presets

val BUILT_IN_PRESETS: List<Pair<String, EditState>> = listOf(
    "Studio Portrait" to EditState(
        adjust = Adjustments(exposure = 8f, contrast = 8f, highlights = -12f, shadows = 18f, temperature = 6f, vibrance = 10f),
        skinSmooth = 34f, naturalEnhance = 20f
    ),
    "Street Chrome" to EditState(
        adjust = Adjustments(contrast = 22f, highlights = -16f, shadows = 10f, saturation = 12f, clarity = 16f, dehaze = 8f, grain = 12f),
        split = SplitTone(215f, 10f, 45f, 10f)
    ),
    "Golden Hour" to EditState(
        adjust = Adjustments(exposure = 8f, contrast = 14f, highlights = -14f, shadows = 20f, temperature = 26f, vibrance = 14f, fade = 10f),
        split = SplitTone(40f, 16f, 35f, 12f)
    ),
    "Moody Film" to EditState(
        adjust = Adjustments(exposure = -6f, contrast = 20f, highlights = -18f, shadows = 8f, temperature = -14f, saturation = -12f, fade = 18f, grain = 14f),
        split = SplitTone(230f, 16f, 0f, 0f)
    ),
    "Clean Bright" to EditState(
        adjust = Adjustments(exposure = 14f, contrast = 10f, highlights = -8f, shadows = 16f, saturation = 8f, sharpness = 18f)
    ),
    "Cinematic Wide" to EditState(
        adjust = Adjustments(contrast = 26f, highlights = -18f, shadows = 14f, temperature = -10f, saturation = 14f, dehaze = 12f, vignette = 18f),
        split = SplitTone(205f, 18f, 35f, 14f)
    ),
    "Analog Fade" to EditState(
        adjust = Adjustments(contrast = 8f, highlights = -18f, shadows = 22f, saturation = -14f, fade = 26f, grain = 20f),
        curves = mapOf(
            CurveChannel.MASTER to listOf(0.06f, 0.28f, 0.52f, 0.75f, 0.94f),
            CurveChannel.RED to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            CurveChannel.GREEN to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            CurveChannel.BLUE to listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        )
    ),
    "Mono Editorial" to EditState(
        adjust = Adjustments(saturation = -100f, contrast = 26f, highlights = -14f, shadows = 14f, clarity = 18f, grain = 12f),
        curves = mapOf(
            CurveChannel.MASTER to listOf(0.02f, 0.22f, 0.5f, 0.8f, 0.98f),
            CurveChannel.RED to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            CurveChannel.GREEN to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            CurveChannel.BLUE to listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        )
    )
)

val AVAILABLE_FONTS = listOf(
    "Sans" to android.graphics.Typeface.DEFAULT,
    "Sans Bold" to android.graphics.Typeface.DEFAULT_BOLD,
    "Serif" to android.graphics.Typeface.SERIF,
    "Serif Bold" to android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD),
    "Mono" to android.graphics.Typeface.MONOSPACE,
    "Condensed" to android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL),
    "Cursive" to android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
)

val EMOJI_PALETTE = listOf("✨", "🌟", "❤️", "🔥", "🌸", "🌈", "☀️", "🌙", "💫", "🎈", "🍃", "🦋", "☕", "🎧", "📷", "🗺️")
