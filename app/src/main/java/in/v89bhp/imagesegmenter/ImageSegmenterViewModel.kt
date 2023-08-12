package `in`.v89bhp.imagesegmenter

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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


    fun removeBackground() {
        // Run in a IO dispatcher as a coroutine:
        viewModelScope.launch(
            context = coroutineDispatcher,
            start = start
        ) {
            val segmentationResult =
                imageSegmentationHelper.segment(imageBitmap!!.asAndroidBitmap(), 0)
            Log.i(TAG, "Segmented region count: ${segmentationResult.results?.size ?: 0}")
        }

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