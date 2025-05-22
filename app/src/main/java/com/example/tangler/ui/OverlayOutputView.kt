package com.example.tangler.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

class OverlayOutputView(context: Context) : FrameLayout(context) {
    val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#99000000")) // 90% 투명 셀로판 느낌
            cornerRadius = 24f
        }
        gravity = Gravity.CENTER
        setPadding(16, 8, 16, 8)
        visibility=View.INVISIBLE
    }


    init {
        addView(textView) // `TextView`는 `OverlayTextView` 내에서 관리되고, 추가됩니다.
    }

    fun updateText(text: String) {
        textView.visibility= View.VISIBLE
        textView.text = text
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                textView.visibility=View.GONE

                return true
            }
        }
        return super.onTouchEvent(event)
    }
}