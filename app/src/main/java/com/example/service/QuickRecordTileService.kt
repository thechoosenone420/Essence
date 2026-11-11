package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuickRecordTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(AudioRecordService.isRecording.value)

        scope.launch {
            AudioRecordService.isRecording.collectLatest { recording ->
                updateTileState(recording)
            }
        }
    }

    private fun updateTileState(isRecording: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRecording) "Recording..." else "Quick Record"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_mic)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, TransparentRecordActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            val pendingIntent = PendingIntent.getActivity(
                this,
                5001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
