package `in`.v89bhp.imagesegmenter.extensions

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.get
import androidx.core.graphics.set

// Size of blurring filter. FILTER_SIZE x FILTER_SIZE filter will be created:
private const val FILTER_SIZE = 5

fun Bitmap.smoothenTransparentEdges(): Bitmap {
    val smoothedBitmap = this.copy(this.config, true)
    for (rowIndex in 0 until this.height) {
        for (columnIndex in 0 until this.width) {
            val maskValue = this[columnIndex, rowIndex]
            if (maskValue == Color.TRANSPARENT) {
                try {
                    if (this[columnIndex + 1, rowIndex] != Color.TRANSPARENT) {
                        // TODO Edge detected. Do smoothing

                        for (i in 1..20) {
                            try {
                                val pixels = Array(FILTER_SIZE * FILTER_SIZE) { 0 }.toIntArray()
                                this.getPixels(
                                    pixels,
                                    0,
                                    FILTER_SIZE,
                                    columnIndex + i,
                                    rowIndex - 1,
                                    FILTER_SIZE,
                                    FILTER_SIZE
                                )
                                smoothedBitmap[columnIndex + i, rowIndex] =
                                    getSmoothedPixelValue(pixels)
                            } catch (e: IllegalArgumentException) {
                                // Do nothing
                            }
                        }

                    }
                } catch (e: IllegalArgumentException) {
                    // Do nothing
                }
            }
        }
    }

    return smoothedBitmap
}

fun getSmoothedPixelValue(pixels: IntArray): Int {
    val alphas = mutableListOf<Int>()
    val reds = mutableListOf<Int>()
    val greens = mutableListOf<Int>()
    val blues = mutableListOf<Int>()
    for (pixel in pixels) {
        alphas.add(Color.alpha(pixel))
        reds.add(Color.red(pixel))
        greens.add(Color.green(pixel))
        blues.add(Color.blue(pixel))
    }
    return Color.argb(
        alphas.average().toInt(),
        reds.average().toInt(),
        greens.average().toInt(),
        blues.average().toInt()
    )
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}