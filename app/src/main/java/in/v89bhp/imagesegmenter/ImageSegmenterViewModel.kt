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
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper
import org.tensorflow.lite.support.common.ops.DequantizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.vision.segmenter.Segmentation


class ImageSegmenterViewModel : ViewModel() {

    private var imageBitmap: ImageBitmap? by mutableStateOf(null)

    var imageConfiguration  = ""

    var imageSize = ""

    var colorSpace = ""

    private lateinit var imageSegmentationHelper: ImageSegmentationHelper

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

    private val imageSegmentationListener = object : ImageSegmentationHelper.SegmentationListener {
        override fun onError(error: String) {
            TODO("Not yet implemented")
        }

        override fun onResults(
            results: List<Segmentation>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        ) {
           // TODO
            Log.i(TAG, "Segmented region count: ${results?.size ?: 0}")
        }

    }

    fun removeBackground() {
        // TODO Run in a IO dispatcher as a coroutine:
        imageSegmentationHelper.segment(imageBitmap!!.asAndroidBitmap(), 0)
    }

    fun initializeImageSegmentationHelper(context: Context) {
        imageSegmentationHelper = ImageSegmentationHelper(
            context = context,
            imageSegmentationListener = imageSegmentationListener
        )
    }



    fun rotateImage() {
        val imageProcessor =
            ImageProcessor.Builder()
//                .add(Rot90Op(1))
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))

                .build()

        imageBitmap = imageProcessor.process(TensorImage.fromBitmap(imageBitmap!!.asAndroidBitmap())).bitmap.asImageBitmap()
    }

    companion object {
        private const val TAG = "ImageSegmenterViewModel"
    }
}