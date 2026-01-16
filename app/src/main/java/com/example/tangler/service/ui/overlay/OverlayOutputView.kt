package com.example.tangler.service.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import io.noties.markwon.Markwon

class OverlayOutputView(private val context: Context) : FrameLayout(context) {
    private var markdown: Markwon?=null

    val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        background = GradientDrawable().apply {
            setColor("#99000000".toColorInt()) // 90% 투명 셀로판 느낌
            cornerRadius = 24f
        }
        gravity = Gravity.CENTER
        setPadding(16, 8, 16, 8)
        visibility= INVISIBLE
    }


    init {
        textView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                textView.visibility = GONE
                textView.text = ""
                true
            } else {
                false
            }
        }
        addView(textView) // `TextView`는 `OverlayTextView` 내에서 관리되고, 추가됩니다.
    }

    fun updateText(text: String) {
        if (markdown == null) {
            markdown = Markwon.create(textView.context)

            textView.movementMethod = null
            textView.isClickable = false
            textView.isFocusable = false
            textView.isLongClickable = false
        }
        textView.visibility= VISIBLE
        markdown!!.setMarkdown(textView, text)
    }
}