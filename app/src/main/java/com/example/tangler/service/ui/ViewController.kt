package com.example.tangler.service.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.example.tangler.service.ui.overlay.OverlayInsertView
import com.example.tangler.service.ui.overlay.OverlayOutputView

class ViewController(
    private val windowManager: WindowManager
) {
    private lateinit var overlayInsertView: OverlayInsertView
    private lateinit var overlayOutputView: OverlayOutputView
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private lateinit var mediaProjection: MediaProjection

    fun showTouchableResizableBox(context: Context) {
        overlayInsertView = OverlayInsertView(context)
        overlayOutputView=OverlayOutputView(context)

        // ResizableOverlayView를 위한 WindowManager.LayoutParams 설정
        val params = WindowManager.LayoutParams(
            1000,  // width
            400,   // height
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 200
            alpha = 0.9f
        }

        // 오버레이 뷰 추가
        windowManager.addView(overlayInsertView, params)

        // TextView를 위한 별도 WindowManager.LayoutParams 설정
        val textParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // 화면 너비 전체
            WindowManager.LayoutParams.WRAP_CONTENT, // 텍스트에 따라 높이 자동
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x=0
            y=0
        }
        windowManager.addView(overlayOutputView, textParams)
    }

    fun setupVirtualDisplay(captureHandler: Handler, captureRunnable: Runnable) {
        val density = Resources.getSystem().displayMetrics.densityDpi
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        overlayInsertView.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                overlayInsertView.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

                virtualDisplay= mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth, screenHeight, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, null
                )!!
                //captureHandler.postDelayed(captureRunnable,3000)
                overlayInsertView.setRunnable(captureHandler,captureRunnable)
                //Handler(Looper.getMainLooper()).postDelayed(captureRunnable, 3000)
            }
        })
    }

    fun setupMediaProjection(intent: Intent?, mediaProjectionManager: MediaProjectionManager) {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return
        val data = intent.getParcelableExtra("data", Intent::class.java) ?: return
        mediaProjectionManager.getMediaProjection(resultCode, data)?.let {
            mediaProjection=it
        }

        // 콜백 등록
        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("MediaProjection", "MediaProjection stopped.")
                virtualDisplay.release()
                imageReader.close()
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun getImageLatestImage(): Image? {
        return imageReader.acquireLatestImage()
    }

    fun getOverlayPositionWithOffset(): Rect {
        return overlayInsertView.getOverlayPositionWithOffset()
    }

    fun updateText(text: String) {
        overlayOutputView.updateText(text)
    }

    fun removeViews() {
        overlayInsertView.let {
            if (it.windowToken != null) {
                windowManager.removeView(it)
            }
        }
        overlayOutputView.let {
            if (it.windowToken != null) {
                windowManager.removeView(it)
            }
        }
    }
}