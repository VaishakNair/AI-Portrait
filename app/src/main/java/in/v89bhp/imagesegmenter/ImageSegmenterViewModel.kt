package `in`.v89bhp.imagesegmenter

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel


class ImageSegmenterViewModel : ViewModel() {


    fun getImageBitmap(context: Context): ImageBitmap {
        val assetManager: AssetManager = context.assets

        val imageBitmap = assetManager.open("sample_images/pp.jpg").use {
            BitmapFactory.decodeStream(it).asImageBitmap()
        }
        return imageBitmap
    }

    fun removeBackground() {

    }
}