package studio.velox.atelier.model

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class AtelierScreen {
    ATELIER_HOME,
    DARKROOM
}

enum class AtelierHomeSection(val label: String) {
    ATELIER("Atelier"),
    CONTACT_SHEET("Rolls"),
    RECIPES("Stocks"),
    OPTICS_LAB("Pipeline")
}

enum class DarkroomModule(val label: String, val code: String) {
    FILM("Stocks", "FLM"),
    LIGHT("Light", "EXP"),
    CURVES("Curves", "CRV"),
    WHEELS("Wheels", "3WY"),
    HSL("Color", "HSL"),
    ANALOG("Halation", "HAL"),
    MASK("Mask", "MSK"),
    CROP("Geometry", "GEO"),
    PRINT("Print", "PRT")
}

enum class CurveChannelId(val title: String, val strokeColor: Color) {
    LUMA("Master", Color(0xFFF5F5F7)),
    RED("Red", Color(0xFFFF5A52)),
    GREEN("Green", Color(0xFF34D399)),
    BLUE("Blue", Color(0xFF60A5FA))
}

enum class MaskType(val label: String) {
    NONE("Off"),
    RADIAL_SPOT("Radial Spot"),
    SKY_GRADIENT("Linear Sky"),
    EDGE_VIGNETTE("Subject Rim")
}

enum class PrintBorderStyle(val label: String) {
    NONE("Borderless"),
    MUSEUM_WHITE("Museum Matte"),
    HASSELBLAD_DARK("Obsidian Rebate"),
    CONTACT_35MM("35mm Negative")
}

enum class ExportFormat(val label: String, val ext: String, val mime: String) {
    JPEG("JPEG Pro", "jpg", "image/jpeg"),
    PNG("PNG 16-bpc", "png", "image/png"),
    WEBP("WebP Lossless", "webp", "image/webp")
}

data class WheelGrade(
    val offsetX: Float = 0f, // -1f..1f on chromatic wheel
    val offsetY: Float = 0f, // -1f..1f on chromatic wheel
    val lumaTrim: Float = 0f // -50f..50f
)

data class HslZone(
    val id: String,
    val label: String,
    val color: Color,
    val centerHue: Float,
    val hue: Float = 0f,
    val sat: Float = 0f,
    val lum: Float = 0f
)

data class PhotoExifInfo(
    val cameraModel: String = "LEICA SL3 · FULL FRAME",
    val lensName: String = "APO-SUMMICRON 50mm f/2",
    val exposureSummary: String = "ISO 100 · 50mm · f/2.0 · 1/500s",
    val dimensions: String = "6000 × 4000 RAW"
)

data class ContactRollItem(
    val id: String,
    val frameCode: String,
    val title: String,
    val stockBadge: String,
    val ratingStars: Int,
    val flagColor: Color,
    val exif: PhotoExifInfo,
    val sceneKind: Int
)

data class FilmStockRecipe(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val accent: Color,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val temperature: Float = 0f,
    val vibrance: Float = 0f,
    val saturation: Float = 0f,
    val halation: Float = 0f,
    val bloom: Float = 0f,
    val grain: Float = 0f,
    val matteFade: Float = 0f,
    val shadowWheel: WheelGrade = WheelGrade(),
    val highlightWheel: WheelGrade = WheelGrade(),
    val lumaCurve: List<Float> = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
)

data class RgbScopeData(
    val rBins: FloatArray = FloatArray(48),
    val gBins: FloatArray = FloatArray(48),
    val bBins: FloatArray = FloatArray(48),
    val lumaBins: FloatArray = FloatArray(48),
    val clippedShadows: Boolean = false,
    val clippedHighlights: Boolean = false
)

