package `in`.v89bhp.imagesegmenter

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel


class ImageSegmenterViewModel : ViewModel() {

    private var imageBitmap: ImageBitmap? by mutableStateOf(null)

    var imageConfiguration by mutableStateOf("")

    var imageSize by mutableStateOf("")

    var colorSpace by mutableStateOf("")

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

    fun removeBackground() {

    }

    companion object {
        private const val TAG = "ImageSegmenterViewModel"
    }
}