package studio.velox.atelier.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import studio.velox.atelier.model.CurveChannelId
import studio.velox.atelier.model.DarkroomState
import studio.velox.atelier.model.ExportFormat
import studio.velox.atelier.model.MaskType
import studio.velox.atelier.model.PhotoExifInfo
import studio.velox.atelier.model.PrintBorderStyle
import studio.velox.atelier.model.RgbScopeData
import studio.velox.atelier.model.WheelGrade
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LoadedSourcePhoto(
    val bitmap: Bitmap,
    val exif: PhotoExifInfo
)

data class GradedFrameResult(
    val gradedBitmap: Bitmap,
    val rawAlignedBitmap: Bitmap,
    val scope: RgbScopeData
)

object BitmapGradeEngine {

    private fun clamp255(v: Float): Int {
        val i = v.toInt()
        return if (i < 0) 0 else if (i > 255) 255 else i
    }

    fun buildSplineLut(points: List<Float>): IntArray {
        val lut = IntArray(256)
        val pts = if (points.size == 5) points else listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        for (i in 0..255) {
            val t = i / 255f
            val seg = (t * 4f).toInt().coerceIn(0, 3)
            val lt = ((t - seg * 0.25f) / 0.25f).coerceIn(0f, 1f)
            val p0 = pts[(seg - 1).coerceAtLeast(0)]
            val p1 = pts[seg]
            val p2 = pts[(seg + 1).coerceAtMost(4)]
            val p3 = pts[(seg + 2).coerceAtMost(4)]
            val lt2 = lt * lt
            val lt3 = lt2 * lt
            val y = 0.5f * (
                2f * p1 +
                    (-p0 + p2) * lt +
                    (2f * p0 - 5f * p1 + 4f * p2 - p3) * lt2 +
                    (-p0 + 3f * p1 - 3f * p2 + p3) * lt3
                )
            lut[i] = (y.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        }
        return lut
    }

    fun loadUriOrSample(
        context: Context,
        uri: Uri?,
        sampleSceneKind: Int,
        fallbackExif: PhotoExifInfo,
        maxDimension: Int = 1080
    ): LoadedSourcePhoto {
        if (uri != null) {
            try {
                // 1. Read bounds
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
                val rawW = boundsOpts.outWidth.coerceAtLeast(1)
                val rawH = boundsOpts.outHeight.coerceAtLeast(1)
                var sampleSize = 1
                while (max(rawW, rawH) / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                // 2. Decode scaled mutable-friendly ARGB_8888 bitmap
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }

                // 3. Parse EXIF orientation & real camera tags
                var rotationDeg = 0f
                var parsedExif = fallbackExif.copy(dimensions = "${rawW} × ${rawH} PX")
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    rotationDeg = when (
                        exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    ) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }

                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                    val cameraTitle = when {
                        model.isNotEmpty() && model.startsWith(make, ignoreCase = true) -> model.uppercase()
                        make.isNotEmpty() && model.isNotEmpty() -> "$make $model".uppercase()
                        model.isNotEmpty() -> model.uppercase()
                        else -> "ANDROID OPTICAL SENSOR"
                    }

                    val focalRaw = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                    val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    val shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)

                    val parts = mutableListOf<String>()
                    if (!iso.isNullOrBlank()) parts.add("ISO $iso")
                    if (focalRaw > 0.0) parts.add("${focalRaw.roundToInt()}mm")
                    if (!aperture.isNullOrBlank()) parts.add("f/$aperture")
                    if (!shutter.isNullOrBlank()) {
                        val sVal = shutter.toDoubleOrNull()
                        if (sVal != null && sVal > 0 && sVal < 1.0) {
                            parts.add("1/${(1.0 / sVal).roundToInt()}s")
                        } else {
                            parts.add("${shutter}s")
                        }
                    }

                    parsedExif = PhotoExifInfo(
                        cameraModel = cameraTitle,
                        lensName = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
                            ?: "PRIME OPTICAL MODULE",
                        exposureSummary = if (parts.isNotEmpty()) parts.joinToString(" · ") else "AUTO EXPOSURE · 16-BIT PIPELINE",
                        dimensions = "${rawW} × ${rawH} RAW/RGB"
                    )
                }

                if (decoded != null) {
                    val oriented = if (rotationDeg != 0f) {
                        val m = Matrix().apply { postRotate(rotationDeg) }
                        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
                    } else {
                        decoded.copy(Bitmap.Config.ARGB_8888, false)
                    }
                    return LoadedSourcePhoto(oriented, parsedExif)
                }
            } catch (_: Exception) {
                // Fall back cleanly to studio test plate if URI stream fails
            }
        }

        return LoadedSourcePhoto(
            bitmap = generateStudioTestPlate(sampleSceneKind, width = 640, height = 820),
            exif = fallbackExif
        )
    }

    /**
     * Generates a rich photographic studio plate with bright specular highlights so Halation,
     * Bloom, 3-Way Color Wheels, Curves, and Masking visibly shine out-of-the-box.
     */
    fun generateStudioTestPlate(kind: Int, width: Int = 640, height: Int = 820): Bitmap {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val ny = y.toFloat() / height
            for (x in 0 until width) {
                val nx = x.toFloat() / width
                var r: Float
                var g: Float
                var b: Float

                when (kind % 4) {
                    0 -> {
                        // Window-lit portrait silhouette with bright warm specular bokeh orbs
                        val windowGlow = exp(-((nx - 0.36f).pow(2) + (ny - 0.34f).pow(2)) * 4.2f)
                        r = 26f + 168f * windowGlow + 42f * (1f - ny)
                        g = 20f + 114f * windowGlow + 26f * (1f - ny)
                        b = 28f + 68f * windowGlow + 36f * nx

                        // Specular street/window lamps (triggers gorgeous CineStill Halation!)
                        val lamp1 = exp(-((nx - 0.22f).pow(2) + (ny - 0.22f).pow(2)) * 95f)
                        val lamp2 = exp(-((nx - 0.78f).pow(2) + (ny - 0.19f).pow(2)) * 120f)
                        val lamp3 = exp(-((nx - 0.80f).pow(2) + (ny - 0.58f).pow(2)) * 110f)
                        r += lamp1 * 235f + lamp2 * 220f + lamp3 * 90f
                        g += lamp1 * 205f + lamp2 * 165f + lamp3 * 185f
                        b += lamp1 * 145f + lamp2 * 95f + lamp3 * 235f

                        // Portrait head + shoulder silhouette with rim light
                        val headDist = hypot(nx - 0.50f, (ny - 0.39f) * 1.18f)
                        val bodyDist = hypot((nx - 0.50f) * 0.85f, (ny - 0.82f) * 0.95f)
                        if (headDist < 0.145f || (ny > 0.52f && bodyDist < 0.29f)) {
                            val rim = if (headDist in 0.128f..0.145f) 115f else 0f
                            r = 24f + rim
                            g = 18f + rim * 0.65f
                            b = 22f + rim * 0.35f
                        }
                    }

                    1 -> {
                        // Kowloon Wet Tungsten Neon Night (ideal for 800T Halation + HSL!)
                        val neonCyan = exp(-((nx - 0.24f).pow(2) + (ny - 0.34f).pow(2)) * 14f)
                        val neonRed = exp(-((nx - 0.77f).pow(2) + (ny - 0.30f).pow(2)) * 13f)
                        val wetReflect = if (ny > 0.60f) sin(ny * 95f) * 0.5f + 0.5f else 0f

                        r = 14f + neonRed * 215f + wetReflect * neonRed * 110f
                        g = 18f + neonCyan * 185f + neonRed * 42f + wetReflect * neonCyan * 85f
                        b = 34f + neonCyan * 230f + neonRed * 95f + wetReflect * 95f

                        // Bright neon tube cores
                        if (nx in 0.17f..0.21f && ny in 0.16f..0.46f) {
                            r = 205f; g = 250f; b = 255f
                        }
                        if (nx in 0.78f..0.83f && ny in 0.14f..0.48f) {
                            r = 255f; g = 185f; b = 175f
                        }
                    }

                    2 -> {
                        // Lofoten Arctic Fjord Sunset & Sun Disc
                        val horizonGlow = exp(-((ny - 0.48f).pow(2)) * 14f)
                        val sunDisc = exp(-((nx - 0.62f).pow(2) + (ny - 0.38f).pow(2)) * 140f)
                        r = 24f + horizonGlow * 205f + sunDisc * 245f
                        g = 38f + horizonGlow * 118f + sunDisc * 215f + (1f - ny) * 32f
                        b = 68f + (1f - ny) * 98f + ny * 54f + sunDisc * 165f

                        // Jagged mountain ridge
                        val ridgeY = 0.54f - 0.22f * exp(-((nx - 0.30f).pow(2)) * 22f) -
                            0.18f * exp(-((nx - 0.74f).pow(2)) * 26f)
                        if (ny in ridgeY..0.57f) {
                            r = 22f; g = 24f; b = 34f
                        }
                    }

                    else -> {
                        // Brutalist Architectural Sunbeam & Silver Contrast
                        val diagonalShaft = exp(-((nx * 0.9f - ny * 0.7f - 0.12f).pow(2)) * 28f)
                        r = 18f + diagonalShaft * 218f
                        g = 18f + diagonalShaft * 212f
                        b = 20f + diagonalShaft * 204f
                        if (nx > 0.64f && ny > 0.32f) {
                            r *= 0.26f; g *= 0.26f; b *= 0.28f
                        }
                    }
                }

                pixels[y * width + x] = (0xFF shl 24) or
                    (clamp255(r) shl 16) or
                    (clamp255(g) shl 8) or
                    clamp255(b)
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun wheelToRgbDelta(wheel: WheelGrade): Triple<Float, Float, Float> {
        val mag = hypot(wheel.offsetX, wheel.offsetY).coerceIn(0f, 1f)
        if (mag < 0.01f && abs(wheel.lumaTrim) < 0.5f) return Triple(0f, 0f, 0f)
        val angle = atan2(wheel.offsetY, wheel.offsetX) // -PI..PI
        // Color wheel mapping: 0 rad = Red/Warm, +2.09 rad = Green, -2.09 rad = Blue/Teal
        val rTint = cos(angle) * mag * 46f + wheel.lumaTrim * 0.9f
        val gTint = cos(angle - 2.094f) * mag * 46f + wheel.lumaTrim * 0.9f
        val bTint = cos(angle + 2.094f) * mag * 46f + wheel.lumaTrim * 0.9f
        return Triple(rTint, gTint, bTint)
    }

    /**
     * Master Non-Destructive Pixel Pipeline:
     * Geometry -> Tone -> WB/Sat -> 3-Way Color Wheels -> 8-Zone HSL -> Spline Curves ->
     * Selective Mask -> Analog Halation & Bloom -> 35mm Grain -> Gallery Print Rebate + Scope.
     */
    fun processPreview(
        source: Bitmap,
        state: DarkroomState,
        targetMaxDim: Int = 720
    ): GradedFrameResult {
        // 1. Apply Crop Aspect + Rotation + Straighten + Mirror onto aligned base bitmap
        val alignedRaw = createGeometryAlignedBitmap(source, state, targetMaxDim)
        val w = alignedRaw.width
        val h = alignedRaw.height
        val pixels = IntArray(w * h)
        alignedRaw.getPixels(pixels, 0, w, 0, 0, w, h)

        val lumaLut = buildSplineLut(state.curves[CurveChannelId.LUMA].orEmpty())
        val rLut = buildSplineLut(state.curves[CurveChannelId.RED].orEmpty())
        val gLut = buildSplineLut(state.curves[CurveChannelId.GREEN].orEmpty())
        val bLut = buildSplineLut(state.curves[CurveChannelId.BLUE].orEmpty())

        val expGain = 2f.pow(state.exposure / 65f)
        val cNorm = (state.contrast + state.clarity * 0.35f) / 100f
        val contrastMult = if (cNorm >= 0f) 1f + cNorm * 1.15f else 1f + cNorm * 0.65f

        val hiDelta = state.highlights * 0.85f
        val shDelta = state.shadows * 0.85f
        val whDelta = state.whites * 0.80f
        val blDelta = state.blacks * 0.80f
        val matteLift = state.matteFade * 0.70f

        val tempShift = state.temperature * 0.75f
        val tintShift = state.tint * 0.55f
        val satScale = 1f + state.saturation / 100f
        val vibScale = state.vibrance / 100f

        val (shWr, shWg, shWb) = wheelToRgbDelta(state.shadowWheel)
        val (midWr, midWg, midWb) = wheelToRgbDelta(state.midtoneWheel)
        val (hiWr, hiWg, hiWb) = wheelToRgbDelta(state.highlightWheel)
        val wheelsActive = abs(shWr) + abs(shWb) + abs(midWr) + abs(hiWr) + abs(hiWb) > 0.5f

        val hslActive = state.hslZones.any {
            abs(it.hue) > 0.5f || abs(it.sat) > 0.5f || abs(it.lum) > 0.5f
        }

        val maskOn = state.maskType != MaskType.NONE
        val maskExpGain = 2f.pow(state.maskExposure / 75f)
        val maskWarmShift = state.maskWarmth * 0.65f

        val vigStrength = state.vignette / 100f
        val grainStrength = (state.grain / 100f) * 38f
        val halfW = w * 0.5f
        val halfH = h * 0.5f
        val invMaxDistSq = 1f / (halfW * halfW + halfH * halfH).coerceAtLeast(1f)

        // Bright highlight map on 1/4 grid for real Analog Halation & Optical Bloom!
        val doHalationOrBloom = state.halation > 1f || state.bloom > 1f
        val gw = (w / 4).coerceAtLeast(1)
        val gh = (h / 4).coerceAtLeast(1)
        val brightGrid = if (doHalationOrBloom) FloatArray(gw * gh) else null

        var idx = 0
        for (y in 0 until h) {
            val ny = y.toFloat() / h
            val dy = y - halfH
            val dySq = dy * dy

            for (x in 0 until w) {
                val nx = x.toFloat() / w
                val c = pixels[idx]
                var r = ((c ushr 16) and 0xFF).toFloat()
                var g = ((c ushr 8) and 0xFF).toFloat()
                var b = (c and 0xFF).toFloat()

                // Selective Mask Weight (0..1)
                var maskWeight = 0f
                if (maskOn) {
                    maskWeight = when (state.maskType) {
                        MaskType.RADIAL_SPOT -> {
                            val d = hypot(nx - state.maskCenterX, ny - state.maskCenterY)
                            val rad = state.maskRadius.coerceAtLeast(0.12f)
                            (1f - (d / rad)).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
                        }
                        MaskType.SKY_GRADIENT -> {
                            val span = 0.35f
                            ((state.maskCenterY + span * 0.5f - ny) / span).coerceIn(0f, 1f)
                        }
                        MaskType.EDGE_VIGNETTE -> {
                            val d = hypot(nx - 0.5f, ny - 0.5f) / 0.707f
                            d.coerceIn(0f, 1f).pow(1.6f)
                        }
                        MaskType.NONE -> 0f
                    }
                }

                // 1. Global + Masked Exposure & White Balance
                val combinedGain = if (maskWeight > 0f) {
                    expGain * (1f + (maskExpGain - 1f) * maskWeight)
                } else {
                    expGain
                }
                val localWarm = tempShift + maskWarmShift * maskWeight

                r = r * combinedGain + localWarm + tintShift * 0.3f
                g = g * combinedGain - tintShift * 0.65f
                b = b * combinedGain - localWarm + tintShift * 0.3f

                // 2. Highlight / Shadow / White / Black recovery
                val lumaNorm = ((0.299f * r + 0.587f * g + 0.114f * b) / 255f).coerceIn(0f, 1f)
                val shMask = (1f - lumaNorm) * (1f - lumaNorm)
                val hiMask = lumaNorm * lumaNorm
                val toneOffset = shDelta * shMask +
                    hiDelta * hiMask +
                    blDelta * shMask * (1f - lumaNorm) +
                    whDelta * hiMask * lumaNorm
                r += toneOffset
                g += toneOffset
                b += toneOffset

                // 3. Contrast & Matte Paper Lift
                r = (r - 128f) * contrastMult + 128f
                g = (g - 128f) * contrastMult + 128f
                b = (b - 128f) * contrastMult + 128f
                if (matteLift > 0f) {
                    val comp = 1f - matteLift / 255f
                    r = r * comp + matteLift
                    g = g * comp + matteLift
                    b = b * comp + matteLift
                }

                // 4. Skin-Safe Vibrance & Saturation
                if (satScale != 1f || vibScale != 0f) {
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    val maxCh = max(r, max(g, b))
                    val minCh = min(r, min(g, b))
                    val curSat = if (maxCh > 1f) (maxCh - minCh) / 255f else 0f
                    val boost = (satScale + vibScale * (1f - min(1f, curSat * 1.35f))).coerceAtLeast(0f)
                    r = gray + (r - gray) * boost
                    g = gray + (g - gray) * boost
                    b = gray + (b - gray) * boost
                }

                // 5. 3-Way Cinema Color Balance Wheels
                if (wheelsActive) {
                    val midWeight = (1f - abs(lumaNorm - 0.5f) * 2f).coerceIn(0f, 1f)
                    r += shWr * shMask + midWr * midWeight + hiWr * hiMask
                    g += shWg * shMask + midWg * midWeight + hiWg * hiMask
                    b += shWb * shMask + midWb * midWeight + hiWb * hiMask
                }

                // 6. 8-Zone HSL Equalizer (fast selective hue band shift)
                if (hslActive) {
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    val delta = maxC - minC
                    if (delta > 12f) {
                        val hueDeg = when (maxC) {
                            r -> ((g - b) / delta + if (g < b) 6f else 0f) * 60f
                            g -> ((b - r) / delta + 2f) * 60f
                            else -> ((r - g) / delta + 4f) * 60f
                        }
                        var satDelta = 0f
                        var lumDelta = 0f
                        for (z in state.hslZones) {
                            if (z.sat == 0f && z.lum == 0f && z.hue == 0f) continue
                            var diff = abs(hueDeg - z.centerHue)
                            if (diff > 180f) diff = 360f - diff
                            if (diff < 40f) {
                                val wZone = 1f - diff / 40f
                                satDelta += (z.sat / 100f) * wZone
                                lumDelta += z.lum * 0.45f * wZone
                            }
                        }
                        if (satDelta != 0f || lumDelta != 0f) {
                            val gray = 0.299f * r + 0.587f * g + 0.114f * b
                            val sm = (1f + satDelta).coerceIn(0f, 2.2f)
                            r = gray + (r - gray) * sm + lumDelta
                            g = gray + (g - gray) * sm + lumDelta
                            b = gray + (b - gray) * sm + lumDelta
                        }
                    }
                }

                // 7. 4-Channel Spline Tone Curves
                val ri = lumaLut[rLut[clamp255(r)]]
                val gi = lumaLut[gLut[clamp255(g)]]
                val bi = lumaLut[bLut[clamp255(b)]]
                r = ri.toFloat()
                g = gi.toFloat()
                b = bi.toFloat()

                // Record bright specular energy for Halation & Bloom pass
                if (brightGrid != null && (x and 3) == 0 && (y and 3) == 0) {
                    val peak = max(r, max(g, b))
                    val spec = ((peak - 188f) / 67f).coerceIn(0f, 1f)
                    val gx = (x shr 2).coerceAtMost(gw - 1)
                    val gy = (y shr 2).coerceAtMost(gh - 1)
                    brightGrid[gy * gw + gx] = spec * spec
                }

                // 8. Vignette & 35mm Silver Grain
                if (vigStrength > 0.01f) {
                    val dx = x - halfW
                    val normD = (dx * dx + dySq) * invMaxDistSq
                    val vf = (1f - vigStrength * normD * 0.82f).coerceIn(0.15f, 1f)
                    r *= vf; g *= vf; b *= vf
                }
                if (grainStrength > 0.5f) {
                    val hash = (((x * 73856093) xor (y * 19349663)) and 255) / 255f - 0.5f
                    val gr = hash * grainStrength
                    r += gr; g += gr; b += gr
                }

                // Optional Ruby Mask Isolation Overlay (Dehancer / Lightroom style)
                if (maskOn && state.showRubyMaskOverlay && maskWeight > 0.02f) {
                    val rubyAlpha = (maskWeight * 0.62f).coerceIn(0f, 0.75f)
                    r = r * (1f - rubyAlpha) + 255f * rubyAlpha
                    g = g * (1f - rubyAlpha) + 42f * rubyAlpha
                    b = b * (1f - rubyAlpha) + 75f * rubyAlpha
                }

                pixels[idx] = (0xFF shl 24) or
                    (clamp255(r) shl 16) or
                    (clamp255(g) shl 8) or
                    clamp255(b)
                idx++
            }
        }

        // 9. Separable Box-Blur Halation (Red-Orange Film Scatter) & Optical Bloom Pass!
        if (brightGrid != null) {
            val blurred = boxBlurGrid(brightGrid, gw, gh, radius = 4)
            val halAmt = (state.halation / 100f) * 145f
            val bloomAmt = (state.bloom / 100f) * 95f

            var p = 0
            for (y in 0 until h) {
                val gy = (y shr 2).coerceAtMost(gh - 1)
                val rowOff = gy * gw
                for (x in 0 until w) {
                    val gx = (x shr 2).coerceAtMost(gw - 1)
                    val glow = blurred[rowOff + gx]
                    if (glow > 0.008f) {
                        val c = pixels[p]
                        // Halation scatters strongly into Red, moderately into Orange-Green, zero Blue!
                        val nr = ((c ushr 16) and 0xFF) + glow * (halAmt + bloomAmt)
                        val ng = ((c ushr 8) and 0xFF) + glow * (halAmt * 0.28f + bloomAmt * 0.92f)
                        val nb = (c and 0xFF) + glow * (bloomAmt * 0.88f)
                        pixels[p] = (0xFF shl 24) or
                            (clamp255(nr) shl 16) or
                            (clamp255(ng) shl 8) or
                            clamp255(nb)
                    }
                    p++
                }
            }
        }

        // 10. Compute Live 48-Bin RGB Parade & Luminance Waveform Scope
        val rHist = FloatArray(48)
        val gHist = FloatArray(48)
        val bHist = FloatArray(48)
        val lHist = FloatArray(48)
        var shClips = 0
        var hiClips = 0
        var samples = 0
        for (i in pixels.indices step 7) {
            val c = pixels[i]
            val rv = (c ushr 16) and 0xFF
            val gv = (c ushr 8) and 0xFF
            val bv = c and 0xFF
            val lv = (0.299f * rv + 0.587f * gv + 0.114f * bv).toInt().coerceIn(0, 255)
            rHist[(rv * 47) / 255]++
            gHist[(gv * 47) / 255]++
            bHist[(bv * 47) / 255]++
            lHist[(lv * 47) / 255]++
            if (lv <= 3) shClips++
            if (lv >= 252) hiClips++
            samples++
        }
        val maxBin = maxOf(
            rHist.maxOrNull() ?: 1f,
            gHist.maxOrNull() ?: 1f,
            bHist.maxOrNull() ?: 1f,
            1f
        )
        for (bIdx in 0 until 48) {
            rHist[bIdx] = (rHist[bIdx] / maxBin).coerceIn(0f, 1f)
            gHist[bIdx] = (gHist[bIdx] / maxBin).coerceIn(0f, 1f)
            bHist[bIdx] = (bHist[bIdx] / maxBin).coerceIn(0f, 1f)
            lHist[bIdx] = (lHist[bIdx] / maxBin).coerceIn(0f, 1f)
        }

        val gradedCore = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
        val finalFramed = if (state.printStyle != PrintBorderStyle.NONE) {
            applyGalleryPrintBorder(gradedCore, state)
        } else {
            gradedCore
        }

        return GradedFrameResult(
            gradedBitmap = finalFramed,
            rawAlignedBitmap = alignedRaw,
            scope = RgbScopeData(
                rBins = rHist,
                gBins = gHist,
                bBins = bHist,
                lumaBins = lHist,
                clippedShadows = shClips > samples * 0.025f,
                clippedHighlights = hiClips > samples * 0.025f
            )
        )
    }

    private fun boxBlurGrid(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val tmp = FloatArray(w * h)
        val out = FloatArray(w * h)
        val diam = radius * 2 + 1
        // Horizontal
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) {
                    val sx = (x + k).coerceIn(0, w - 1)
                    sum += src[row + sx]
                }
                tmp[row + x] = sum / diam
            }
        }
        // Vertical
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) {
                    val sy = (y + k).coerceIn(0, h - 1)
                    sum += tmp[sy * w + x]
                }
                out[y * w + x] = sum / diam
            }
        }
        return out
    }

    private fun createGeometryAlignedBitmap(
        source: Bitmap,
        state: DarkroomState,
        maxDim: Int
    ): Bitmap {
        // Aspect crop rect
        var cropW = source.width
        var cropH = source.height
        val targetRatio: Float? = when (state.aspectRatio) {
            "1:1" -> 1f
            "4:5" -> 4f / 5f
            "3:2" -> 3f / 2f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            else -> null
        }
        if (targetRatio != null) {
            val curRatio = source.width.toFloat() / source.height.coerceAtLeast(1)
            if (curRatio > targetRatio) {
                cropW = (source.height * targetRatio).roundToInt().coerceAtLeast(1)
            } else {
                cropH = (source.width / targetRatio).roundToInt().coerceAtLeast(1)
            }
        }
        val cropX = ((source.width - cropW) / 2).coerceAtLeast(0)
        val cropY = ((source.height - cropH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)

        val scale = min(1f, maxDim.toFloat() / max(cropped.width, cropped.height).coerceAtLeast(1))
        val m = Matrix().apply {
            postScale(
                scale * if (state.mirrorHorizontal) -1f else 1f,
                scale
            )
            val totalAngle = state.quarterTurns * 90f + state.straightenDegrees
            if (totalAngle != 0f) {
                postRotate(totalAngle)
            }
        }
        return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, m, true)
    }

    private fun applyGalleryPrintBorder(photo: Bitmap, state: DarkroomState): Bitmap {
        val minSide = min(photo.width, photo.height)
        val sidePad = (minSide * 0.055f).roundToInt().coerceAtLeast(14)
        val botPad = (minSide * 0.16f).roundToInt().coerceAtLeast(48)
        val outW = photo.width + sidePad * 2
        val outH = photo.height + sidePad + botPad

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val isLightPaper = state.printStyle == PrintBorderStyle.MUSEUM_WHITE
        paint.color = when (state.printStyle) {
            PrintBorderStyle.MUSEUM_WHITE -> 0xFFF7F5F0.toInt()
            PrintBorderStyle.HASSELBLAD_DARK -> 0xFF0C0D10.toInt()
            PrintBorderStyle.CONTACT_35MM -> 0xFF09090B.toInt()
            PrintBorderStyle.NONE -> 0xFF000000.toInt()
        }
        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), paint)
        canvas.drawBitmap(photo, sidePad.toFloat(), sidePad.toFloat(), null)

        // 35mm film sprocket & orange rebate code
        if (state.printStyle == PrintBorderStyle.CONTACT_35MM) {
            paint.color = 0xFFE87B35.toInt()
            paint.typeface = Typeface.MONOSPACE
            paint.textSize = (minSide * 0.022f).coerceAtLeast(10f)
            canvas.drawText(
                "VELOX SAFETY FILM 800T   ▶ ${state.activeRoll.frameCode}",
                sidePad.toFloat(),
                sidePad * 0.72f,
                paint
            )
        }

        // Camera & Lens EXIF Stamp
        paint.color = if (isLightPaper) 0xFF141519.toInt() else 0xFFF3F4F6.toInt()
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val titleSize = (minSide * 0.028f).coerceAtLeast(12f)
        paint.textSize = titleSize
        val baseY = sidePad + photo.height + botPad * 0.46f
        val caption = state.signatureText.ifBlank { state.exif.cameraModel }
        canvas.drawText(caption, sidePad.toFloat(), baseY, paint)

        paint.color = if (isLightPaper) 0xFF6B7280.toInt() else 0xFF9CA3AF.toInt()
        paint.typeface = Typeface.MONOSPACE
        val subSize = (minSide * 0.020f).coerceAtLeast(10f)
        paint.textSize = subSize
        canvas.drawText(
            "${state.exif.lensName}  ·  ${state.exif.exposureSummary}",
            sidePad.toFloat(),
            baseY + subSize * 1.55f,
            paint
        )

        // Iconic Vermilion Red Dot on right margin
        paint.color = 0xFFFF4D2D.toInt()
        canvas.drawCircle(
            outW - sidePad - titleSize * 0.5f,
            baseY - titleSize * 0.25f,
            titleSize * 0.36f,
            paint
        )

        // Subtle inner keyline around photo on Museum Matte
        if (isLightPaper) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = 0x22000000
            canvas.drawRect(
                RectF(
                    sidePad.toFloat(),
                    sidePad.toFloat(),
                    (sidePad + photo.width).toFloat(),
                    (sidePad + photo.height).toFloat()
                ),
                paint
            )
        }

        return out
    }

    /**
     * Renders high-resolution master image and writes it into Android MediaStore Gallery!
     */
    fun exportMasterToGallery(
        context: Context,
        source: Bitmap,
        state: DarkroomState,
        format: ExportFormat,
        quality: Int,
        shareAfterSave: Boolean
    ): Uri? {
        val masterFrame = processPreview(source, state, targetMaxDim = 2048)
        val bmp = masterFrame.gradedBitmap
        val filename = "VELOX_${System.currentTimeMillis()}.${format.ext}"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/VeloxAtelier"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return try {
            resolver.openOutputStream(itemUri)?.use { outStream ->
                val compressFormat = when (format) {
                    ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                bmp.compress(compressFormat, quality.coerceIn(60, 100), outStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, doneValues, null, null)
            }

            if (shareAfterSave) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = format.mime
                    putExtra(Intent.EXTRA_STREAM, itemUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share VELOX Master Render")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            itemUri
        } catch (_: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
    }
}
