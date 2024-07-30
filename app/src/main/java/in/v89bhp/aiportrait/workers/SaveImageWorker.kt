package `in`.v89bhp.aiportrait.workers

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.work.Worker
import androidx.work.WorkerParameters
import `in`.v89bhp.aiportrait.Constants
import java.io.FileOutputStream

class SaveImageWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    override fun doWork(): Result { // TODO
//        val imageArray = inputData.getByteArray(Constants.KEY_IMAGE_BYTE_ARRAY)

        Log.i(TAG, "Saving image ${Thread.currentThread().name}")
        return Result.success()
    }

    fun saveImage() { // TODO


    }


    companion object {
        const val TAG = "SaveImageWorker"
    }
}