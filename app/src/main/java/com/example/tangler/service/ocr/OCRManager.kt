package com.example.tangler.service.ocr

import android.graphics.Bitmap

class OCRManager {


    private val ocrMap: Map<OCRType, OCRComponent> = mapOf(
        OCRType.ENG to OCRENGComponentImpl(),
        OCRType.JPN to OCRJPNComponentImpl()
    )

    fun ocrProcess(bitmap: Bitmap): String {
        val key = uiStateProvider.getCurrentOCRType()
        val component = ocrMap[key]
            ?: error("Unsupported OCR type: $key")

        return component.process(bitmap)
    }
}