data class DarkroomState(
    val activeRoll: ContactRollItem = CONTACT_ROLL_SAMPLES.first(),
    val pickedUri: Uri? = null,
    val exif: PhotoExifInfo = CONTACT_ROLL_SAMPLES.first().exif,
    val activeModule: DarkroomModule = DarkroomModule.LIGHT,

    // Film Stock Recipe
    val activeRecipeId: String = "cinestill_800t",
    val recipeIntensity: Float = 85f,

    // Light & Tone
    val activeLightParam: String = "Exposure",
    val exposure: Float = 10f,
    val contrast: Float = 14f,
    val highlights: Float = -20f,
    val shadows: Float = 18f,
    val whites: Float = 6f,
    val blacks: Float = -8f,

    // 4-Channel Spline Tone Curves
    val activeCurveChannel: CurveChannelId = CurveChannelId.LUMA,
    val curves: Map<CurveChannelId, List<Float>> = mapOf(
        CurveChannelId.LUMA to listOf(0.03f, 0.22f, 0.52f, 0.79f, 0.98f),
        CurveChannelId.RED to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannelId.GREEN to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        CurveChannelId.BLUE to listOf(0.02f, 0.26f, 0.5f, 0.74f, 0.97f)
    ),

    // 3-Way Cinema Color Balance Wheels (Capture One 4.0 / Resolve style)
    val shadowWheel: WheelGrade = WheelGrade(offsetX = -0.22f, offsetY = 0.14f, lumaTrim = 0f),
    val midtoneWheel: WheelGrade = WheelGrade(offsetX = 0.04f, offsetY = -0.05f, lumaTrim = 0f),
    val highlightWheel: WheelGrade = WheelGrade(offsetX = 0.24f, offsetY = -0.18f, lumaTrim = 0f),

    // White Balance & 8-Zone HSL Equalizer
    val colorTabIndex: Int = 0, // 0 = WB, 1 = 8-Zone HSL
    val temperature: Float = 8f,
    val tint: Float = 2f,
    val vibrance: Float = 16f,
    val saturation: Float = 4f,
    val selectedHslZone: Int = 1,
    val hslZones: List<HslZone> = DEFAULT_HSL_ZONES,

    // Dehancer / Darkroom 7 Analog Emulsion Engine (Halation, Bloom, Grain, Clarity)
    val activeAnalogParam: String = "Halation",
    val halation: Float = 34f, // Red-orange specular highlight scatter!
    val bloom: Float = 24f,    // Creamy optical highlight glow!
    val grain: Float = 18f,    // 35mm silver halide structure
    val clarity: Float = 14f,
    val matteFade: Float = 8f,
    val vignette: Float = 18f,

    // Selective Masking Studio
    val maskType: MaskType = MaskType.RADIAL_SPOT,
    val maskCenterX: Float = 0.48f,
    val maskCenterY: Float = 0.38f,
    val maskRadius: Float = 0.44f,
    val maskExposure: Float = 22f,
    val maskWarmth: Float = 16f,
    val showRubyMaskOverlay: Boolean = false,

    // Crop & Geometry
    val aspectRatio: String = "Original",
    val straightenDegrees: Float = 0f,
    val quarterTurns: Int = 0,
    val mirrorHorizontal: Boolean = false,

    // Gallery Print & EXIF Rebate
    val printStyle: PrintBorderStyle = PrintBorderStyle.NONE,
    val signatureText: String = "",

    // Viewport HUD Toggles
    val showScope: Boolean = true,
    val showSplitWipe: Boolean = false,
    val splitFraction: Float = 0.50f
)

val DEFAULT_HSL_ZONES = listOf(
    HslZone("crimson", "Crimson", Color(0xFFEF4444), 0f),
    HslZone("amber", "Skin", Color(0xFFF97316), 28f, hue = 2f, sat = 6f, lum = 8f),
    HslZone("gold", "Gold", Color(0xFFEAB308), 58f),
    HslZone("foliage", "Emerald", Color(0xFF22C55E), 125f),
    HslZone("cyan", "Teal", Color(0xFF06B6D4), 185f, hue = -6f, sat = 14f),
    HslZone("cobalt", "Cobalt", Color(0xFF3B82F6), 230f),
    HslZone("iris", "Violet", Color(0xFFA855F7), 280f),
    HslZone("magenta", "Magenta", Color(0xFFEC4899), 330f)
)

