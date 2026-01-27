package com.example.tangler.service.ocr

import android.graphics.Bitmap
import com.example.tangler.service.ocr.component.OCRComponent
import com.example.tangler.service.ocr.component.OCRENGComponentImpl
import com.example.tangler.service.ocr.component.OCRJPNComponentImpl

class OCRManagerImpl: OCRManager {

    private val ocrMap: Map<OCRType, OCRComponent> = mapOf(
        OCRType.ENG to OCRENGComponentImpl(),
        OCRType.JPN to OCRJPNComponentImpl()
    )

    override fun ocrProcess(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val key = UiStateProvider.getCurrentOCRType()
        val component = ocrMap[key]
            ?: error("Unsupported OCR type: $key")

        component.recognizeTextFromImage(
            bitmap = bitmap,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}
