package com.example.tangler.service

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.ContentValues
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.example.tangler.R
import com.example.tangler.gptapi.GptManager
import com.example.tangler.ocr.OCRManager
import com.example.tangler.ui.OverlayOutputView
import com.example.tangler.ui.OverlayInsertView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    private val captureRunnable = object : Runnable {
        override fun run() {
            val image = imageReader.acquireLatestImage()
            image?.let {
                val fullBitmap = imageToBitmap(it)

                val updatedRegion = overlayInsertView?.getOverlayPositionWithOffset()
                if (updatedRegion != null) {
                    val croppedBitmap = cropBitmap(fullBitmap, updatedRegion)
                    //saveBitmapToFile(croppedBitmap,"1")
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
    }

    override fun onCreate() {
        super.onCreate()
        gptManager=GptManager()
        showTouchableResizableBox()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()

        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Screen Capture", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("화면 캡처 중")
            .setContentText("지정된 영역을 분석 중입니다.")
            .setSmallIcon(R.drawable.tung_sahur)
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



    private fun cropBitmap(bitmap: Bitmap, captureRegion: Rect): Bitmap {
        val cropLeft = captureRegion.left.coerceAtLeast(0)
        val cropTop = captureRegion.top.coerceAtLeast(0)
        val cropWidth = captureRegion.width().coerceAtMost(bitmap.width - cropLeft)
        val cropHeight = captureRegion.height().coerceAtMost(bitmap.height - cropTop)

        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
    }


    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x=0
            y=0
        }


        // // 70% 투명한 흰색 배경과 검은색 텍스트
        // overlayTextView?.textView?.apply {
        //     setBackgroundColor(Color.TRANSPARENT)  // '#B3'은 70% 투명도, 흰색
        //     setTextColor(Color.BLACK)  // 텍스트 색상 검정
        // }

        windowManager?.addView(overlayOutputView, textParams)
    }

    fun saveBitmapToFile(bitmap: Bitmap, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 이상: MediaStore API 사용
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Screenshots")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = this.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Failed to create new MediaStore record.")

                resolver.openOutputStream(uri).use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out!!)) {
                        throw IOException("Failed to save bitmap.")
                    }
                }

                // 파일 쓰기 완료 표시
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Log.d("SaveImage", "Saved to MediaStore: $uri")

            } else {
                // Android 9 이하: 직접 외부 저장소에 파일로 저장
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val screenshotsDir = File(picturesDir, "Screenshots")
                if (!screenshotsDir.exists()) screenshotsDir.mkdirs()

                val file = File(screenshotsDir, fileName)
                FileOutputStream(file).use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw IOException("Failed to save bitmap.")
                    }
                }

                // 갤러리에 즉시 반영
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)

                Log.d("SaveImage", "Saved to file: ${file.absolutePath}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("SaveImage", "Error saving image: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(overlayInsertView)
        windowManager?.removeView(overlayOutputView)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
