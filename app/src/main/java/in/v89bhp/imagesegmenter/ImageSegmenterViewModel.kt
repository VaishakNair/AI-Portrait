package `in`.v89bhp.imagesegmenter

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
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

    private var imageBitmap: ImageBitmap? by mutableStateOf(null)

    var isProcessing by mutableStateOf(false)

    var imageLoaded by mutableStateOf(false)

    var backgroundRemoved by mutableStateOf(false)

    var outputImageBitmap: ImageBitmap? by mutableStateOf(null)

    var imageConfiguration = ""

    var imageSize = ""

    var colorSpace = ""


    fun getImageBitmap(context: Context): ImageBitmap {
        if (imageBitmap == null) {
            val assetManager: AssetManager = context.assets

            imageBitmap = assetManager.open("sample_images/pp.jpg").use {
                BitmapFactory.decodeStream(it).asImageBitmap()
            }.also {
                imageConfiguration = it.config.toString()
                imageSize = "${it.width} x ${it.height}"
                colorSpace = it.colorSpace.toString()

            }
        }
        return imageBitmap!!
    }

    private val onError: (errorMessage: String) -> Unit = { errorMessage ->
        TODO("Not yet implemented")
    }


    fun loadImage(context: Context, uri: Uri) {

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

                val categoryMaskTensor = segmentation.masks[0] // A single category mask with each pixel value corresponding
                // to the category to which the pixel belongs
                Log.i(TAG, "Category mask tensor width x height: ${categoryMaskTensor.width} x ${categoryMaskTensor.height}")
                val categoryMaskArray = categoryMaskTensor.buffer.array()
                Log.i(TAG,"Category mask array size: ${categoryMaskArray.size}")

                val pixels = IntArray(categoryMaskArray.size)

                for (i in categoryMaskArray.indices) {
                    pixels[i] = if (categoryMaskArray[i].toInt() != 15) Color.TRANSPARENT else Color.RED
                }

                val imageMask = Bitmap.createBitmap(
                    pixels,
                    categoryMaskTensor.width,
                    categoryMaskTensor.height,
                    Bitmap.Config.ARGB_8888
                )


                // PreviewView is in FILL_START mode. So we need to scale up the bounding
                // box to match with the size that the captured images will be displayed.
//                val scaleFactor = max(width * 1f / segmentationResult.imageWidth, height * 1f / segmentationResult.imageHeight)
                val scaleWidth = (segmentationResult.imageWidth * 1f).toInt()
                val scaleHeight = (segmentationResult.imageHeight * 1f).toInt()

                val scaledImageMask = Bitmap.createScaledBitmap(imageMask, scaleWidth, scaleHeight, true)

                outputImageBitmap = applyMask(scaledImageMask)

                isProcessing = false


            }
        }

    }

    suspend fun applyMask(scaledImageMask: Bitmap) = withContext(Dispatchers.IO) {
            val outputBitmap = imageBitmap!!.asAndroidBitmap().let {// Make a mutable copy of the input bitmap.
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

            outputBitmap.asImageBitmap()
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