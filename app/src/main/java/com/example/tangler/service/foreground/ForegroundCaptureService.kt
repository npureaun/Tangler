package com.example.tangler.service.foreground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.tangler.R
import com.example.tangler.service.aiapi.gpt.AIManager
import com.example.tangler.service.aiapi.gpt.GptManagerImpl
import com.example.tangler.service.bitmap.BitmapComponent
import com.example.tangler.service.bitmap.BitmapComponentImpl
import com.example.tangler.service.ocr.OCRComponent
import com.example.tangler.service.ocr.OCRComponentImpl
import com.example.tangler.service.ui.ViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForegroundCaptureService : Service() {
    companion object {
        const val CHANNEL_ID = "capture_channel"
        const val NOTIF_ID = 1
    }
    private var windowManager: WindowManager? = null

    private lateinit var ocrComponent: OCRComponent
    private lateinit var aiManager: AIManager
    private lateinit var bitmapComponent: BitmapComponent
    private lateinit var viewController: ViewController

    //mainProcess
    private val captureHandler = Handler(Looper.getMainLooper())
    private val captureRunnable = Runnable {
        val image = viewController.getImageLatestImage()
        image?.let {
            val fullBitmap = bitmapComponent.imageToBitmap(it)

            val updatedRegion = viewController.getOverlayPositionWithOffset()
            val croppedBitmap = bitmapComponent.cropBitmap(fullBitmap, updatedRegion, true)
            var isGptRunning = true
            ocrComponent.recognizeTextFromImage(croppedBitmap, { recognizedText ->
                //코루틴으로 . -> .. -> ... 으로 ui업데이트 되도록
                CoroutineScope(Dispatchers.Main).launch {
                    val states = listOf(".", "..", "...")
                    var i = 0
                    while (isGptRunning) {
                        if (i % 50 == 0) {
                            viewController.updateText(states[(i / 50) % states.size])
                        }
                        delay(10)
                        i++
                    }
                }
                aiManager.requestGptResponse(recognizedText){resultText->
                    Thread.sleep(10)
                    isGptRunning=false
                    if(resultText==null) viewController.updateText("ERROR")
                    else viewController.updateText(resultText)
                }
                Log.d("OCR", "인식된 텍스트: $recognizedText")
            }, { error ->
                Log.e("OCR", "OCR 처리 실패: ${error.message}")
            })

            Log.d("Capture", "Screen captured and cropped.")

            it.close()
        }

        // 🟡 다음 실행 예약
        //captureHandler.postDelayed(this, 3000)
    }

    override fun onCreate() {
        super.onCreate()
        ocrComponent= OCRComponentImpl()
        aiManager=GptManagerImpl()
        bitmapComponent= BitmapComponentImpl(this.contentResolver)
        setUpViewController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()

        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        intent?.let { viewController.setupMediaProjection(it, mediaProjectionManager) }
        viewController.setupVirtualDisplay(captureHandler, captureRunnable)

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

    private fun setUpViewController() {
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager?.let {
                viewController= ViewController(it)
            }
        }
        viewController.showTouchableResizableBox(this)
    }

    private fun stopService(){
        viewController.removeViews()
        stopSelf()
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
