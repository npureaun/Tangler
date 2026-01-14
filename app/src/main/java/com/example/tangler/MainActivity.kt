package com.example.tangler

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tangler.activity.CapturePermission
import com.example.tangler.activity.MainUiController
import com.example.tangler.service.foreground.ForegroundCaptureService

class MainActivity : AppCompatActivity() {

    private lateinit var uiController: MainUiController
    private lateinit var permission: CapturePermission
    private lateinit var serviceIntent : Intent

    private val mediaProjectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                serviceIntent = Intent(this, ForegroundCaptureService::class.java).apply {
                    putExtra("resultCode", result.resultCode)
                    putExtra("data", result.data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
                moveTaskToBack(true)
            } else {
                Toast.makeText(this, "화면 캡처 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permission = CapturePermission(this, mediaProjectionLauncher)
        permission.permissionCheck()

        uiController = MainUiController(this)
    }

        override fun onDestroy() {
        super.onDestroy()
    }
}



//class MainActivity : AppCompatActivity() {
//    private lateinit var mediaProjectionManager: MediaProjectionManager
//    private lateinit var foregroundCaptureServiceIntent:Intent
//
//    private val mediaProjectionLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
//
//                val serviceIntent = Intent(this, ForegroundCaptureService::class.java).apply {
//                    putExtra("resultCode", result.resultCode)
//                    putExtra("data", result.data)
//                }
//
//                ContextCompat.startForegroundService(this, serviceIntent)
//                moveTaskToBack(true)
//
//            } else {
//                Toast.makeText(this, "화면 캡처 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopService(foregroundCaptureServiceIntent)
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // STEP 1: 오버레이 권한 확인
//        mediaProjectionManager =
//            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//
//        checkOverlayPermissionAndStartCapture()
//    }
//
//    private fun checkOverlayPermissionAndStartCapture() {
//        if (Settings.canDrawOverlays(this)) {
//            requestScreenCapture()
//        } else {
//            requestOverlayPermission()
//        }
//    }
//
//    private fun requestOverlayPermission() {
//        val intent = Intent(
//            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//            "package:$packageName".toUri()
//        )
//        startActivity(intent)
//
//        Toast.makeText(this, "오버레이 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun requestScreenCapture() {
//        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
//        mediaProjectionLauncher.launch(captureIntent)
//    }
//}
