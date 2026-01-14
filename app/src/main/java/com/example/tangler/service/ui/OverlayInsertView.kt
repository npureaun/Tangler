package com.example.tangler.service.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout

class OverlayInsertView(context: Context,) : FrameLayout(context) {

    //오버레이뷰의 오른쪽 위에 위치하는 자식 버튼 생성
    //

    private val paint = Paint().apply {
        color = Color.argb(100, 0, 150, 250)
        style = Paint.Style.FILL
    }

    private var captureHandler: Handler? = null
    private var captureRunnable:Runnable?=null



    private var isDragging = false
    private var isResizing = false
    private var isHiding=false
    private var lastX = 0f
    private var lastY = 0f

    private var originalWidth = 0
    private var originalHeight = 0

    private val resizeThreshold = 100f
    private val minWidth = 200f
    private val minHeight = 80f
    private val maxWidth = resources.displayMetrics.widthPixels.toFloat()
    private val maxHeight = 1000f

    private val handleSize = 45f
    private val hideHandleSize = 100f

    private var overlayButton:Button

    init {
        // FrameLayout은 기본적으로 onDraw를 호출하지 않으므로 강제로 허용
        setWillNotDraw(false)
        // 레이아웃 파라미터 세팅
        overlayButton = Button(context).apply {
            alpha=0.5f
            setOnClickListener {
                captureHandler?.postDelayed(captureRunnable!!,1)
            }

            // 기본 스타일 및 크기
            layoutParams = LayoutParams(
                dpToPx(32),  // 너비
                dpToPx(36)   // 높이
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                marginEnd = dpToPx(8)
                topMargin = dpToPx(8)
            }
        }

        addView(overlayButton)
    }
    fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()


    fun setRunnable(handler: Handler, runnable: Runnable) {
        this.captureHandler = handler
        this.captureRunnable=runnable
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 배경 사각형
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 리사이즈 핸들 (오른쪽 아래)
        canvas.drawRect(width - handleSize, height - handleSize, width.toFloat(), height.toFloat(), paint)

        canvas.drawRect(0f, 0f, hideHandleSize, hideHandleSize/2, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if(isHiding) {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val params = layoutParams as WindowManager.LayoutParams

                    params.width = 1000
                    params.height = 400
                    wm.updateViewLayout(this, params)
                    isHiding=false

                    overlayButton.visibility= VISIBLE
                    return true
                }

                if (isInResizeArea(event)) {
                    isResizing = true
                    lastX = event.rawX
                    lastY = event.rawY
                    originalWidth = width
                    originalHeight = height
                }else if(isInHideArea(event)){
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val params = layoutParams as WindowManager.LayoutParams
                    isHiding=true
                    overlayButton.visibility= INVISIBLE

                    params.width = hideHandleSize.toInt()
                    params.height = (hideHandleSize/2).toInt()
                    wm.updateViewLayout(this, params)
                }
                else {
                    isDragging = true
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val params = layoutParams as WindowManager.LayoutParams

                if (isResizing) {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY

                    val newWidth = (originalWidth + dx).coerceIn(minWidth, maxWidth)
                    val newHeight = (originalHeight + dy).coerceIn(minHeight, maxHeight)

                    params.width = newWidth.toInt()
                    params.height = newHeight.toInt()
                    wm.updateViewLayout(this, params)

                } else if (isDragging) {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()

                    params.x += dx
                    params.y += dy
                    wm.updateViewLayout(this, params)

                    lastX = event.rawX
                    lastY = event.rawY
                }
                //캡쳐 러너블 정지
               // captureHandler?.removeCallbacks(captureRunnable!!)
                return true
            }

            MotionEvent.ACTION_UP -> {
                //캡쳐 러너블 재시작
                isDragging = false
                isResizing = false
                //captureHandler?.postDelayed(captureRunnable!!,3000)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isInHideArea(event: MotionEvent):Boolean{
        val location = IntArray(2)
        getLocationOnScreen(location)
        val viewLeft = location[0]
        val viewTop = location[1]

        val localX = event.rawX - viewLeft
        val localY = event.rawY - viewTop

        return localX <= hideHandleSize && localY <= 45
    }

    private fun isInResizeArea(event: MotionEvent): Boolean {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val viewLeft = location[0]
        val viewTop = location[1]

        val localX = event.rawX - viewLeft
        val localY = event.rawY - viewTop

        return localX >= (width - handleSize) && localY >= (height - handleSize)
    }

    fun getStatusBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) /2 else 0
    }

    fun getOverlayPositionWithOffset(): Rect {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val yOffset = getStatusBarHeight()
        return Rect(location[0], location[1] - (yOffset), location[0] + width, location[1] + height - (yOffset))
    }
}