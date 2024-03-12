package `in`.v89bhp.imagesegmenter.helpers

import android.R.id.input
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

    suspend fun enhanceResolution(image: Bitmap): Bitmap {
        withContext(coroutineDispatcher) {
            val preprocessedTensorImage = preprocessImage(image)
            val input = preprocessedTensorImage.tensorBuffer.buffer
            val output = ByteBuffer.allocate(input.capacity() * 4 * 4)

            Interpreter(File("file:///android_asset/sisr.tflite")).use { interpreter ->
                interpreter.run(
                    input,
                    output
                )
            }

        }
    }
}