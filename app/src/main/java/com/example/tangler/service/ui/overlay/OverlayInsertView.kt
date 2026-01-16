package com.example.tangler.service.ui.overlay

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
import android.widget.ImageView
import com.example.tangler.R

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
    private var expandedWidth = 0
    private var expandedHeight = 0

    private val resizeThreshold = 100f
    private val minWidth = 200f
    private val minHeight = 80f
    private val maxWidth = resources.displayMetrics.widthPixels.toFloat()
    private val maxHeight = 1000f

    private val handleSize = 45f
    private val hideHandleSize = 100f

    private var overlayButton:Button
    private val contentLayer = FrameLayout(context)
    private var isCollapsed = false

    private var iconDownX = 0f
    private var iconDownY = 0f
    private var iconWasDragged = false
    private val iconTouchSlop = 20f




    private val iconToggleView = ImageView(context).apply {
        setImageResource(R.drawable.layout_icon)
        layoutParams = LayoutParams(
            dpToPx(40),
            dpToPx(40)
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        alpha = 0.9f
    }


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

        contentLayer.addView(overlayButton)
        addView(iconToggleView)
        addView(contentLayer)
    }
    private fun collapse() {
        isCollapsed = true
        isDragging = false
        isResizing = false
        lastX = 0f
        lastY = 0f

        contentLayer.visibility = GONE

        val params = layoutParams as WindowManager.LayoutParams
        expandedWidth = params.width
        expandedHeight = params.height
        updateLayoutKeepingIconPosition(dpToPx(40), dpToPx(40))
    }


    private fun expand() {
        isCollapsed = false

        contentLayer.visibility = VISIBLE

        val targetWidth = if (expandedWidth > 0) expandedWidth else 1000
        val targetHeight = if (expandedHeight > 0) expandedHeight else 400
        updateLayoutKeepingIconPosition(targetWidth, targetHeight)
    }



    fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()


    fun setRunnable(handler: Handler, runnable: Runnable) {
        this.captureHandler = handler
        this.captureRunnable=runnable
    }

    override fun onDraw(canvas: Canvas) {
        if (isCollapsed) return

        super.onDraw(canvas)

        // 배경 사각형
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 리사이즈 핸들 (오른쪽 아래)
        canvas.drawRect(width - handleSize, height - handleSize, width.toFloat(), height.toFloat(), paint)

        //canvas.drawRect(0f, 0f, hideHandleSize, hideHandleSize/2, paint)
    }

    private fun handleMove(event: MotionEvent): Boolean {
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
        return true
    }

    private fun isInIconArea(ev: MotionEvent): Boolean {
        val iconLocation = IntArray(2)
        iconToggleView.getLocationOnScreen(iconLocation)

        val x = ev.rawX
        val y = ev.rawY

        return x >= iconLocation[0] &&
                x <= iconLocation[0] + iconToggleView.width &&
                y >= iconLocation[1] &&
                y <= iconLocation[1] + iconToggleView.height
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // ⭐ 아이콘 영역 터치는 부모가 가로챈다
        if (isInIconArea(ev)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun handleDragOnly(event: MotionEvent): Boolean {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = layoutParams as WindowManager.LayoutParams

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                // 기준점 설정만 한다 (이전 상태 완전히 무시)
                lastX = event.rawX
                lastY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()

                // ⭐ 이동만 허용 (리사이즈/상태 변화 없음)
                params.x += dx
                params.y += dy
                wm.updateViewLayout(this, params)

                // 기준점 갱신
                lastX = event.rawX
                lastY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // ⭐ 반드시 상태 정리
                isDragging = false
                isResizing = false
                lastX = 0f
                lastY = 0f
                return true
            }
        }
        return true
    }




    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 아이콘 토글 처리 (드래그 시 토글 방지)
        if (isInIconArea(event)) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    iconDownX = event.rawX
                    iconDownY = event.rawY
                    iconWasDragged = false
                    if (isCollapsed) {
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = kotlin.math.abs(event.rawX - iconDownX)
                    val dy = kotlin.math.abs(event.rawY - iconDownY)
                    if (dx > iconTouchSlop || dy > iconTouchSlop) {
                        iconWasDragged = true
                    }
                    if (isCollapsed && iconWasDragged) {
                        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        val params = layoutParams as WindowManager.LayoutParams
                        val moveDx = (event.rawX - lastX).toInt()
                        val moveDy = (event.rawY - lastY).toInt()
                        params.x += moveDx
                        params.y += moveDy
                        wm.updateViewLayout(this, params)
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (!iconWasDragged) {
                        if (isCollapsed) expand() else collapse()
                    }
                    isDragging = false
                    isResizing = false
                    lastX = 0f
                    lastY = 0f
                    return true
                }
            }
        }

        // 접힌 상태: 드래그만 허용
        if (isCollapsed) {
            return handleDragOnly(event)
        }

        // 펼쳐진 상태: 기존 드래그/리사이즈
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInResizeArea(event)) {
                    isResizing = true
                    lastX = event.rawX
                    lastY = event.rawY
                    originalWidth = width
                    originalHeight = height
                } else {
                    isDragging = true
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                return handleMove(event)
            }

            MotionEvent.ACTION_UP -> {
                isDragging = false
                isResizing = false
                return true
            }
        }
        return false
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

    private fun updateLayoutKeepingIconPosition(targetWidth: Int, targetHeight: Int) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = layoutParams as WindowManager.LayoutParams

        val viewLocation = IntArray(2)
        val iconLocation = IntArray(2)
        getLocationOnScreen(viewLocation)
        iconToggleView.getLocationOnScreen(iconLocation)

        val iconOffsetX = iconLocation[0] - viewLocation[0]
        val iconOffsetY = iconLocation[1] - viewLocation[1]

        params.width = targetWidth
        params.height = targetHeight
        params.x = iconLocation[0] - iconOffsetX
        params.y = iconLocation[1] - iconOffsetY

        wm.updateViewLayout(this, params)
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
