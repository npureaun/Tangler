package com.example.tangler.service.ocr

object UiStateProvider {
    private var currentOCRType: OCRType = OCRType.ENG

    fun getCurrentOCRType(): OCRType = currentOCRType

    fun setCurrentOCRType(type: OCRType) {
        currentOCRType = type
    }
}
