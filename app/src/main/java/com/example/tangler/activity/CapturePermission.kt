package com.example.tangler.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri

class CapturePermission(
    private val activity: Activity,
    private val mediaProjectionLauncher: ActivityResultLauncher<Intent>
) {

    private val mediaProjectionManager =
        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun permissionCheck() {
        if (Settings.canDrawOverlays(activity)) {
            requestScreenCapture()
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${activity.packageName}".toUri()
        )
        activity.startActivity(intent)
        Toast.makeText(activity, "오버레이 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }
}
