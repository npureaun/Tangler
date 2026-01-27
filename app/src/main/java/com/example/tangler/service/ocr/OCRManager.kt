package com.example.tangler.service.ocr

import android.graphics.Bitmap

interface OCRManager {
    fun ocrProcess(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    )
}