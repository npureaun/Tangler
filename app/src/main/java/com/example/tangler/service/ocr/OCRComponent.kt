package com.example.tangler.service.ocr

import android.graphics.Bitmap

interface OCRComponent {
    fun recognizeTextFromImage(bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit)
}