package com.example.tangler.activity

import android.app.Activity
import android.widget.Button
import com.example.tangler.R

class MainUiController(
    private val activity: Activity,
    private val onRestartClick: () -> Unit,
    private val onExitClick: () -> Unit
) {

    fun bind() {
        val restartButton =
            activity.findViewById<Button>(R.id.btnRestart)
        val exitButton =
            activity.findViewById<Button>(R.id.btnExit)

        restartButton.setOnClickListener {
            onRestartClick()
        }

        exitButton.setOnClickListener {
            onExitClick()
        }
    }
}
