package com.example.tangler.service.bitmap

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image

interface BitmapComponent {
    fun cropBitmap(bitmap: Bitmap, captureRegion: Rect, saveOption: Boolean=false): Bitmap
    fun imageToBitmap(image: Image): Bitmap
}