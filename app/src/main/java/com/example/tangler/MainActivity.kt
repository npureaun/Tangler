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
    private lateinit var serviceIntent: Intent

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

        uiController = MainUiController(
            activity = this,
            onRestartClick = {
                restartCaptureService()
            },
            onExitClick = {
                shutdownApp()
            }
        )
        uiController.bind()
    }

    fun restartCaptureService(){
        stopService(serviceIntent)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)

        finish()
    }

    fun shutdownApp(){
        stopService(serviceIntent)
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}