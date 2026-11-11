package com.example.service

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

class TransparentRecordActivity : Activity() {

    companion object {
        private const val REQ_CODE_RECORD = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AudioRecordService.isRecording.value) {
            // Already recording -> stop it
            AudioRecordService.stop(this)
            Toast.makeText(this, "Recording stopped & saved", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasAudio && hasNotif) {
            AudioRecordService.start(this)
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            // Forward to MainActivity to grant permissions nicely
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("REQUEST_PERMISSIONS_FOR_RECORD", true)
            }
            startActivity(intent)
            finish()
        }
    }
}
