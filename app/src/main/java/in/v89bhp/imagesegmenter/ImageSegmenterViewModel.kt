package `in`.v89bhp.imagesegmenter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class ImageSegmenterViewModel(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val start: CoroutineStart = CoroutineStart.DEFAULT,
    private val imageSegmentationHelper: ImageSegmentationHelper = ImageSegmentationHelper(
        coroutineDispatcher = Dispatchers.IO
    )
) : ViewModel() {

    var imageBitmap: ImageBitmap? by mutableStateOf(null)

    var loadingImage by mutableStateOf(false)

    var isProcessing by mutableStateOf(false)

    var imageLoaded by mutableStateOf(false)

    var backgroundRemoved by mutableStateOf(false)

    var imageConfiguration = ""

    var imageSize = ""

    var colorSpace = ""


    var modelMetadata by mutableStateOf("")


    private val onError: (errorMessage: String) -> Unit = { errorMessage ->
        TODO("Not yet implemented")
    }


    fun loadModelMetadata(context: Context) { // TODO
        modelMetadata = "To be computed"
    }

    fun loadImage(context: Context, imageUri: Uri) {
        backgroundRemoved = false
        loadingImage = true
        val source: ImageDecoder.Source =
            ImageDecoder.createSource(context.contentResolver, imageUri)
        imageBitmap =
            ImageDecoder.decodeBitmap(source).let { bitmap -> // Convert to ARGB_8888 format:
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }.asImageBitmap().also {
                imageConfiguration = it.config.toString()
                imageSize = "${it.width} x ${it.height}"
                colorSpace = it.colorSpace.toString()
            }
        loadingImage = false
        imageLoaded = true
    }

    fun removeBackground() {
        // Run in a IO dispatcher as a coroutine:
        viewModelScope.launch(
            context = coroutineDispatcher,
            start = start
        ) {
            isProcessing = true
            val segmentationResult =
                imageSegmentationHelper.segment(imageBitmap!!.asAndroidBitmap(), 0)
            Log.i(TAG, "Segmented region count: ${segmentationResult.results?.size ?: 0}")

            if (!segmentationResult.results.isNullOrEmpty()) {
                val segmentation = segmentationResult.results[0]

                val confidenceMaskTensor =
                    segmentation.masks[0] // A single confidence mask with each pixel value corresponding
                // to the probability of the pixel being background (close to 0.0) or foreground (close to 1.0)
                Log.i(
                    TAG,
                    "Confidence mask tensor width x height: ${confidenceMaskTensor.width} x ${confidenceMaskTensor.height}"
                )
                val categoryMaskArray = confidenceMaskTensor.tensorBuffer.floatArray
                Log.i(TAG, "Confidence mask array size: ${categoryMaskArray.size}")

                val pixels = IntArray(categoryMaskArray.size)

                for (i in categoryMaskArray.indices) {
                    pixels[i] =
                        if (categoryMaskArray[i] < 0.6) Color.TRANSPARENT else Color.RED  // TODO Modify the threshold as needed.
                }

                val imageMask = Bitmap.createBitmap(
                    pixels,
                    confidenceMaskTensor.width,
                    confidenceMaskTensor.height,
                    Bitmap.Config.ARGB_8888
                )


                // PreviewView is in FILL_START mode. So we need to scale up the bounding
                // box to match with the size that the captured images will be displayed.
//                val scaleFactor = max(width * 1f / segmentationResult.imageWidth, height * 1f / segmentationResult.imageHeight)
                val scaleWidth = (segmentationResult.imageWidth * 1f).toInt()
                val scaleHeight = (segmentationResult.imageHeight * 1f).toInt()

                val scaledImageMask =
                    Bitmap.createScaledBitmap(imageMask, scaleWidth, scaleHeight, true)

                imageBitmap = applyMask(scaledImageMask)

                isProcessing = false
                backgroundRemoved = true
            }
        }

    }

    private suspend fun applyMask(scaledImageMask: Bitmap) = withContext(Dispatchers.IO) {
        var outputBitmap =
            imageBitmap!!.asAndroidBitmap().let {// Make a mutable copy of the input bitmap.
                it.copy(it.config, true)
            }

        for (i in 0 until scaledImageMask.width) {
            for (j in 0 until scaledImageMask.height) {
                val maskValue = scaledImageMask[i, j]
                if (maskValue == Color.TRANSPARENT) {
                    outputBitmap[i, j] = maskValue
                }
            }
        }
        outputBitmap = smoothEdges(outputBitmap)
        outputBitmap.asImageBitmap()
    }

//    fun smoothEdges(outputBitmap: Bitmap): Bitmap {
//        val smoothedBitmap = outputBitmap.copy(outputBitmap.config, true)
//        val a: Long = 0xff000f00
//        Log.i(TAG, Color.green(a.toInt()).toString())
//        for (rowIndex in 0 until outputBitmap.height) {
//            for (columnIndex in 0 until outputBitmap.width) {
//                val maskValue = outputBitmap[columnIndex, rowIndex]
//                if (maskValue == Color.TRANSPARENT) {
//                    try {
//                        if (outputBitmap[columnIndex + 1, rowIndex] != Color.TRANSPARENT) {
//                            // TODO Edge detected. Do smoothing
//                            val pixels = arrayOf(9).toIntArray()
//                            outputBitmap.getPixels(pixels, 0, 3, columnIndex + 1, rowIndex -1, 3, 3)
//                        }
//                    } catch (e: IllegalArgumentException) {
//                        // Do nothing
//                    }
//
////                    outputBitmap[columnIndex, rowIndex] = maskValue
//                }
//            }
//        }
//
//        return smoothedBitmap
//    }

    fun smoothEdges(outputBitmap: Bitmap): Bitmap {
        val smoothedBitmap = outputBitmap.copy(outputBitmap.config, true)
//        val a: Long = 0xff000f00
//        Log.i(TAG, Color.green(a.toInt()).toString())
        for (rowIndex in 0 until outputBitmap.height) {
            for (columnIndex in 0 until outputBitmap.width) {
                val maskValue = outputBitmap[columnIndex, rowIndex]
                if (maskValue == Color.TRANSPARENT) {
                    try {
                        if (outputBitmap[columnIndex + 1, rowIndex] != Color.TRANSPARENT) {
                            // TODO Edge detected. Do smoothing
                            val pixels = Array(9) { 0 }.toIntArray()
                            outputBitmap.getPixels(
                                pixels,
                                0,
                                3,
                                columnIndex + 1,
                                rowIndex - 1,
                                3,
                                3
                            )
                            smoothedBitmap[columnIndex + 1, rowIndex] = getSmoothedPixelValue(pixels)
//                            Log.i(TAG, Color.red(pixels[0]).toString())
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


    fun initializeImageSegmentationHelper(context: Context) {
        imageSegmentationHelper.setupImageSegmenter(context, onError)
    }


    fun rotateImage() {
        val imageProcessor =
            ImageProcessor.Builder()
//                .add(Rot90Op(1))
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))

                .build()

        imageBitmap =
            imageProcessor.process(TensorImage.fromBitmap(imageBitmap!!.asAndroidBitmap())).bitmap.asImageBitmap()
    }

    companion object {
        private const val TAG = "ImageSegmenterViewModel"
    }
}