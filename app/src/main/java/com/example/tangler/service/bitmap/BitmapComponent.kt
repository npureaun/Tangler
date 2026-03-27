package com.example.tangler.service.bitmap

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image

interface BitmapComponent {
    fun cropBitmap(saveOption: Boolean, bitmap: Bitmap, captureRegion: Rect): Bitmap
    fun imageToBitmap(image: Image): Bitmap
}