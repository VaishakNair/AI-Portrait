package `in`.v89bhp.imagesegmenter.helpers

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.nio.ByteBuffer


class SisrHelper(
    private val coroutineDispatcher: CoroutineDispatcher
) {

    fun preprocessImage(image: Bitmap): TensorImage {
        val targetHeight = Math.floorDiv(image.height, 4) * 4
        val targetWidth = Math.floorDiv(image.width, 4) * 4
        val imageProcessor =
            ImageProcessor.Builder()
                .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build()
        // The alpha channel will be discarded
        // when the image gets converted into a TensorImage (TensorImage.fromBitmap(image)) below:
        // Preprocess the image and convert it into a TensorImage for segmentation.
        return imageProcessor.process(TensorImage.fromBitmap(image))
    }

    suspend fun enhanceResolution(image: Bitmap): ImageBitmap {
        return withContext(coroutineDispatcher) {
            val preprocessedTensorImage = preprocessImage(image)
            val outputHeight = preprocessedTensorImage.height * 4
            val outputWidth = preprocessedTensorImage.width * 4
            val input = preprocessedTensorImage.tensorBuffer.buffer
            val output = ByteBuffer.allocate(input.capacity() * 4 * 4)

            Interpreter(File("file:///android_asset/sisr.tflite")).use { interpreter ->
                interpreter.run(
                    input,
                    output
                )
            }
            getOutputImage(output, outputHeight, outputWidth).asImageBitmap()

        }
    }

    private fun getOutputImage(output: ByteBuffer, height: Int, width: Int): Bitmap {
        output.rewind()
        val outputWidth = width
        val outputHeight = height
        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outputWidth * outputHeight)
        for (i in 0 until (outputWidth * outputHeight)) {
            val a = 0xFF
            // TODO May need to remove the * 255.0f part
            val r = output.float * 255.0f
            val g = output.float * 255.0f
            val b = output.float * 255.0f
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
        return bitmap
    }
}