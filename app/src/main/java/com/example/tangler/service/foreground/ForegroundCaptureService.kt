package com.example.tangler.service.foreground

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.tangler.R
import com.example.tangler.service.bitmap.BitmapComponent
import com.example.tangler.service.gptapi.GptManager
import com.example.tangler.service.ocr.OCRManager
import com.example.tangler.service.ui.OverlayInsertView
import com.example.tangler.service.ui.OverlayOutputView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.tangler.service.bitmap.BitmapComponentImpl

class ForegroundCaptureService : Service() {
    companion object {
        const val CHANNEL_ID = "capture_channel"
        const val NOTIF_ID = 1
        const val ACTION_SCREENSHOT = "com.example.ACTION_SCREENSHOT"
    }

    private val ocrManager=OCRManager()
    private var mediaProjection: MediaProjection? = null
    private lateinit var imageReader: ImageReader
    private var virtualDisplay: VirtualDisplay? = null
    private var windowManager: WindowManager? = null
    private var overlayInsertView: OverlayInsertView? = null
    private var overlayOutputView: OverlayOutputView?=null

    private val captureHandler = Handler(Looper.getMainLooper())

    private lateinit var gptManager:GptManager
    private lateinit var bitmapComponent: BitmapComponent

    private val captureRunnable = Runnable {
        val image = imageReader.acquireLatestImage()
        image?.let {
            val fullBitmap = bitmapComponent.imageToBitmap(it)

            val updatedRegion = overlayInsertView?.getOverlayPositionWithOffset()
            if (updatedRegion != null) {
                val croppedBitmap = bitmapComponent.cropBitmap(fullBitmap, updatedRegion, true)
                var isGptRunning = true
                ocrManager.recognizeTextFromImage(croppedBitmap, { recognizedText ->
                    //코루틴으로 . -> .. -> ... 으로 ui업데이트 되도록
                    val loadingJob = CoroutineScope(Dispatchers.Main).launch {
                        val states = listOf(".", "..", "...")
                        var i = 0
                        while (isGptRunning) {
                            if (i % 50 == 0) {
                                overlayOutputView?.updateText(states[(i / 50) % states.size])
                            }
                            delay(10)
                            i++
                        }
                    }
                    gptManager.requestGptResponse(recognizedText){resultText->
                        Thread.sleep(10)
                        isGptRunning=false
                        if(resultText==null) overlayOutputView?.updateText("ERROR")
                        else overlayOutputView?.updateText(resultText)
                    }
                    Log.d("OCR", "인식된 텍스트: $recognizedText")
                }, { error ->
                    Log.e("OCR", "OCR 처리 실패: ${error.message}")
                })

                Log.d("Capture", "Screen captured and cropped.")
            }

            it.close()
        }

        // 🟡 다음 실행 예약
        //captureHandler.postDelayed(this, 3000)
    }

    override fun onCreate() {
        super.onCreate()
        gptManager=GptManager()
        bitmapComponent= BitmapComponentImpl(this.contentResolver)
        showTouchableResizableBox()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()

        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        // 콜백 등록
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("MediaProjection", "MediaProjection stopped.")
                virtualDisplay?.release()
                imageReader.close()
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))

        setupVirtualDisplay()

        return START_NOT_STICKY
    }

    private fun createNotification() {
        val channel = NotificationChannel(CHANNEL_ID, "Screen Capture", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("화면 캡처 중")
            .setContentText("지정된 영역을 분석 중입니다.")
            .setSmallIcon(R.drawable.icon)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun setupVirtualDisplay() {
        val density = Resources.getSystem().displayMetrics.densityDpi
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        overlayInsertView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                overlayInsertView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth, screenHeight, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, null
                )
                //captureHandler.postDelayed(captureRunnable,3000)
                overlayInsertView?.setRunnable(captureHandler,captureRunnable)
                //Handler(Looper.getMainLooper()).postDelayed(captureRunnable, 3000)
            }
        })
    }

    private fun showTouchableResizableBox() {
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }

        overlayInsertView = OverlayInsertView(this)
        overlayOutputView=OverlayOutputView(this)

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
        windowManager?.addView(overlayInsertView, params)

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
        windowManager?.addView(overlayOutputView, textParams)
    }

    private fun stopService(){
        removeViews()
        stopSelf()
    }

    private fun removeViews() {
        overlayInsertView?.let {
            windowManager?.removeView(it)
        }
        overlayOutputView?.let {
            windowManager?.removeView(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopService()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
