package `in`.v89bhp.imagesegmenter.ui.backgroundremover

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
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
import `in`.v89bhp.imagesegmenter.extensions.smoothenTransparentEdges
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper.Companion.IMAGE_HEIGHT
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper.Companion.IMAGE_WIDTH
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileOutputStream


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

    var imageSaved by mutableStateOf(false)

    var imageDimensionError by mutableStateOf(false)

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
        imageSaved = false
        imageDimensionError = false
        imageLoaded = false
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

        if (imageBitmap!!.width < IMAGE_WIDTH || imageBitmap!!.height < IMAGE_HEIGHT) {
            // Show error snackbar:
            imageDimensionError = true
            return
        }

        imageLoaded = true
    }

    fun saveImage(context: Context) {
        viewModelScope.launch(
            context = coroutineDispatcher, start = start
        ) {
            val resolver = context.applicationContext.contentResolver
            val photoDetails = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.png"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val imagesCollection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val photoContentUri = resolver.insert(imagesCollection, photoDetails)

            isProcessing = true

            withContext(Dispatchers.IO) {
                // Dump the image data to the actual file returned by MediaStore:
                resolver.openFileDescriptor(photoContentUri!!, "w", null).use { fd ->
                    FileOutputStream(fd!!.fileDescriptor).use { os ->
                        imageBitmap!!.asAndroidBitmap().apply { setHasAlpha(true) }.compress(
                            Bitmap.CompressFormat.PNG, 100, os
                        )
                    }
                }

                // Photo dumped. Clear the IS_PENDING STATUS:
                photoDetails.clear()
                photoDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(photoContentUri, photoDetails, null, null)
            }
            isProcessing = false
            imageSaved = true
        }
    }

    fun removeBackground() {
        // Run in a IO dispatcher as a coroutine:
        viewModelScope.launch(
            context = coroutineDispatcher, start = start
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
        outputBitmap = outputBitmap.smoothenTransparentEdges()
        outputBitmap.asImageBitmap()
    }

    fun initializeImageSegmentationHelper(context: Context) {
        imageSegmentationHelper.setupImageSegmenter(context, onError)
    }


    fun rotateImage() {
        val imageProcessor = ImageProcessor.Builder()
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