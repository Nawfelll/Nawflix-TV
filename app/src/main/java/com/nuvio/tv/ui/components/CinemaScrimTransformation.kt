package com.nuvio.tv.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Bakes the home screen's contrast gradients into the backdrop bitmap, once, at decode time.
 *
 * Drawing two full-screen translucent gradients over a full-screen image every frame is what saturates
 * the GPU of a low-end TV (the screen is blended 5+ times). With the scrim baked in, the GPU draws a
 * single opaque image. The bitmap is also capped to [maxWidth] and can use RGB565 to halve its memory.
 */
class CinemaScrimTransformation(
    private val maxWidth: Int,
    private val rgb565: Boolean
) : Transformation() {

    override val cacheKey: String = "cinema_scrim_v2_${maxWidth}_$rgb565"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val outW = minOf(input.width, maxWidth)
        val outH = (input.height.toLong() * outW / input.width).toInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(
            outW, outH, if (rgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(out)
        val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(input, Rect(0, 0, input.width, input.height), Rect(0, 0, outW, outH), imagePaint)

        val w = outW.toFloat()
        val h = outH.toFloat()
        val shadePaint = Paint(Paint.DITHER_FLAG)

        // Left-to-right: dark behind the hero text, fading to the artwork on the right.
        shadePaint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(black(.88f), black(.56f), black(.08f), 0x00000000),
            floatArrayOf(0f, .5f, .75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, shadePaint)

        // Top-to-bottom: light veil under the top bar, then melts into the page colour behind the shelves.
        shadePaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(black(.45f), 0x00000000, black(.18f), 0xD208090C.toInt(), 0xFF08090C.toInt()),
            floatArrayOf(0f, .36f, .56f, .78f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, shadePaint)
        return out
    }

    private fun black(alpha: Float): Int = ((alpha * 255f + .5f).toInt() shl 24)
}
