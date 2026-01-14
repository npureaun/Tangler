package com.example.tangler.service.bitmap

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.IOException

class BitmapComponentImpl(
    private val resolver: ContentResolver,
): BitmapComponent {
    private fun saveBitmapToFile(bitmap: Bitmap, fileName: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Screenshots")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create new MediaStore record.")

            resolver.openOutputStream(uri).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out!!)) {
                    throw IOException("Failed to save bitmap.")
                }
            }

            // 파일 쓰기 완료 표시
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            Log.d("SaveImage", "Saved to MediaStore: $uri")

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("SaveImage", "Error saving image: ${e.message}")
        }
    }

    override fun cropBitmap(bitmap: Bitmap, captureRegion: Rect, saveOption: Boolean): Bitmap {
        val cropLeft = captureRegion.left.coerceAtLeast(0)
        val cropTop = captureRegion.top.coerceAtLeast(0)
        val cropWidth = captureRegion.width().coerceAtMost(bitmap.width - cropLeft)
        val cropHeight = captureRegion.height().coerceAtMost(bitmap.height - cropTop)

        val result= Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
        if(saveOption) saveBitmapToFile(result, "1")
        return result
    }

     override fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }
}