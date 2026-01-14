package com.example.tangler.service.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRComponentImpl: OCRComponent {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // OCR 처리 후 문자열을 반환하는 함수
    override fun recognizeTextFromImage(bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                // OCR 성공 시 추출된 텍스트를 반환
                onSuccess(visionText.text)
            }
            .addOnFailureListener { e ->
                // OCR 실패 시 에러 처리
                onFailure(e)
            }
    }
}
