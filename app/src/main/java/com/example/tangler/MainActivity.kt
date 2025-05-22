package com.example.tangler

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.example.tangler.service.ForegroundCaptureService

class MainActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var foregroundCaptureServiceIntent:Intent
    private val REQUEST_OVERLAY_PERMISSION = 1000
    private val REQUEST_MEDIA_PROJECTION = 1001

    override fun onDestroy() {
        super.onDestroy()
        stopService(foregroundCaptureServiceIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            return
        }

        // STEP 2: MediaProjection 권한 요청
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
    }

    // STEP 3: 권한 요청 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) {
                    mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
                } else {
                    Toast.makeText(this, "오버레이 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK && data != null) {
                    foregroundCaptureServiceIntent = Intent(this, ForegroundCaptureService::class.java).apply {
                        putExtra("resultCode", resultCode)
                        putExtra("data", data)
                    }

                    ContextCompat.startForegroundService(this, foregroundCaptureServiceIntent)
                    moveTaskToBack(true)
                } else {
                    Toast.makeText(this, "화면 캡처 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
