/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package `in`.v89bhp.imagesegmenter.helpers

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation

/**
 * Class responsible to run the Image Segmentation model. more information about the DeepLab model
 * being used can be found here:
 * https://ai.googleblog.com/2018/03/semantic-image-segmentation-with.html
 * https://github.com/tensorflow/models/tree/master/research/deeplab
 *
 * Label names: 'background', 'aeroplane', 'bicycle', 'bird', 'boat', 'bottle', 'bus', 'car', 'cat',
 * 'chair', 'cow', 'diningtable', 'dog', 'horse', 'motorbike', 'person', 'pottedplant', 'sheep',
 * 'sofa', 'train', 'tv'
 */
class ImageSegmentationHelper(
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    val coroutineDispatcher: CoroutineDispatcher
) {

    private lateinit var context: Context
    private lateinit var onError: (errorMessage: String) -> Unit
    private var imageSegmenter: ImageSegmenter? = null


    fun clearImageSegmenter() {
        imageSegmenter = null
    }

    fun setupImageSegmenter(context: Context, onError: (String) -> Unit) {
        // Create the base options for the segment
        val optionsBuilder =
            ImageSegmenter.ImageSegmenterOptions.builder()

        // Set general segmentation options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // TODO Tweak logic to always use GPU or NNAPI delegate if they're available (instead of
        //  letting user decide):
        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }

            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    onError("GPU is not supported on this device")
                }
            }

            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        /*
        CATEGORY_MASK is being specifically used to predict the available objects
        based on individual pixels in this sample. The other option available for
        OutputType, CONFIDENCE_MAP, provides a gray scale mapping of the image
        where each pixel has a confidence score applied to it from 0.0f to 1.0f
         */
        optionsBuilder.setOutputType(OutputType.CONFIDENCE_MASK)
        try {
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    context,
                    MODEL_U2NET, // TODO Specify model here
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
            onError(
                "Image segmentation failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }


    suspend fun segment(image: Bitmap, imageRotation: Int): SegmentationResult =
        withContext(coroutineDispatcher) {
            if (imageSegmenter == null) {
                setupImageSegmenter(context, onError)
            }

            // Inference time is the difference between the system time at the start and finish of the
            // process
            var inferenceTime = SystemClock.uptimeMillis()

            // Create preprocessor for the image.
            // See https://www.tensorflow.org/lite/inference_with_metadata/
            //            lite_support#imageprocessor_architecture
            val imageProcessor =
                ImageProcessor.Builder()
                    .add(Rot90Op(-imageRotation / 90))

                    .build()

            // Preprocess the image and convert it into a TensorImage for segmentation.
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

            val segmentResult = imageSegmenter?.segment(tensorImage)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            SegmentationResult(
                segmentResult,
                inferenceTime,
                tensorImage.height,
                tensorImage.width
            )
        }


    data class SegmentationResult(
        val results: List<Segmentation>?,
        val inferenceTime: Long,
        val imageHeight: Int,
        val imageWidth: Int
    )

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILE_NET_DM05 = "lite-model_deeplabv3-mobilenetv2_dm05_1_default_2.tflite"
        const val MODEL_DEEPLAB_XCEPTION65 = "lite-model_deeplabv3-xception65_1_default_2.tflite"
        const val MODEL_U2NET = "40_with_metadata.tflite"

        private const val TAG = "Image Segmentation Helper"
    }
}
