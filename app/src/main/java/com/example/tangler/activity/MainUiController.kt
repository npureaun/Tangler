package com.example.tangler.activity

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.example.tangler.R
import com.example.tangler.service.ocr.OCRType
import com.example.tangler.service.ocr.UiStateProvider

class MainUiController(
    private val activity: Activity,
    private val onRestartClick: () -> Unit,
    private val onExitClick: () -> Unit
) {

    fun bind() {
        val ocrTypeSpinner =
            activity.findViewById<Spinner>(R.id.spinnerOcrType)
        val restartButton =
            activity.findViewById<Button>(R.id.btnRestart)
        val exitButton =
            activity.findViewById<Button>(R.id.btnExit)

        val ocrTypes = OCRType.entries.toTypedArray()
        val adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            ocrTypes.map { it.name }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        ocrTypeSpinner.adapter = adapter
        ocrTypeSpinner.setSelection(ocrTypes.indexOf(UiStateProvider.getCurrentOCRType()))
        ocrTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                UiStateProvider.setCurrentOCRType(ocrTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        restartButton.setOnClickListener {
            onRestartClick()
        }

        exitButton.setOnClickListener {
            onExitClick()
        }
    }
}
