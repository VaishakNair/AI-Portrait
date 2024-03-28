package `in`.v89bhp.imagesegmenter.ui.upscaler

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.v89bhp.imagesegmenter.helpers.SisrHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream


class SisrViewModel(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val start: CoroutineStart = CoroutineStart.DEFAULT,
    private val sisrHelper: SisrHelper = SisrHelper(coroutineDispatcher = Dispatchers.IO)
) : ViewModel() {

    var imageBitmap: ImageBitmap? by mutableStateOf(null)

    var loadingImage by mutableStateOf(false)

    var isProcessing by mutableStateOf(false)

    var imageLoaded by mutableStateOf(false)

    var imageUpscaled by mutableStateOf(false)

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
        imageUpscaled = false
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

        if (imageBitmap!!.width < SisrHelper.IMAGE_WIDTH || imageBitmap!!.height < SisrHelper.IMAGE_HEIGHT) {
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

    fun enhanceResolution() {
        viewModelScope.launch(
            context = coroutineDispatcher, start = start
        ) {
            isProcessing = true

            val enhancedImageBitmap = sisrHelper.enhanceResolution(imageBitmap!!.asAndroidBitmap())
            imageBitmap = enhancedImageBitmap
            imageSize = "${imageBitmap!!.width} x ${imageBitmap!!.height}"
            isProcessing = false
            imageUpscaled = true
        }
    }


    fun initializeSisrHelper(context: Context) {
        sisrHelper.setup(context)
    }

    companion object {
        private const val TAG = "SisrViewModel"
    }
}