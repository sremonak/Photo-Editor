package com.lumacraft.studio.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.lumacraft.studio.model.Adjustments
import com.lumacraft.studio.model.AiState
import com.lumacraft.studio.model.BrushStroke
import com.lumacraft.studio.model.MaskPaintStroke
import com.lumacraft.studio.model.CollageConfig
import com.lumacraft.studio.model.CurveChannel
import com.lumacraft.studio.model.EditState
import com.lumacraft.studio.model.ExportFormat
import com.lumacraft.studio.model.ExportOptions
import com.lumacraft.studio.model.GeoState
import com.lumacraft.studio.model.HealSpot
import com.lumacraft.studio.model.TapFix
import com.lumacraft.studio.model.WheelGrade
import com.lumacraft.studio.model.AVAILABLE_FONTS
import kotlin.math.PI
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

data class LoadedPhoto(val bitmap: Bitmap, val info: String, val dims: String)

data class ProcessedFrame(
    val bitmap: Bitmap,
    val hist: IntArray,        // 32 luma bins
    val clipShadow: Boolean,
    val clipHighlight: Boolean
)

/** Parsed Adobe .cube LUT — either a 3D (size³) or a 1D (size) table. */
class ParsedLut(val is3D: Boolean, val size: Int, val table: FloatArray)

/**
 * Adobe .cube parser + applier with trilinear 3D / linear 1D interpolation.
 * Accepts 3D luts up to 33³ and 1D luts up to 4096 entries.
 */
object LutEngine {
    private var cacheHash: Int = 0
    private var cacheLut: ParsedLut? = null

    @Synchronized
    fun parse(text: String): ParsedLut? {
        val h = text.hashCode()
        if (h == cacheHash) return cacheLut
        val lut = doParse(text)
        cacheHash = h
        cacheLut = lut
        return lut
    }

    private fun doParse(text: String): ParsedLut? {
        var is3D = true
        var size = 0
        val values = ArrayList<Float>(4096)
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            when {
                line.startsWith("LUT_3D_SIZE") -> {
                    is3D = true
                    size = line.substringAfter("LUT_3D_SIZE").trim().toIntOrNull() ?: 0
                }
                line.startsWith("LUT_1D_SIZE") -> {
                    is3D = false
                    size = line.substringAfter("LUT_1D_SIZE").trim().toIntOrNull() ?: 0
                }
                line.startsWith("TITLE") || line.startsWith("DOMAIN_") -> {}
                else -> {
                    val parts = line.split(Regex("[\\s,]+"))
                    if (parts.size >= 3) {
                        try {
                            values.add(parts[0].toFloat())
                            values.add(parts[1].toFloat())
                            values.add(parts[2].toFloat())
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        if (size <= 0) return null
        if (is3D && size > 33) return null
        if (!is3D && size > 4096) return null
        val expected = if (is3D) size * size * size * 3 else size * 3
        if (values.size < expected) return null
        return ParsedLut(is3D, size, values.toFloatArray())
    }

    fun apply(lut: ParsedLut, r: Float, g: Float, b: Float, intensity: Float): Triple<Float, Float, Float> =
        if (lut.is3D) apply3D(lut, r, g, b, intensity) else apply1D(lut, r, g, b, intensity)

    private fun apply1D(lut: ParsedLut, r: Float, g: Float, b: Float, k: Float): Triple<Float, Float, Float> {
        val s = lut.size
        val t = lut.table
        fun chan(v: Float, off: Int): Float {
            val x = (v / 255f * (s - 1)).coerceIn(0f, (s - 1).toFloat())
            val i = x.toInt()
            val f = x - i
            val lo = t[i * 3 + off] * 255f
            val hi = if (i + 1 < s) t[(i + 1) * 3 + off] * 255f else lo
            return lo + (hi - lo) * f
        }
        val nr = chan(r, 0)
        val ng = chan(g, 1)
        val nb = chan(b, 2)
        return Triple(r + (nr - r) * k, g + (ng - g) * k, b + (nb - b) * k)
    }

    private fun apply3D(lut: ParsedLut, r: Float, g: Float, b: Float, k: Float): Triple<Float, Float, Float> {
        val s = lut.size
        val t = lut.table
        val m = s - 1
        fun corner(ri: Int, gi: Int, bi: Int, ch: Int): Float =
            t[(((ri * s + gi) * s + bi) * 3) + ch]
        fun frac(v: Float): Pair<Int, Float> {
            val x = (v / 255f * m).coerceIn(0f, m.toFloat())
            val i = x.toInt()
            return i to (x - i)
        }
        val (r0, fr) = frac(r)
        val (g0, fg) = frac(g)
        val (b0, fb) = frac(b)
        val r1 = min(r0 + 1, m)
        val g1 = min(g0 + 1, m)
        val b1 = min(b0 + 1, m)
        fun lerp(a: Float, b2: Float, f: Float) = a + (b2 - a) * f
        fun trilerp(ch: Int): Float {
            val c000 = corner(r0, g0, b0, ch); val c100 = corner(r1, g0, b0, ch)
            val c010 = corner(r0, g1, b0, ch); val c110 = corner(r1, g1, b0, ch)
            val c001 = corner(r0, g0, b1, ch); val c101 = corner(r1, g0, b1, ch)
            val c011 = corner(r0, g1, b1, ch); val c111 = corner(r1, g1, b1, ch)
            val c00 = lerp(c000, c100, fr); val c10 = lerp(c010, c110, fr)
            val c01 = lerp(c001, c101, fr); val c11 = lerp(c011, c111, fr)
            val c0 = lerp(c00, c10, fg); val c1 = lerp(c01, c11, fg)
            return lerp(c0, c1, fb) * 255f
        }
        val nr = trilerp(0)
        val ng = trilerp(1)
        val nb = trilerp(2)
        return Triple(r + (nr - r) * k, g + (ng - g) * k, b + (nb - b) * k)
    }
}

object ImageEditEngine {

    private fun cl(v: Float): Int {
        val i = v.toInt()
        return if (i < 0) 0 else if (i > 255) 255 else i
    }

    // ------------------------------------------------------------- decoding

    fun decode(context: Context, uri: Uri?, maxDim: Int, sampleKind: Int = 0): LoadedPhoto? {
        if (uri != null) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0) return null
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return null

                var rotation = 0f
                var info = "LOCAL PHOTO"
                context.contentResolver.openInputStream(uri)?.use { s ->
                    try {
                        val exif = ExifInterface(s)
                        rotation = when (
                            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        ) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                        val cam = when {
                            model.isEmpty() -> "Local Photo"
                            model.startsWith(make, true) -> model
                            make.isEmpty() -> model
                            else -> "$make $model"
                        }
                        val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                        val ap = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                        val shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                        val parts = mutableListOf<String>()
                        if (!iso.isNullOrBlank()) parts.add("ISO $iso")
                        if (focal > 0.0) parts.add("${focal.roundToInt()}mm")
                        if (!ap.isNullOrBlank()) parts.add("f/$ap")
                        if (!shutter.isNullOrBlank()) {
                            val sv = shutter.toDoubleOrNull()
                            if (sv != null && sv > 0 && sv < 1.0) parts.add("1/${(1.0 / sv).roundToInt()}s")
                        }
                        info = if (parts.isNotEmpty()) "$cam · ${parts.joinToString(" ")}" else cam
                    } catch (_: Exception) {
                    }
                }

                val oriented = if (rotation != 0f) {
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
                } else {
                    decoded.copy(Bitmap.Config.ARGB_8888, false)
                }
                return LoadedPhoto(oriented, info, "${bounds.outWidth} × ${bounds.outHeight}")
            } catch (_: Exception) {
                return null
            }
        }
        // Demo plate when launched without a photo ("New Project" showcase)
        return LoadedPhoto(samplePlate(sampleKind, 900, 1200), "LumaCraft Demo Plate", "900 × 1200")
    }

    /** Procedurally generated demo scene (used by New Project & filter thumbnails). */
    fun samplePlate(kind: Int, w: Int = 320, h: Int = 400): Bitmap {
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val ny = y.toFloat() / h
            for (x in 0 until w) {
                val nx = x.toFloat() / w
                var r: Float
                var g: Float
                var b: Float
                when (kind % 3) {
                    0 -> { // sunset portrait silhouette
                        val glow = exp(-((nx - 0.4f).pow(2) + (ny - 0.4f).pow(2)) * 5f)
                        r = 40f + 190f * glow + (1f - ny) * 30f
                        g = 34f + 120f * glow
                        b = 60f + 40f * glow + ny * 50f
                        val d = hypot((nx - 0.5f) * 1.1f, (ny - 0.62f) * 0.9f)
                        if (d < 0.3f) {
                            val rim = if (d in 0.27f..0.3f) 90f else 0f
                            r = 26f + rim; g = 18f + rim * 0.6f; b = 20f + rim * 0.3f
                        }
                    }
                    1 -> { // neon street
                        val cyan = exp(-((nx - 0.25f).pow(2) + (ny - 0.3f).pow(2)) * 16f)
                        val red = exp(-((nx - 0.75f).pow(2) + (ny - 0.36f).pow(2)) * 14f)
                        r = 16f + red * 220f
                        g = 20f + cyan * 170f + red * 40f
                        b = 40f + cyan * 235f + red * 90f
                        val reflect = if (ny > 0.62f) (sin(ny * 80f) * 0.5f + 0.5f) else 0f
                        r += reflect * 26f; g += reflect * 30f; b += reflect * 34f
                    }
                    else -> { // mountain dusk
                        val sky = exp(-((ny - 0.46f).pow(2)) * 12f)
                        val sun = exp(-((nx - 0.6f).pow(2) + (ny - 0.4f).pow(2)) * 130f)
                        r = 30f + sky * 180f + sun * 240f
                        g = 40f + sky * 110f + sun * 200f
                        b = 70f + (1f - ny) * 90f + sun * 150f
                        val ridge = 0.56f - 0.2f * exp(-((nx - 0.32f).pow(2)) * 20f) - 0.15f * exp(-((nx - 0.72f).pow(2)) * 24f)
                        if (ny > ridge && ny < 0.6f) { r = 26f; g = 28f; b = 40f }
                    }
                }
                px[y * w + x] = (0xFF shl 24) or (cl(r) shl 16) or (cl(g) shl 8) or cl(b)
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    // ------------------------------------------------------------- curves & color helpers

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
                2f * p1 + (-p0 + p2) * lt +
                    (2f * p0 - 5f * p1 + 4f * p2 - p3) * lt2 +
                    (-p0 + 3f * p1 - 3f * p2 + p3) * lt3
                )
            lut[i] = (y.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        }
        return lut
    }

    private fun hueRotateMatrix(deg: Float): FloatArray {
        val a = deg * PI.toFloat() / 180f
        val c = cos(a)
        val s = sin(a)
        return floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f
        )
    }

