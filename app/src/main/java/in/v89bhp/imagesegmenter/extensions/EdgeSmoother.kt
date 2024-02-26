package `in`.v89bhp.imagesegmenter.extensions

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.get
import androidx.core.graphics.set

// Size of blurring filter. FILTER_SIZE x FILTER_SIZE filter will be created:
private const val FILTER_SIZE = 9
// How many pixels starting at the edge pixel should be smoothened horizontally:
private const val STRIDE = 10

fun Bitmap.smoothenTransparentEdges(): Bitmap {
    var smoothenedBitmap = this.copy(this.config, true)
    var maskedBitmap = this.copy(this.config, true)

    for (i in 0 until 2) { // Two iterations for smoothening from left to right and from right to left (by rotating the image 180 degrees)
        for (rowIndex in 0 until maskedBitmap.height) {
            for (columnIndex in 0 until maskedBitmap.width) {
                val maskValue = maskedBitmap[columnIndex, rowIndex]
                if (maskValue == Color.TRANSPARENT) {
                    try {
                        if (maskedBitmap[columnIndex + 1, rowIndex] != Color.TRANSPARENT) {
                            // Edge detected. Do smoothing:
                            for (i in 1..STRIDE) {
                                try {
                                    val pixels = Array(FILTER_SIZE * FILTER_SIZE) { 0 }.toIntArray()
                                    maskedBitmap.getPixels(
                                        pixels,
                                        0,
                                        FILTER_SIZE,
                                        columnIndex + i,
                                        rowIndex - 1,
                                        FILTER_SIZE,
                                        FILTER_SIZE
                                    )
                                    smoothenedBitmap[columnIndex + i, rowIndex] =
                                        getSmoothenedPixelValue(pixels)
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
        smoothenedBitmap = smoothenedBitmap.rotate(180f)
        maskedBitmap = maskedBitmap.rotate(180f)
    }

    return smoothenedBitmap
}

fun getSmoothenedPixelValue(pixels: IntArray): Int {
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