val CONTACT_ROLL_SAMPLES = listOf(
    ContactRollItem(
        id = "roll_portrait",
        frameCode = "36A",
        title = "Window_Portrait_50mm.DNG",
        stockBadge = "PORTRA 400",
        ratingStars = 5,
        flagColor = Color(0xFFFF4D2D),
        exif = PhotoExifInfo(
            cameraModel = "LEICA SL3",
            lensName = "SUMMILUX-M 50mm f/1.4 ASPH",
            exposureSummary = "ISO 100 · 50mm · f/1.4 · 1/400s",
            dimensions = "6000 × 4000 DNG"
        ),
        sceneKind = 0
    ),
    ContactRollItem(
        id = "roll_tungsten",
        frameCode = "37A",
        title = "Kowloon_Wet_Tungsten.ARW",
        stockBadge = "800T HALATION",
        ratingStars = 5,
        flagColor = Color(0xFF60A5FA),
        exif = PhotoExifInfo(
            cameraModel = "SONY A7CR",
            lensName = "FE 35mm f/1.4 GM",
            exposureSummary = "ISO 800 · 35mm · f/1.4 · 1/125s",
            dimensions = "7008 × 4672 ARW"
        ),
        sceneKind = 1
    ),
    ContactRollItem(
        id = "roll_alps",
        frameCode = "04B",
        title = "Lofoten_Last_Light.IIQ",
        stockBadge = "PROVIA 100F",
        ratingStars = 4,
        flagColor = Color(0xFF34D399),
        exif = PhotoExifInfo(
            cameraModel = "HASSELBLAD X2D 100C",
            lensName = "XCD 28P f/4.0",
            exposureSummary = "ISO 64 · 28mm · f/8.0 · 1/90s",
            dimensions = "11656 × 8742 16-BIT"
        ),
        sceneKind = 2
    ),
    ContactRollItem(
        id = "roll_silver",
        frameCode = "19C",
        title = "Brutalist_Noon_Shadow.RAF",
        stockBadge = "ILFORD HP5",
        ratingStars = 5,
        flagColor = Color(0xFFE8C58F),
        exif = PhotoExifInfo(
            cameraModel = "FUJIFILM GFX100 II",
            lensName = "GF 55mm f/1.7 R WR",
            exposureSummary = "ISO 400 · 55mm · f/2.8 · 1/1000s",
            dimensions = "11648 × 8736 RAF"
        ),
        sceneKind = 3
    )
)

val FILM_STOCK_RECIPES = listOf(
    FilmStockRecipe(
        id = "raw_linear",
        code = "00",
        name = "Linear Zero",
        category = "Calibrated",
        accent = Color(0xFFA6ACB8)
    ),
    FilmStockRecipe(
        id = "cinestill_800t",
        code = "800T",
        name = "CineStill 800T",
        category = "Tungsten",
        accent = Color(0xFFFF4D2D),
        exposure = 6f,
        contrast = 22f,
        highlights = -18f,
        shadows = 12f,
        temperature = -16f,
        vibrance = 24f,
        halation = 48f,
        bloom = 32f,
        grain = 22f,
        shadowWheel = WheelGrade(-0.28f, 0.16f, 0f),
        highlightWheel = WheelGrade(0.26f, -0.14f, 0f),
        lumaCurve = listOf(0.02f, 0.20f, 0.51f, 0.81f, 0.98f)
    ),
    FilmStockRecipe(
        id = "portra_400",
        code = "P400",
        name = "Portra 400",
        category = "Color Neg",
        accent = Color(0xFFE8C58F),
        exposure = 10f,
        contrast = 10f,
        highlights = -24f,
        shadows = 24f,
        temperature = 14f,
        vibrance = 16f,
        saturation = -6f,
        halation = 18f,
        bloom = 20f,
        grain = 16f,
        matteFade = 12f,
        shadowWheel = WheelGrade(-0.08f, 0.06f, 0f),
        highlightWheel = WheelGrade(0.22f, -0.20f, 0f),
        lumaCurve = listOf(0.05f, 0.24f, 0.52f, 0.78f, 0.96f)
    ),
    FilmStockRecipe(
        id = "kodachrome_64",
        code = "KR64",
        name = "Kodachrome 64",
        category = "Slide",
        accent = Color(0xFFF59E0B),
        exposure = 4f,
        contrast = 28f,
        highlights = -14f,
        shadows = -6f,
        temperature = 18f,
        vibrance = 22f,
        saturation = 10f,
        halation = 14f,
        grain = 14f,
        lumaCurve = listOf(0.01f, 0.18f, 0.51f, 0.83f, 0.99f)
    ),
    FilmStockRecipe(
        id = "provia_100f",
        code = "RDP3",
        name = "Provia 100F",
        category = "Slide",
        accent = Color(0xFF34D399),
        exposure = 4f,
        contrast = 20f,
        highlights = -20f,
        shadows = 14f,
        temperature = -6f,
        vibrance = 30f,
        saturation = 12f,
        bloom = 14f,
        grain = 8f
    ),
    FilmStockRecipe(
        id = "ilford_hp5",
        code = "HP5+",
        name = "Ilford HP5 400",
        category = "Silver B&W",
        accent = Color(0xFFF5F5F7),
        exposure = 6f,
        contrast = 36f,
        highlights = -16f,
        shadows = 8f,
        saturation = -100f,
        vibrance = -100f,
        bloom = 22f,
        grain = 34f,
        matteFade = 6f,
        lumaCurve = listOf(0.02f, 0.16f, 0.50f, 0.84f, 0.99f)
    )
)