    private fun applyMatrix(m: FloatArray, r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val nr = m[0] * r + m[1] * g + m[2] * b
        val ng = m[3] * r + m[4] * g + m[5] * b
        val nb = m[6] * r + m[7] * g + m[8] * b
        return Triple(nr, ng, nb)
    }

    private fun hueTint(hueDeg: Float): Triple<Float, Float, Float> {
        val rad = (hueDeg % 360f + 360f) % 360f / 60f
        val i = rad.toInt() % 6
        val f = rad - rad.toInt()
        val q = 1f - f
        return when (i) {
            0 -> Triple(1f, f, 0f)
            1 -> Triple(q, 1f, 0f)
            2 -> Triple(0f, 1f, f)
            3 -> Triple(0f, q, 1f)
            4 -> Triple(f, 0f, 1f)
            else -> Triple(1f, 0f, q)
        }
    }

    private fun wheelDelta(w: WheelGrade): Triple<Float, Float, Float> {
        val mag = hypot(w.x, w.y).coerceIn(0f, 1f)
        if (mag < 0.01f) return Triple(0f, 0f, 0f)
        val ang = atan2(w.y, w.x)
        val strength = 46f * mag
        return Triple(cos(ang) * strength, cos(ang - 2.094f) * strength, cos(ang + 2.094f) * strength)
    }

    // ------------------------------------------------------------- geometry

