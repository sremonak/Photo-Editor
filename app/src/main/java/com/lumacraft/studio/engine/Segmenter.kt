package com.lumacraft.studio.engine

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.tasks.await

/** Per-pixel foreground confidence (0f = background, 1f = subject). */
class SegMask(val width: Int, val height: Int, val confidence: FloatArray)

/**
 * On-device ML Kit Selfie Segmentation — the model is bundled inside the APK,
 * runs 100% offline, and requires no network permission.
 */
object Segmenter {

    private val segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
        Segmentation.getClient(options)
    }

    suspend fun segment(bitmap: Bitmap): SegMask? = try {
        val input = InputImage.fromBitmap(
            if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
            else bitmap.copy(Bitmap.Config.ARGB_8888, false),
            0
        )
        val mask = segmenter.process(input).await()
        val mw = mask.width.coerceAtLeast(1)
        val mh = mask.height.coerceAtLeast(1)
        val buf = mask.buffer
        buf.rewind()
        val out = FloatArray(mw * mh)
        for (i in out.indices) {
            if (buf.hasRemaining()) out[i] = buf.float.coerceIn(0f, 1f)
        }
        buf.rewind()
        SegMask(mw, mh, out)
    } catch (_: Exception) {
        null
    }

    fun sampleAt(mask: SegMask?, nx: Float, ny: Float): Float {
        if (mask == null || mask.width <= 0 || mask.height <= 0) return 0.5f
        val sx = (nx * (mask.width - 1)).toInt().coerceIn(0, mask.width - 1)
        val sy = (ny * (mask.height - 1)).toInt().coerceIn(0, mask.height - 1)
        return mask.confidence[sy * mask.width + sx]
    }
}
