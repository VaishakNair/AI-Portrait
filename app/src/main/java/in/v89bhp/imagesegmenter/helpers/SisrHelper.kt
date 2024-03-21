package `in`.v89bhp.imagesegmenter.helpers

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Arrays
import kotlin.math.roundToInt


class SisrHelper(
    private val coroutineDispatcher: CoroutineDispatcher
) {

    private lateinit var context: Context

    fun setup(context: Context) {
        this.context = context
    }

    fun preprocessImage(image: Bitmap): TensorImage {
        val targetHeight = Math.floorDiv(image.height, 4) * 4
        val targetWidth = Math.floorDiv(image.width, 4) * 4
        val imageProcessor =
            ImageProcessor.Builder()
//                .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(ResizeOp(300, 200, ResizeOp.ResizeMethod.BILINEAR))
                .build()
        // The alpha channel will be discarded
        // when the image gets converted into a TensorImage (TensorImage.fromBitmap(image)) below:
        // Preprocess the image and convert it into a TensorImage for segmentation.
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(image)
        return imageProcessor.process(tensorImage)
    }

    /** Memory-map the model file in Assets.  */

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("sr.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

//    suspend fun enhanceResolution(image: Bitmap): ImageBitmap {
//        return withContext(coroutineDispatcher) {
//            val preprocessedTensorImage = preprocessImage(image)
//            val outputHeight = preprocessedTensorImage.height * 4
//            val outputWidth = preprocessedTensorImage.width * 4
//            val input = preprocessedTensorImage.tensorBuffer.buffer
//            val output = ByteBuffer.allocate(input.capacity() * 4 * 4)
//
//            Interpreter(loadModelFile()).use { interpreter ->
//                interpreter.run(
//                    input,
//                    output
//                )
//            }
//            getOutputImage(output, outputHeight, outputWidth).asImageBitmap()
//
//        }
//    }

    suspend fun enhanceResolution(image: Bitmap): ImageBitmap {
        return withContext(coroutineDispatcher) {
            val preprocessedTensorImage = preprocessImage(image)
            val outputHeight = preprocessedTensorImage.height * 4
            val outputWidth = preprocessedTensorImage.width * 4
            val input = preprocessedTensorImage.tensorBuffer.buffer
//            val output = ByteBuffer.allocate(input.capacity() * 4 * 4)
            val output =
                TensorBuffer.createFixedSize(intArrayOf(1200, 800, 3), DataType.FLOAT32)
            Interpreter(loadModelFile()).use { interpreter ->
                interpreter.run(
                    input,
                    output.buffer

                )
            }
            val outBitmap = Bitmap.createBitmap(
                800,
                1200, Bitmap.Config.ARGB_8888
            )
//            convertTensorBufferToBitmap(output,outBitmap)
//            outBitmap.asImageBitmap()
            getOutputImage(output, outputHeight, outputWidth).asImageBitmap()

        }
    }

    fun convertTensorBufferToBitmap(buffer: TensorBuffer, bitmap: Bitmap) {
//        if (buffer.dataType != DataType.UINT8) {
//            // We will add support to FLOAT format conversion in the future, as it may need other configs.
//            throw UnsupportedOperationException(
//                String.format(
//                    "Converting TensorBuffer of type %s to Bitmap is not supported yet.",
//                    buffer.dataType
//                )
//            )
//        }
        val shape = buffer.shape
        require(shape.size == 3 && shape[0] > 0 && shape[1] > 0 && shape[2] == 3) {
            String.format(
                "Buffer shape %s is not valid. 3D TensorBuffer with shape [w, h, 3] is required",
                Arrays.toString(shape)
            )
        }
        val h = shape[0]
        val w = shape[1]
        require(!(bitmap.width != w || bitmap.height != h)) {
            String.format(
                "Given bitmap has different width or height %s with the expected ones %s.",
                Arrays.toString(intArrayOf(bitmap.width, bitmap.height)),
                Arrays.toString(intArrayOf(w, h))
            )
        }
        require(bitmap.isMutable) { "Given bitmap is not mutable" }
        // TODO(b/138904567): Find a way to avoid creating multiple intermediate buffers every time.
        val intValues = IntArray(w * h)
        val rgbValues = buffer.floatArray
        var i = 0
        var j = 0
        while (i < intValues.size) {
            val r = rgbValues[j++].coerceIn(0f, 255f).toInt().toByte()
            val g = rgbValues[j++].coerceIn(0f, 255f).toInt().toByte()
            val b = rgbValues[j++].coerceIn(0f, 255f).toInt().toByte()
            intValues[i] = r.toInt() shl 16 or (g.toInt() shl 8) or b.toInt()
            i++
        }
        bitmap.setPixels(intValues, 0, w, 0, 0, w, h)
    }

    private fun getOutputImageBackup(output: ByteBuffer, height: Int, width: Int): Bitmap {
        output.rewind()
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val pixels = IntArray(width * height)
        val minValue = 0f
        val maxValue = 255f
        for (i in 0 until (width * height)) {
            val a = 0xFF
            // TODO May need to remove the * 255.0f part
//            val r = output.float * 255.0f
//            val g = output.float * 255.0f
//            val b = output.float * 255.0f
            val r = output.float.coerceIn(minValue, maxValue)
            val g = output.float.coerceIn(minValue, maxValue)
            val b = output.float.coerceIn(minValue, maxValue)
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun getOutputImage(output: TensorBuffer, height: Int, width: Int): Bitmap {
//        output.rewind()
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val pixels = IntArray(width * height)
        val minValue = 0f
        val maxValue = 255f
        val rgbValues = output.floatArray
        var j = 0
        val a = 0xFF
        for (i in 0 until (width * height)) {
            // TODO May need to remove the * 255.0f part
//            val r = output.float * 255.0f
//            val g = output.float * 255.0f
//            val b = output.float * 255.0f
            val r = rgbValues[j++].coerceIn(minValue, maxValue).roundToInt().toByte()
            val g = rgbValues[j++].coerceIn(minValue, maxValue).roundToInt().toByte()
            val b = rgbValues[j++].coerceIn(minValue, maxValue).roundToInt().toByte()
            pixels[i] = (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}