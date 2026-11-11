package com.pedro.widgets.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoStorage(private val context: Context) {

    private fun getWidgetPhotoDir(widgetId: Int): File {
        val dir = File(context.filesDir, "widget_photos/$widgetId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun savePhotosForWidget(widgetId: Int, uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val targetDir = getWidgetPhotoDir(widgetId)
        // Limpar fotos antigas deste widget
        targetDir.listFiles()?.forEach { it.delete() }

        val savedPaths = mutableListOf<String>()

        uris.forEachIndexed { index, uri ->
            try {
                val bitmap = decodeSampledBitmapFromUri(uri, 1080, 1080)
                if (bitmap != null) {
                    val file = File(targetDir, "photo_${System.currentTimeMillis()}_$index.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    savedPaths.add(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        savedPaths
    }

    suspend fun loadBitmap(path: String, targetWidth: Int = 800, targetHeight: Int = 800): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            options.inJustDecodeBounds = false

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val inputBounds = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputBounds, null, options)
        inputBounds.close()

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        val inputData = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputData, null, options)
        inputData.close()

        return bitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }
}