    private fun align(source: Bitmap, geo: GeoState, maxDim: Int): Bitmap {
        var cw = source.width
        var ch = source.height
        val ratio = when (geo.aspect) {
            "1:1" -> 1f
            "4:5" -> 0.8f
            "3:4" -> 0.75f
            "2:3" -> 2f / 3f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            "4:3" -> 4f / 3f
            "3:2" -> 1.5f
            else -> null
        }
        if (ratio != null) {
            val cur = source.width.toFloat() / source.height.coerceAtLeast(1)
            if (cur > ratio) cw = (source.height * ratio).roundToInt().coerceAtLeast(1)
            else ch = (source.width / ratio).roundToInt().coerceAtLeast(1)
        }
        val cx = ((source.width - cw) / 2).coerceAtLeast(0)
        val cy = ((source.height - ch) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(source, cx, cy, cw, ch)

        val scale = min(1f, maxDim.toFloat() / max(cropped.width, cropped.height).coerceAtLeast(1))
        var work = Bitmap.createScaledBitmap(cropped, (cropped.width * scale).roundToInt().coerceAtLeast(1), (cropped.height * scale).roundToInt().coerceAtLeast(1), true)

        if (abs(geo.keystoneV) > 0.5f || abs(geo.keystoneH) > 0.5f) {
            val wf = work.width.toFloat()
            val hf = work.height.toFloat()
            val kv = (geo.keystoneV / 100f) * wf * 0.22f
            val kh = (geo.keystoneH / 100f) * hf * 0.22f
            val src = floatArrayOf(0f, 0f, wf, 0f, wf, hf, 0f, hf)
            val dst = floatArrayOf(
                -min(0f, kv), -min(0f, kh),
                wf + min(0f, kv), max(0f, kh),
                wf - max(0f, kv), hf - max(0f, kh),
                max(0f, kv), hf + min(0f, kh)
            )
            val pm = Matrix()
            if (pm.setPolyToPoly(src, 0, dst, 0, 4)) {
                val out = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
                Canvas(out).drawBitmap(work, pm, Paint(Paint.FILTER_BITMAP_FLAG))
                work = out
            }
        }

        val m = Matrix()
        if (geo.flipH) m.postScale(-1f, 1f, work.width * 0.5f, work.height * 0.5f)
        if (geo.flipV) m.postScale(1f, -1f, work.width * 0.5f, work.height * 0.5f)
        val total = geo.quarterTurns * 90f + geo.straighten
        if (total != 0f) m.postRotate(total)
        return if (m.isIdentity) work else Bitmap.createBitmap(work, 0, 0, work.width, work.height, m, true)
    }

    // ------------------------------------------------------------- retouch pre-passes

    private fun healCircle(px: IntArray, w: Int, h: Int, cx: Int, cy: Int, radPx: Int) {
        val ring = (radPx * 1.45f)
        var ar = 0f; var ag = 0f; var ab = 0f
        val n = 12
        for (s in 0 until n) {
            val ang = (2.0 * PI * s) / n
            val sx = (cx + cos(ang) * ring).roundToInt().coerceIn(0, w - 1)
            val sy = (cy + sin(ang) * ring).roundToInt().coerceIn(0, h - 1)
            val c = px[sy * w + sx]
            ar += (c ushr 16) and 0xFF; ag += (c ushr 8) and 0xFF; ab += c and 0xFF
        }
        ar /= n; ag /= n; ab /= n
        val x0 = (cx - radPx).coerceAtLeast(0); val x1 = (cx + radPx).coerceAtMost(w - 1)
        val y0 = (cy - radPx).coerceAtLeast(0); val y1 = (cy + radPx).coerceAtMost(h - 1)
        for (py in y0..y1) {
            for (pxx in x0..x1) {
                val d = hypot((pxx - cx).toFloat(), (py - cy).toFloat())
                if (d < radPx) {
                    val t = (1f - d / radPx).coerceIn(0f, 1f)
                    val feather = t * t * (3f - 2f * t)
                    val o = px[py * w + pxx]
                    val cxx = (pxx + (radPx * 1.4f).roundToInt()).coerceIn(0, w - 1)
                    val cc = px[py * w + cxx]
                    val pr = ar * 0.6f + ((cc ushr 16) and 0xFF) * 0.4f
                    val pg = ag * 0.6f + ((cc ushr 8) and 0xFF) * 0.4f
                    val pb = ab * 0.6f + (cc and 0xFF) * 0.4f
                    px[py * w + pxx] = (0xFF shl 24) or
                        (cl(((o ushr 16) and 0xFF) * (1f - feather) + pr * feather) shl 16) or
                        (cl(((o ushr 8) and 0xFF) * (1f - feather) + pg * feather) shl 8) or
                        cl((o and 0xFF) * (1f - feather) + pb * feather)
                }
            }
        }
    }

    private fun applyHealSpots(px: IntArray, w: Int, h: Int, spots: List<HealSpot>) {
        val minDim = min(w, h).toFloat()
        spots.forEach { s ->
            healCircle(
                px, w, h,
                (s.x * w).roundToInt().coerceIn(0, w - 1),
                (s.y * h).roundToInt().coerceIn(0, h - 1),
                (s.r * minDim).roundToInt().coerceIn(6, 120)
            )
        }
    }

    private fun applyTapFixes(px: IntArray, w: Int, h: Int, redEyes: List<TapFix>, teeth: List<TapFix>) {
        val minDim = min(w, h).toFloat()
        redEyes.forEach { e ->
            val cx = (e.x * w).roundToInt().coerceIn(0, w - 1)
            val cy = (e.y * h).roundToInt().coerceIn(0, h - 1)
            val rad = (e.r * minDim).roundToInt().coerceIn(4, 80)
            for (py in (cy - rad).coerceAtLeast(0)..(cy + rad).coerceAtMost(h - 1)) {
                for (pxx in (cx - rad).coerceAtLeast(0)..(cx + rad).coerceAtMost(w - 1)) {
                    val d = hypot((pxx - cx).toFloat(), (py - cy).toFloat())
                    if (d < rad) {
                        val i = py * w + pxx
                        val o = px[i]
                        val r = (o ushr 16) and 0xFF
                        val g = (o ushr 8) and 0xFF
                        val b = o and 0xFF
                        if (r > g * 1.3f && r > b * 1.3f && r > 60) {
                            val nr = (g + b) / 2
                            px[i] = (0xFF shl 24) or (nr shl 16) or (g shl 8) or b
                        }
                    }
                }
            }
        }
        teeth.forEach { t ->
            val cx = (t.x * w).roundToInt().coerceIn(0, w - 1)
            val cy = (t.y * h).roundToInt().coerceIn(0, h - 1)
            val rad = (t.r * minDim).roundToInt().coerceIn(4, 80)
            for (py in (cy - rad).coerceAtLeast(0)..(cy + rad).coerceAtMost(h - 1)) {
                for (pxx in (cx - rad).coerceAtLeast(0)..(cx + rad).coerceAtMost(w - 1)) {
                    val d = hypot((pxx - cx).toFloat(), (py - cy).toFloat())
                    if (d < rad) {
                        val i = py * w + pxx
                        val o = px[i]
                        val r = ((o ushr 16) and 0xFF).toFloat()
                        val g = ((o ushr 8) and 0xFF).toFloat()
                        val b = (o and 0xFF).toFloat()
                        // yellowish bright pixels (teeth) → desaturate yellow + brighten
                        if (r > 110 && r > g && g > b && (r - b) > 22) {
                            val gray = 0.3f * r + 0.59f * g + 0.11f * b
                            val br = (gray + 14f).coerceAtMost(255f)
                            val nr2 = r * 0.45f + br * 0.55f
                            val ng2 = g * 0.45f + br * 0.55f
                            val nb2 = b * 0.45f + br * 0.55f
                            px[i] = (0xFF shl 24) or (cl(nr2) shl 16) or (cl(ng2) shl 8) or cl(nb2)
                        }
                    }
                }
            }
        }
    }

    private fun applyBrushStrokes(px: IntArray, w: Int, h: Int, strokes: List<BrushStroke>) {
        val minDim = min(w, h).toFloat()
        strokes.forEach { st ->
            val radPx = (st.radius * minDim).roundToInt().coerceIn(8, 160)
            when (st.mode) {
                com.lumacraft.studio.model.BrushMode.REMOVE -> {
                    st.points.forEach { p ->
                        healCircle(px, w, h, (p.first * w).roundToInt().coerceIn(0, w - 1), (p.second * h).roundToInt().coerceIn(0, h - 1), radPx)
                    }
                }
                com.lumacraft.studio.model.BrushMode.BLUR -> {
                    val xs = st.points.map { (it.first * w).roundToInt() }
                    val ys = st.points.map { (it.second * h).roundToInt() }
                    val x0 = (xs.minOrNull() ?: 0) - radPx; val x1 = (xs.maxOrNull() ?: 0) + radPx
                    val y0 = (ys.minOrNull() ?: 0) - radPx; val y1 = (ys.maxOrNull() ?: 0) + radPx
                    if (x1 - x0 > 4 && y1 - y0 > 4) {
                        val cx0 = x0.coerceIn(0, w - 1); val cx1 = x1.coerceIn(0, w - 1)
                        val cy0 = y0.coerceIn(0, h - 1); val cy1 = y1.coerceIn(0, h - 1)
                        val rw = cx1 - cx0 + 1
                        val rh = cy1 - cy0 + 1
                        val region = IntArray(rw * rh)
                        for (y in 0 until rh) System.arraycopy(px, (cy0 + y) * w + cx0, region, y * rw, rw)
                        val rad = (radPx / 3).coerceIn(1, 10)
                        val diam = rad * 2 + 1
                        val tmp = IntArray(rw * rh)
                        for (y in 0 until rh) {
                            for (x in 0 until rw) {
                                var sr = 0f; var sg = 0f; var sb = 0f; var sa = 0f
                                for (k in -rad..rad) {
                                    val c = region[y * rw + (x + k).coerceIn(0, rw - 1)]
                                    sr += (c ushr 16) and 0xFF; sg += (c ushr 8) and 0xFF; sb += c and 0xFF; sa += (c ushr 24) and 0xFF
                                }
                                tmp[y * rw + x] = ((sa / diam).roundToInt() shl 24) or ((sr / diam).roundToInt() shl 16) or ((sg / diam).roundToInt() shl 8) or (sb / diam).roundToInt()
                            }
                        }
                        val blurred = IntArray(rw * rh)
                        for (y in 0 until rh) {
                            for (x in 0 until rw) {
                                var sr = 0f; var sg = 0f; var sb = 0f; var sa = 0f
                                for (k in -rad..rad) {
                                    val c = tmp[(y + k).coerceIn(0, rh - 1) * rw + x]
                                    sr += (c ushr 16) and 0xFF; sg += (c ushr 8) and 0xFF; sb += c and 0xFF; sa += (c ushr 24) and 0xFF
                                }
                                blurred[y * rw + x] = ((sa / diam).roundToInt() shl 24) or ((sr / diam).roundToInt() shl 16) or ((sg / diam).roundToInt() shl 8) or (sb / diam).roundToInt()
                            }
                        }
                        // blend painted mask
                        for (y in 0 until rh) {
                            for (x in 0 until rw) {
                                val nx = (cx0 + x).toFloat() / w
                                val ny = (cy0 + y).toFloat() / h
                                var minD = Float.MAX_VALUE
                                for (p in st.points) {
                                    minD = min(minD, hypot((nx - p.first) * w, (ny - p.second) * h))
                                }
                                if (minD < radPx * 1.6f) {
                                    val fall = ((1f - minD / (radPx * 1.6f)).coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
                                    if (fall > 0.01f) {
                                        val o = region[y * rw + x]
                                        val bl = blurred[y * rw + x]
                                        region[y * rw + x] =
                                            (o.toInt() and 0xFF000000.toInt()) or
                                            (cl(((o ushr 16) and 0xFF) * (1f - fall) + ((bl ushr 16) and 0xFF) * fall) shl 16) or
                                            (cl(((o ushr 8) and 0xFF) * (1f - fall) + ((bl ushr 8) and 0xFF) * fall) shl 8) or
                                            cl((o and 0xFF) * (1f - fall) + (bl and 0xFF) * fall)
                                    }
                                }
                            }
                        }
                        for (y in 0 until rh) System.arraycopy(region, y * rw, px, (cy0 + y) * w + cx0, rw)
                    }
                }
                com.lumacraft.studio.model.BrushMode.PIXELATE -> {
                    val cell = radPx.coerceAtLeast(4)
                    st.points.forEach { p ->
                        val cx = (p.first * w).roundToInt()
                        val cy = (p.second * h).roundToInt()
                        val x0 = (cx - cell).coerceIn(0, w - 1)
                        val x1 = (cx + cell).coerceIn(0, w - 1)
                        val y0 = (cy - cell).coerceIn(0, h - 1)
                        val y1 = (cy + cell).coerceIn(0, h - 1)
                        var bx = x0
                        while (bx <= x1) {
                            var by = y0
                            while (by <= y1) {
                                val bx1 = min(bx + cell - 1, x1)
                                val by1 = min(by + cell - 1, y1)
                                var sr = 0f; var sg = 0f; var sb = 0f; var cnt = 0f
                                for (y in by..by1) {
                                    for (x in bx..bx1) {
                                        val c = px[y * w + x]
                                        sr += (c ushr 16) and 0xFF; sg += (c ushr 8) and 0xFF; sb += c and 0xFF; cnt++
                                    }
                                }
                                val avg = (0xFF shl 24) or ((sr / cnt).roundToInt() shl 16) or ((sg / cnt).roundToInt() shl 8) or (sb / cnt).roundToInt()
                                for (y in by..by1) for (x in bx..bx1) px[y * w + x] = avg
                                by += cell
                            }
                            bx += cell
                        }
                    }
                }
            }
        }
    }

    private fun applyNoiseReduction(px: IntArray, w: Int, h: Int, strength: Float) {
        val k = strength / 100f
        if (k <= 0.01f) return
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val c = px[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                var sr = 0; var sg = 0; var sb = 0; var n = 0
                val neighbors = intArrayOf(i - 1, i + 1, i - w, i + w, i - w - 1, i - w + 1, i + w - 1, i + w + 1)
                for (j in neighbors) {
                    val nc = px[j]
                    val nr = (nc ushr 16) and 0xFF
                    val ng = (nc ushr 8) and 0xFF
                    val nb = nc and 0xFF
                    val diff = abs(nr - r) + abs(ng - g) + abs(nb - b)
                    // edge-aware: strong edges are preserved
                    if (diff < 96) {
                        sr += nr; sg += ng; sb += nb; n++
                    }
                }
                if (n > 0) {
                    val blend = k * 0.72f
                    val fr = r * (1f - blend) + (sr / n) * blend
                    val fg2 = g * (1f - blend) + (sg / n) * blend
                    val fb = b * (1f - blend) + (sb / n) * blend
                    px[i] = (0xFF shl 24) or (cl(fr) shl 16) or (cl(fg2) shl 8) or cl(fb)
                }
            }
        }
    }

    private fun isSkin(r: Float, g: Float, b: Float): Boolean {
        return r > 55f && g > 28f && b > 14f && r > g && r > b &&
            (r - b) > 10f && (r - g) > 4f && r < 252f
    }

    private fun applySkinSmooth(px: IntArray, w: Int, h: Int, strength: Float) {
        val s = strength / 100f
        if (s <= 0.01f) return
        val dw = (w / 2).coerceAtLeast(1)
        val dh = (h / 2).coerceAtLeast(1)
        val small = FloatArray(dw * dh)
        val smallR = FloatArray(dw * dh)
        val smallG = FloatArray(dw * dh)
        val smallB = FloatArray(dw * dh)
        for (y in 0 until dh) {
            for (x in 0 until dw) {
                val sx = (x * 2).coerceAtMost(w - 1)
                val sy = (y * 2).coerceAtMost(h - 1)
                val c = px[sy * w + sx]
                smallR[y * dw + x] = ((c ushr 16) and 0xFF).toFloat()
                smallG[y * dw + x] = ((c ushr 8) and 0xFF).toFloat()
                smallB[y * dw + x] = (c and 0xFF).toFloat()
            }
        }
        val rad = (2 + (s * 5)).roundToInt()
        val br = blurFloat(smallR, dw, dh, rad)
        val bg = blurFloat(smallG, dw, dh, rad)
        val bb = blurFloat(smallB, dw, dh, rad)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val c = px[i]
                val r = ((c ushr 16) and 0xFF).toFloat()
                val g = ((c ushr 8) and 0xFF).toFloat()
                val b = (c and 0xFF).toFloat()
                if (!isSkin(r, g, b)) continue
                val sx = (x / 2).coerceAtMost(dw - 1)
                val sy = (y / 2).coerceAtMost(dh - 1)
                val sIdx = sy * dw + sx
                val lr = 0.299f * r + 0.587f * g + 0.114f * b
                val lb = 0.299f * br[sIdx] + 0.587f * bg[sIdx] + 0.114f * bb[sIdx]
                val edgeKeep = 1f - min(1f, abs(lr - lb) / 40f)
                val mix = s * edgeKeep * 0.85f
                if (mix > 0.02f) {
                    px[i] = (0xFF shl 24) or
                        (cl(r * (1f - mix) + br[sIdx] * mix) shl 16) or
                        (cl(g * (1f - mix) + bg[sIdx] * mix) shl 8) or
                        cl(b * (1f - mix) + bb[sIdx] * mix)
                }
            }
        }
    }

    private fun blurFloat(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val tmp = FloatArray(w * h)
        val out = FloatArray(w * h)
        val diam = radius * 2 + 1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) sum += src[row + (x + k).coerceIn(0, w - 1)]
                tmp[row + x] = sum / diam
            }
        }
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) sum += tmp[(y + k).coerceIn(0, h - 1) * w + x]
                out[y * w + x] = sum / diam
            }
        }
        return out
    }

    private fun applySharpen(px: IntArray, w: Int, h: Int, strength: Float) {
        val amt = strength / 100f * 0.9f
        if (amt <= 0.02f) return
        val copy = px.copyOf()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val c = copy[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                var sr = 0; var sg = 0; var sb = 0
                for (k in intArrayOf(i - w - 1, i - w, i - w + 1, i - 1, i + 1, i + w - 1, i + w, i + w + 1)) {
                    val n = copy[k]
                    sr += (n ushr 16) and 0xFF; sg += (n ushr 8) and 0xFF; sb += n and 0xFF
                }
                val br = sr / 8f
                val bg2 = sg / 8f
                val bb2 = sb / 8f
                val nr = r + (r - br) * amt
                val ng = g + (g - bg2) * amt
                val nb = b + (b - bb2) * amt
                px[i] = (0xFF shl 24) or (cl(nr) shl 16) or (cl(ng) shl 8) or cl(nb)
            }
        }
    }

    // ------------------------------------------------------------- LUT / mask / WB utilities

    /** Rasterizes painted mask strokes into a quarter-resolution soft-weight grid (0..1). */
    private fun rasterizeMaskGrid(w: Int, h: Int, strokes: List<MaskPaintStroke>): FloatArray? {
        val gw = (w / 4).coerceAtLeast(1)
        val gh = (h / 4).coerceAtLeast(1)
        val grid = FloatArray(gw * gh)
        var any = false
        val minDim = min(w, h).toFloat()
        for (st in strokes) {
            val rad = ((st.radius * minDim) / 4f).coerceIn(1f, 120f)
            for (p in st.points) {
                val cx = p.first * gw
                val cy = p.second * gh
                val x0 = (cx - rad).toInt().coerceIn(0, gw - 1)
                val x1 = (cx + rad).toInt().coerceIn(0, gw - 1)
                val y0 = (cy - rad).toInt().coerceIn(0, gh - 1)
                val y1 = (cy + rad).toInt().coerceIn(0, gh - 1)
                for (yy in y0..y1) {
                    for (xx in x0..x1) {
                        val d = hypot(xx - cx, yy - cy)
                        if (d < rad) {
                            val t = (1f - d / rad).coerceIn(0f, 1f)
                            val fall = t * t * (3f - 2f * t)
                            val i = yy * gw + xx
                            if (st.erase) {
                                grid[i] = (grid[i] - fall).coerceIn(0f, 1f)
                            } else {
                                grid[i] = (grid[i] + fall).coerceIn(0f, 1f)
                                any = true
                            }
                        }
                    }
                }
            }
        }
        return if (any) grid else null
    }

    /**
     * Exports the current color grade as a real Adobe .cube 3D LUT: a neutral
     * RGB cube is rendered through the full grading pipeline, then re-encoded.
     */
    fun exportGradeAsCube(state: EditState, size: Int = 17): String? {
        return try {
            val rows = size * size * size
            val lutState = state.copy(
                geo = GeoState(),
                ai = AiState(cutout = false, bgColor = null, showMask = false),
                lutText = null,
                maskStrokes = emptyList(),
                showMaskOverlay = false
            )
            val src = Bitmap.createBitmap(1, rows, Bitmap.Config.ARGB_8888)
            val px = IntArray(rows)
            for (i in 0 until rows) {
                val r = (i % size).toFloat() / (size - 1)
                val g = ((i / size) % size).toFloat() / (size - 1)
                val b = (i / (size * size)).toFloat() / (size - 1)
                px[i] = (0xFF shl 24) or
                    ((r * 255f).roundToInt() shl 16) or
                    ((g * 255f).roundToInt() shl 8) or
                    (b * 255f).roundToInt()
            }
            src.setPixels(px, 0, 1, 0, 0, 1, rows)
            val out = render(src, lutState, null, rows, overlays = false)
            val outPx = IntArray(rows)
            out.bitmap.getPixels(outPx, 0, 1, 0, 0, 1, rows)
            val sb = StringBuilder(rows * 26 + 64)
            sb.append("TITLE \"LumaCraft Grade\"\n")
            sb.append("LUT_3D_SIZE ").append(size).append("\n\n")
            for (i in 0 until rows) {
                val c = outPx[i]
                val r = ((c ushr 16) and 0xFF) / 255f
                val g = ((c ushr 8) and 0xFF) / 255f
                val b = (c and 0xFF) / 255f
                sb.append(
                    String.format(
                        java.util.Locale.US,
                        "%.6f %.6f %.6f\n", r, g, b
                    )
                )
            }
            src.recycle()
            sb.toString()
        } catch (_: Exception) {
            null
        }
    }

    /** Gray-world auto white balance analysis → (temperature, tint) slider values. */
    fun analyzeWhiteBalance(src: Bitmap): Pair<Float, Float> {
        val small = if (max(src.width, src.height) > 200) {
            Bitmap.createScaledBitmap(src, 150, (150f * src.height / src.width).roundToInt().coerceAtLeast(1), true)
        } else src
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        var sr = 0L; var sg = 0L; var sb = 0L
        for (c in px) {
            sr += (c ushr 16) and 0xFF
            sg += (c ushr 8) and 0xFF
            sb += c and 0xFF
        }
        val n = px.size.coerceAtLeast(1).toFloat()
        val mr = sr / n
        val mg = sg / n
        val mb = sb / n
        val maxC = max(mr, max(mg, mb)).coerceAtLeast(1f)
        val temp = (((mb - mr) / maxC) * 60f).coerceIn(-30f, 30f)
        val tint = ((mg - (mr + mb) / 2f) / maxC * 40f).coerceIn(-20f, 20f)
        return temp to tint
    }

    // ------------------------------------------------------------- main render

    fun render(
        source: Bitmap,
        state: EditState,
        seg: SegMask?,
        maxDim: Int,
        overlays: Boolean = true
    ): ProcessedFrame {
        val aligned = align(source, state.geo, maxDim)
        val w = aligned.width
        val h = aligned.height
        val px = IntArray(w * h)
        aligned.getPixels(px, 0, w, 0, 0, w, h)

        val a = state.adjust
        val ai = state.ai

        // ---- retouch pre-passes
        if (state.healSpots.isNotEmpty()) applyHealSpots(px, w, h, state.healSpots)
        if (state.redEyeSpots.isNotEmpty() || state.teethSpots.isNotEmpty()) {
            applyTapFixes(px, w, h, state.redEyeSpots, state.teethSpots)
        }
        if (state.brushStrokes.isNotEmpty()) applyBrushStrokes(px, w, h, state.brushStrokes)
        if (a.noiseReduction > 0.5f) applyNoiseReduction(px, w, h, a.noiseReduction)
        val skinOn = state.skinSmooth > 0.5f
        if (skinOn) applySkinSmooth(px, w, h, state.skinSmooth)

        // ---- painted selective mask grid (must exist before clarity grids: mask clarity reuses qBlur)
        val maskGrid = if (state.maskStrokes.isNotEmpty()) rasterizeMaskGrid(w, h, state.maskStrokes) else null
        val maskOn = maskGrid != null
        val maskExpGain = 2f.pow(state.maskExposure / 80f)
        val maskWarmK = state.maskTemp * 0.5f
        val maskSatK = state.maskSat / 100f
        val maskClarK = state.maskClarity / 100f * 0.6f

        // ---- clarity & texture grids (allocated only when used)
        val needQ = abs(a.clarity) > 0.5f || (maskOn && abs(state.maskClarity) > 0.5f)
        val needH = abs(a.texture) > 0.5f
        val gw = (w / 4).coerceAtLeast(1)
        val gh = (h / 4).coerceAtLeast(1)
        var qBlur: FloatArray? = null
        var hBlur: FloatArray? = null
        if (needQ || needH) {
            val lumaQ = FloatArray(w * h)
            for (i in px.indices) {
                val c = px[i]
                lumaQ[i] = 0.299f * ((c ushr 16) and 0xFF) + 0.587f * ((c ushr 8) and 0xFF) + 0.114f * (c and 0xFF)
            }
            if (needQ) {
                val qDown = FloatArray(gw * gh)
                for (gy in 0 until gh) {
                    for (gx in 0 until gw) {
                        qDown[gy * gw + gx] = lumaQ[((gy * 4).coerceAtMost(h - 1)) * w + (gx * 4).coerceAtMost(w - 1)]
                    }
                }
                qBlur = blurFloat(qDown, gw, gh, 3)
            }
            if (needH) {
                val hw = (w / 2).coerceAtLeast(1)
                val hh = (h / 2).coerceAtLeast(1)
                val hDown = FloatArray(hw * hh)
                for (hy in 0 until hh) {
                    for (hx in 0 until hw) {
                        hDown[hy * hw + hx] = lumaQ[(hy * 2).coerceAtMost(h - 1) * w + (hx * 2).coerceAtMost(w - 1)]
                    }
                }
                hBlur = blurFloat(hDown, hw, hh, 2)
            }
        }
        val hwRef = (w / 2).coerceAtLeast(1)
        val hhRef = (h / 2).coerceAtLeast(1)

        // ---- main per-pixel loop parameters
        val masterLut = buildSplineLut(state.curves[CurveChannel.MASTER] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f))
        val rLut = buildSplineLut(state.curves[CurveChannel.RED] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f))
        val gLut = buildSplineLut(state.curves[CurveChannel.GREEN] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f))
        val bLut = buildSplineLut(state.curves[CurveChannel.BLUE] ?: listOf(0f, 0.25f, 0.5f, 0.75f, 1f))

        // ---- imported .cube LUT (cached parse)
        val activeLut = state.lutText?.let { LutEngine.parse(it) }
        val lutK = (state.lutIntensity / 100f).coerceIn(0f, 1f)

        val ne = state.naturalEnhance / 100f
        val expGain = 2f.pow(a.exposure / 60f) * (1f + ne * 0.06f)
        val briAdd = a.brightness * 0.5f + ne * 6f
        val contrastK = if (a.contrast >= 0f) 1f + a.contrast / 100f * 0.85f else 1f + a.contrast / 100f * 0.55f
        val hiD = a.highlights * 0.85f
        val shD = a.shadows * 0.85f + ne * 10f
        val whD = a.whites * 0.8f
        val blD = a.blacks * 0.8f
        val fadeLift = a.fade * 0.55f
        val deh = a.dehaze / 100f
        val temp = a.temperature * 0.55f
        val tint = a.tint * 0.45f
        val satScale = (1f + (a.saturation + ne * 10f) / 100f).coerceAtLeast(0f)
        val vibScale = (a.vibrance + ne * 16f) / 100f
        val clarityK = a.clarity / 100f * 0.55f
        val textureK = a.texture / 100f * 0.4f
        val globalHueM = if (abs(a.hueShift) > 0.5f) hueRotateMatrix(a.hueShift) else null

        // HSL zone rotation matrices
        val zoneMats = state.hsl.mapIndexed { i, z ->
            if (abs(z.hue) > 0.5f) i to hueRotateMatrix(z.hue * 0.6f) else null
        }.filterNotNull().toMap()

        val hslActive = state.hsl.any { abs(it.hue) > 0.5f || abs(it.sat) > 0.5f || abs(it.lum) > 0.5f }

        val (shR, shG, shB) = wheelDelta(state.shadowWheel)
        val (midR, midG, midB) = wheelDelta(state.midWheel)
        val (hiR, hiG, hiB) = wheelDelta(state.highWheel)
        val wheelsActive = abs(shR) + abs(shG) + abs(midR) + abs(hiR) > 0.5f

        val splitOn = state.split.shadowSat > 0.5f || state.split.highlightSat > 0.5f
        val shTint = hueTint(state.split.shadowHue)
        val hiTint = hueTint(state.split.highlightHue)
        val splitBal = state.split.balance / 100f

        val aiSelective = abs(ai.subjectPop) > 0.5f || ai.bgDim > 0.5f || ai.bgColor != null || ai.cutout || ai.showMask
        val subGain = 2f.pow(ai.subjectPop / 70f)
        val bgDimF = (1f - ai.bgDim / 100f * 0.75f).coerceIn(0.15f, 1f)
        val bgColInt = ai.bgColor?.toInt() ?: 0

        val vigK = a.vignette / 100f
        val grainK = a.grain / 100f * 34f
        val halfW = w * 0.5f
        val halfH = h * 0.5f
        val invMaxD = 1f / (halfW * halfW + halfH * halfH).coerceAtLeast(1f)

        var idx = 0
        for (y in 0 until h) {
            val ny = y.toFloat() / h
            val dy = y - halfH
            val dySq = dy * dy
            for (x in 0 until w) {
                val nx = x.toFloat() / w
                val c = px[idx]
                var r = ((c ushr 16) and 0xFF).toFloat()
                var g = ((c ushr 8) and 0xFF).toFloat()
                var b = (c and 0xFF).toFloat()

                val fg = if (aiSelective) Segmenter.sampleAt(seg, nx, ny) else 1f

                // AI background replacement
                if (ai.bgColor != null) {
                    val br = ((bgColInt ushr 16) and 0xFF).toFloat()
                    val bg2 = ((bgColInt ushr 8) and 0xFF).toFloat()
                    val bb2 = (bgColInt and 0xFF).toFloat()
                    r = r * fg + br * (1f - fg)
                    g = g * fg + bg2 * (1f - fg)
                    b = b * fg + bb2 * (1f - fg)
                }

                // AI selective subject / background light
                if (abs(ai.subjectPop) > 0.5f || ai.bgDim > 0.5f) {
                    val gain = expGain * (1f + (subGain - 1f) * fg) * (1f - (1f - bgDimF) * (1f - fg))
                    r *= gain; g *= gain; b *= gain
                } else {
                    r *= expGain; g *= expGain; b *= expGain
                }

                r += briAdd + temp + tint * 0.3f
                g += briAdd - tint * 0.6f
                b += briAdd - temp + tint * 0.3f

                // dehaze: black-point subtraction + contrast expansion
                if (deh > 0.01f) {
                    val bp = 16f * deh
                    val expand = 1f + 0.14f * deh
                    r = (r - bp) * expand + 8f * deh
                    g = (g - bp) * expand + 8f * deh
                    b = (b - bp) * expand + 10f * deh
                }

                val lumaN = ((0.299f * r + 0.587f * g + 0.114f * b) / 255f).coerceIn(0f, 1f)
                val shMask = (1f - lumaN) * (1f - lumaN)
                val hiMask = lumaN * lumaN
                val tone = shD * shMask + hiD * hiMask + blD * shMask * (1f - lumaN) + whD * hiMask * lumaN
                r += tone; g += tone; b += tone

                r = (r - 128f) * contrastK + 128f
                g = (g - 128f) * contrastK + 128f
                b = (b - 128f) * contrastK + 128f

                if (fadeLift > 0.5f) {
                    val comp = 1f - fadeLift / 255f
                    r = r * comp + fadeLift
                    g = g * comp + fadeLift
                    b = b * comp + fadeLift
                }

                // global hue rotation
                if (globalHueM != null) {
                    val t3 = applyMatrix(globalHueM, r, g, b)
                    r = t3.first; g = t3.second; b = t3.third
                }

                // saturation & vibrance
                if (satScale != 1f || vibScale != 0f) {
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    val mx = max(r, max(g, b))
                    val mn = min(r, min(g, b))
                    val curSat = if (mx > 1f) (mx - mn) / 255f else 0f
                    val boost = (satScale + vibScale * (1f - min(1f, curSat * 1.4f))).coerceAtLeast(0f)
                    r = gray + (r - gray) * boost
                    g = gray + (g - gray) * boost
                    b = gray + (b - gray) * boost
                }

                if (wheelsActive) {
                    val midW = (1f - abs(lumaN - 0.5f) * 2f).coerceIn(0f, 1f)
                    r += shR * shMask + midR * midW + hiR * hiMask
                    g += shG * shMask + midG * midW + hiG * hiMask
                    b += shB * shMask + midB * midW + hiB * hiMask
                }

                if (splitOn) {
                    val sW = ((1f - lumaN).coerceIn(0f, 1f)).let { it * it } * (state.split.shadowSat / 100f) * 60f * (1f + splitBal * 0.4f)
                    val hW = lumaN * lumaN * (state.split.highlightSat / 100f) * 50f * (1f - splitBal * 0.4f)
                    r += shTint.first * sW + hiTint.first * hW
                    g += shTint.second * sW + hiTint.second * hW
                    b += shTint.third * sW + hiTint.third * hW
                }

                if (hslActive) {
                    val mx = max(r, max(g, b))
                    val mn = min(r, min(g, b))
                    val d = mx - mn
                    if (d > 12f) {
                        val hueDeg = when (mx) {
                            r -> ((g - b) / d + if (g < b) 6f else 0f) * 60f
                            g -> ((b - r) / d + 2f) * 60f
                            else -> ((r - g) / d + 4f) * 60f
                        }
                        var satD = 0f
                        var lumD = 0f
                        var hueZone = -1
                        var zoneW = 0f
                        for (zi in state.hsl.indices) {
                            val z = state.hsl[zi]
                            if (z.sat == 0f && z.lum == 0f && z.hue == 0f) continue
                            var diff = abs(hueDeg - z.centerHue)
                            if (diff > 180f) diff = 360f - diff
                            if (diff < 42f) {
                                val wz = 1f - diff / 42f
                                satD += z.sat / 100f * wz
                                lumD += z.lum * 0.4f * wz
                                if (z.hue != 0f && wz > zoneW) { hueZone = zi; zoneW = wz }
                            }
                        }
                        if (satD != 0f || lumD != 0f) {
                            val gray = 0.299f * r + 0.587f * g + 0.114f * b
                            val sm = (1f + satD).coerceIn(0f, 2.2f)
                            r = gray + (r - gray) * sm + lumD
                            g = gray + (g - gray) * sm + lumD
                            b = gray + (b - gray) * sm + lumD
                        }
                        if (hueZone >= 0) {
                            val t3 = applyMatrix(zoneMats.getValue(hueZone), r, g, b)
                            val wk = zoneW.coerceIn(0f, 1f)
                            r = r * (1f - wk) + t3.first * wk
                            g = g * (1f - wk) + t3.second * wk
                            b = b * (1f - wk) + t3.third * wk
                        }
                    }
                }

                // clarity & texture (local contrast via blurred luma grids)
                if (qBlur != null || hBlur != null) {
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b
                    if (qBlur != null) {
                        val gx = (x / 4).coerceAtMost(gw - 1)
                        val gy = (y / 4).coerceAtMost(gh - 1)
                        val hf = (luma - qBlur[gy * gw + gx]) * clarityK
                        r += hf; g += hf; b += hf
                    }
                    if (hBlur != null) {
                        val hx = (x / 2).coerceAtMost(hwRef - 1)
                        val hy = (y / 2).coerceAtMost(hhRef - 1)
                        val hf = (luma - hBlur[hy * hwRef + hx]) * textureK
                        r += hf; g += hf; b += hf
                    }
                }

                // painted selective mask adjustments (exposure / warmth / saturation / clarity)
                if (maskOn) {
                    val mw = maskGrid!![(y / 4).coerceAtMost(gh - 1) * gw + (x / 4).coerceAtMost(gw - 1)]
                    if (mw > 0.02f) {
                        if (abs(state.maskExposure) > 0.5f) {
                            val mult = 1f + (maskExpGain - 1f) * mw
                            r *= mult; g *= mult; b *= mult
                        }
                        if (abs(maskWarmK) > 0.5f) {
                            r += maskWarmK * mw
                            b -= maskWarmK * mw
                        }
                        if (abs(maskSatK) > 0.01f) {
                            val gray = 0.299f * r + 0.587f * g + 0.114f * b
                            val sm = (1f + maskSatK).coerceAtLeast(0f)
                            r += (gray + (r - gray) * sm - r) * mw
                            g += (gray + (g - gray) * sm - g) * mw
                            b += (gray + (b - gray) * sm - b) * mw
                        }
                        if (abs(maskClarK) > 0.01f && qBlur != null) {
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            val hf = (luma - qBlur[(y / 4).coerceAtMost(gh - 1) * gw + (x / 4).coerceAtMost(gw - 1)]) * maskClarK * mw
                            r += hf; g += hf; b += hf
                        }
                        if (state.showMaskOverlay) {
                            val mixA = (mw * 0.35f).coerceAtMost(0.5f)
                            r = r * (1f - mixA) + 236f * mixA
                            g = g * (1f - mixA) + 110f * mixA
                            b = b * (1f - mixA) + 199f * mixA
                        }
                    }
                }

                // per-channel curves then master curve
                val rr = masterLut[rLut[cl(r)]].toFloat()
                val gg = masterLut[gLut[cl(g)]].toFloat()
                val bb2 = masterLut[bLut[cl(b)]].toFloat()
                r = rr; g = gg; b = bb2

                // imported .cube LUT (trilinear 3D / linear 1D)
                if (activeLut != null) {
                    val t3 = LutEngine.apply(activeLut, r, g, b, lutK)
                    r = t3.first; g = t3.second; b = t3.third
                }

                if (vigK > 0.01f) {
                    val dx = x - halfW
                    val nrm = (dx * dx + dySq) * invMaxD
                    val vf = (1f - vigK * nrm * 0.85f).coerceIn(0.12f, 1f)
                    r *= vf; g *= vf; b *= vf
                }
                if (grainK > 0.5f) {
                    val hash = (((x * 73856093) xor (y * 19349663)) and 255) / 255f - 0.5f
                    val gr = hash * grainK
                    r += gr; g += gr; b += gr
                }

                // AI subject mask visualization
                if (ai.showMask) {
                    val showFg = if (ai.maskInvert) 1f - fg else fg
                    if (showFg > 0.05f) {
                        val mixA = showFg * 0.32f
                        r = r * (1f - mixA) + 60f * mixA
                        g = g * (1f - mixA) + 220f * mixA
                        b = b * (1f - mixA) + 210f * mixA
                    }
                }

                val outAlpha = if (ai.cutout) (fg * 255f).roundToInt().coerceIn(0, 255) else 255
                px[idx] = (outAlpha shl 24) or (cl(r) shl 16) or (cl(g) shl 8) or cl(b)
                idx++
            }
        }

        // ---- AI background bokeh
        if (ai.bgBlur > 2f && !ai.cutout) {
            val dsW = (w / 3).coerceAtLeast(1)
            val dsH = (h / 3).coerceAtLeast(1)
            val sr = FloatArray(dsW * dsH)
            val sg = FloatArray(dsW * dsH)
            val sb = FloatArray(dsW * dsH)
            for (sy in 0 until dsH) {
                for (sx in 0 until dsW) {
                    val c = px[(sy * 3).coerceAtMost(h - 1) * w + (sx * 3).coerceAtMost(w - 1)]
                    sr[sy * dsW + sx] = ((c ushr 16) and 0xFF).toFloat()
                    sg[sy * dsW + sx] = ((c ushr 8) and 0xFF).toFloat()
                    sb[sy * dsW + sx] = (c and 0xFF).toFloat()
                }
            }
            val rad = (1 + ai.bgBlur / 100f * 6f).roundToInt().coerceIn(1, 7)
            val br = blurFloat(sr, dsW, dsH, rad)
            val bg = blurFloat(sg, dsW, dsH, rad)
            val bb = blurFloat(sb, dsW, dsH, rad)
            val strength = ai.bgBlur / 100f
            for (y in 0 until h) {
                val ny = y.toFloat() / h
                val sy = (y / 3).coerceAtMost(dsH - 1)
                for (x in 0 until w) {
                    val nx = x.toFloat() / w
                    val conf = Segmenter.sampleAt(seg, nx, ny)
                    val w8 = ((1f - conf) * strength).coerceIn(0f, 1f)
                    if (w8 > 0.02f) {
                        val sIdx = sy * dsW + (x / 3).coerceAtMost(dsW - 1)
                        val i = y * w + x
                        val o = px[i]
                        px[i] = (0xFF shl 24) or
                            (cl(((o ushr 16) and 0xFF) * (1f - w8) + br[sIdx] * w8) shl 16) or
                            (cl(((o ushr 8) and 0xFF) * (1f - w8) + bg[sIdx] * w8) shl 8) or
                            cl((o and 0xFF) * (1f - w8) + bb[sIdx] * w8)
                    }
                }
            }
        }

        // ---- sharpen (post)
        if (a.sharpness > 0.5f) applySharpen(px, w, h, a.sharpness)

        val core = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)

        // ---- histogram (before overlays)
        val hist = IntArray(32)
        var clipS = 0
        var clipH = 0
        var samples = 0
        var i2 = 0
        while (i2 < px.size) {
            val c = px[i2]
            val lv = (0.299f * ((c ushr 16) and 0xFF) + 0.587f * ((c ushr 8) and 0xFF) + 0.114f * (c and 0xFF)).toInt().coerceIn(0, 255)
            hist[(lv * 31) / 255]++
            if (lv <= 4) clipS++
            if (lv >= 251) clipH++
            samples++
            i2 += 5
        }

        val final = if (overlays) bakeOverlays(core, state) else core
        return ProcessedFrame(final, hist, clipS > samples * 0.03f, clipH > samples * 0.03f)
    }

    // ------------------------------------------------------------- overlays & frames

    fun bakeOverlays(base: Bitmap, state: EditState): Bitmap {
        var out = base
        // frames first (border around content)
        if (state.frame != com.lumacraft.studio.model.FrameStyle.NONE) {
            out = applyFrame(out, state.frame)
        }
        val strokes = state.drawStrokes
        val texts = state.textLayers
        val emojis = state.emojiLayers
        val wm = state.watermark
        if (strokes.isEmpty() && texts.isEmpty() && emojis.isEmpty() && wm.isEmpty()) return out

        val result = out.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val minDim = min(result.width, result.height).toFloat()

        if (strokes.isNotEmpty()) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
            strokes.forEach { st ->
                paint.color = st.color.toInt()
                paint.strokeWidth = max(2f, st.widthRel * minDim)
                if (st.points.size >= 2) {
                    val path = Path()
                    path.moveTo(st.points[0].first * result.width, st.points[0].second * result.height)
                    for (i in 1 until st.points.size) {
                        path.lineTo(st.points[i].first * result.width, st.points[i].second * result.height)
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        texts.forEach { t ->
            textPaint.color = ((t.opacity.coerceIn(0f, 1f) * 255f).roundToInt() shl 24) or (t.color and 0xFFFFFF).toInt()
            val baseTf = AVAILABLE_FONTS.getOrElse(t.fontIdx) { "Sans" to Typeface.DEFAULT }.second
            textPaint.typeface = Typeface.create(baseTf, if (t.bold && t.italic) Typeface.BOLD_ITALIC else if (t.bold) Typeface.BOLD else if (t.italic) Typeface.ITALIC else Typeface.NORMAL)
            textPaint.textSize = max(8f, t.sizeRel * result.width)
            textPaint.setShadowLayer(textPaint.textSize * 0.08f, 0f, 0f, 0x5A000000)
            if (t.centered) textPaint.textAlign = Paint.Align.CENTER else textPaint.textAlign = Paint.Align.LEFT
            val yPos = t.y * result.height + textPaint.textSize * 0.35f
            canvas.drawText(t.text, t.x * result.width, yPos, textPaint)
        }

        if (emojis.isNotEmpty()) {
            val ep = Paint(Paint.ANTI_ALIAS_FLAG)
            emojis.forEach { e ->
                ep.textSize = max(10f, e.sizeRel * result.width)
                val yPos = e.y * result.height + ep.textSize * 0.35f
                canvas.drawText(e.emoji, e.x * result.width, yPos, ep)
            }
        }

        if (wm.isNotEmpty()) {
            val wp = Paint(Paint.ANTI_ALIAS_FLAG)
            wp.color = 0xB3FFFFFF.toInt()
            wp.textSize = max(10f, result.width * 0.028f)
            wp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(wm, result.width - wp.textSize * 0.4f - wp.measureText(wm), result.height - wp.textSize * 0.9f, wp)
        }
        return result
    }

    private fun applyFrame(base: Bitmap, style: com.lumacraft.studio.model.FrameStyle): Bitmap {
        val minSide = min(base.width, base.height)
        val pad = when (style) {
            com.lumacraft.studio.model.FrameStyle.WHITE, com.lumacraft.studio.model.FrameStyle.DARK -> (minSide * 0.05f).roundToInt().coerceAtLeast(10)
            com.lumacraft.studio.model.FrameStyle.POLAROID -> (minSide * 0.06f).roundToInt().coerceAtLeast(12)
            else -> 0
        }
        val bottomExtra = if (style == com.lumacraft.studio.model.FrameStyle.POLAROID) (minSide * 0.14f).roundToInt().coerceAtLeast(30) else 0
        val outW = base.width + pad * 2
        val outH = base.height + pad * 2 + bottomExtra
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = when (style) {
            com.lumacraft.studio.model.FrameStyle.WHITE -> 0xFFF8F7F4.toInt()
            com.lumacraft.studio.model.FrameStyle.DARK -> 0xFF101114.toInt()
            com.lumacraft.studio.model.FrameStyle.POLAROID -> 0xFFF8F7F4.toInt()
            else -> 0xFF000000.toInt()
        }
        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), paint)
        canvas.drawBitmap(base, pad.toFloat(), pad.toFloat(), null)
        return out
    }

    // ------------------------------------------------------------- export

    fun resolutionMaxDim(mode: String, sourceMax: Int): Int = when (mode) {
        "2048 px" -> 2048
        "1080 px" -> 1080
        "Square 1080" -> 1080
        else -> maxDimFor(sourceMax)
    }

    private fun maxDimFor(sourceMax: Int) = sourceMax.coerceIn(720, 4096)

    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        stripExif: Boolean,
        shareAfter: Boolean,
        cameraInfo: String
    ): Uri? {
        val filename = "LumaCraft_${System.currentTimeMillis()}.${format.ext}"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LumaCraft")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { os ->
                val cf = when (format) {
                    ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(cf, quality.coerceIn(1, 100), os)
            }
            if (!stripExif && format == ExportFormat.JPEG) {
                try {
                    resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        val exif = ExifInterface(pfd.fileDescriptor)
                        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "LumaCraft Photo Studio")
                        if (cameraInfo.isNotBlank()) exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, cameraInfo)
                        exif.saveAttributes()
                    }
                } catch (_: Exception) {
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            if (shareAfter) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = format.mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share photo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun renderForExport(source: Bitmap, state: EditState, seg: SegMask?, options: ExportOptions, overlays: Boolean = true): Bitmap {
        val base = maxDimFor(max(source.width, source.height))
        val target = resolutionMaxDim(options.resolutionMode, base)
        var frame = render(source, state, seg, target, overlays)
        var bmp = frame.bitmap
        if (options.upscale2x && bmp.width.toLong() * bmp.height.toLong() * 4L <= 36_000_000L) {
            bmp = upscale2x(bmp)
        }
        return bmp
    }

    fun upscale2x(bitmap: Bitmap): Bitmap {
        val up = Bitmap.createScaledBitmap(bitmap, bitmap.width * 2, bitmap.height * 2, true)
        // smart sharpen after upscale
        val w = up.width
        val h = up.height
        val px = IntArray(w * h)
        up.getPixels(px, 0, w, 0, 0, w, h)
        applySharpen(px, w, h, 42f)
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    // ------------------------------------------------------------- auto enhance analysis

    fun autoEnhance(preview: Bitmap): Adjustments {
        val small = if (max(preview.width, preview.height) > 200) {
            Bitmap.createScaledBitmap(preview, 150, (150f * preview.height / preview.width).roundToInt().coerceAtLeast(1), true)
        } else preview
        val w = small.width
        val h = small.height
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        val hist = IntArray(256)
        var satSum = 0f
        for (c in px) {
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            hist[(0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)]++
            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            satSum += if (mx > 0) (mx - mn).toFloat() / mx else 0f
        }
        val total = px.size
        var acc = 0
        var p2 = 0
        var p98 = 255
        for (i in 0..255) {
            acc += hist[i]
            if (acc >= total * 0.02f) { p2 = i; break }
        }
        acc = 0
        for (i in 255 downTo 0) {
            acc += hist[i]
            if (acc >= total * 0.02f) { p98 = i; break }
        }
        var mean = 0f
        for (i in 0..255) mean += hist[i] * i
        mean /= total
        val meanSat = satSum / total
        val exposure = ((118f - mean) * 0.45f).coerceIn(-25f, 30f)
        val contrast = ((p98 - p2) < 190).let { if (it) 16f else 8f }
        val shadows = if (p2 < 18) 18f else 8f
        val whites = if (p98 > 240) 0f else 10f
        val blacks = if (p2 > 40) -10f else 0f
        val vibrance = ((0.36f - meanSat) * 90f).coerceIn(0f, 26f)
        val sharpness = 14f
        return Adjustments(
            exposure = exposure, contrast = contrast, shadows = shadows, whites = whites,
            blacks = blacks, vibrance = vibrance, sharpness = sharpness
        )
    }

    // ------------------------------------------------------------- collage compose

    fun composeCollage(context: Context, config: CollageConfig, templateCells: List<RectF>): Bitmap? {
        if (config.photos.isEmpty() || templateCells.isEmpty()) return null
        val aspect = when (config.aspect) {
            "4:5" -> 0.8f; "3:4" -> 0.75f; "2:3" -> 2f / 3f
            "4:3" -> 4f / 3f; "3:2" -> 1.5f; "16:9" -> 16f / 9f
            else -> 1f
        }
        val long = 2200
        val cw: Int
        val ch: Int
        if (aspect >= 1f) { cw = long; ch = (long / aspect).roundToInt() } else { ch = long; cw = (long * aspect).roundToInt() }
        val out = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(config.bgColor.toInt())

        val spacing = config.spacingRel * min(cw, ch)
        val corner = config.cornerRel * min(cw, ch)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        templateCells.forEachIndexed { i, cell ->
            val uri = config.photos[i % config.photos.size]
            val cellW = (cell.width() * cw).roundToInt()
            val cellH = (cell.height() * ch).roundToInt()
            if (cellW < 8 || cellH < 8) return@forEachIndexed
            val bmp = decodeForCell(context, uri, cellW, cellH) ?: return@forEachIndexed

            // center-crop + user pan offset
            val off = config.cellOffsets[i] ?: (0f to 0f)
            val scale = max(cellW.toFloat() / bmp.width, cellH.toFloat() / bmp.height)
            val dw = (bmp.width * scale).roundToInt()
            val dh = (bmp.height * scale).roundToInt()
            val panX = off.first * (dw - cellW).coerceAtLeast(0)
            val panY = off.second * (dh - cellH).coerceAtLeast(0)
            val dx = (cell.left * cw + spacing / 2f + (cellW - dw) / 2f - panX).roundToInt()
            val dy = (cell.top * ch + spacing / 2f + (cellH - dh) / 2f - panY).roundToInt()

            val inset = spacing / 2f
            val rect = RectF(cell.left * cw + inset, cell.top * ch + inset, cell.right * cw - inset, cell.bottom * ch - inset)
            canvas.save()
            if (corner > 0f) {
                val path = Path()
                path.addRoundRect(rect, corner, corner, Path.Direction.CW)
                canvas.clipPath(path)
            } else {
                canvas.clipRect(rect)
            }
            canvas.drawBitmap(bmp, dx.toFloat(), dy.toFloat(), paint)
            canvas.restore()
            if (bmp !== out) bmp.recycle()
        }

        if (config.caption.isNotBlank()) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG)
            tp.color = 0xEEFFFFFF.toInt()
            tp.textSize = cw * 0.035f
            tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            tp.textAlign = Paint.Align.CENTER
            canvas.drawText(config.caption, cw / 2f, ch - cw * 0.03f, tp)
        }
        return out
    }

    private fun decodeForCell(context: Context, uri: Uri, cellW: Int, cellH: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > cellW * 2 && bounds.outHeight / sample > cellH * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) {
            null
        }
    }
}
