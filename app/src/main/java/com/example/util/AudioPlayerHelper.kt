package com.example.util

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var currentFilePath: String? = null

    fun load(filePath: String, autoPlay: Boolean = false) {
        if (currentFilePath == filePath && mediaPlayer != null) {
            if (autoPlay && !_isPlaying.value) play()
            return
        }

        stop()
        currentFilePath = filePath

        val file = File(filePath)
        if (!file.exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                _durationMs.value = duration
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopProgressTicker()
                }
            }
            setSpeed(_playbackSpeed.value)
            if (autoPlay) {
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            player.start()
            _isPlaying.value = true
            startProgressTicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            player.pause()
            _isPlaying.value = false
            stopProgressTicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val player = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = PlaybackParams().apply { this.speed = speed }
                player.playbackParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cycleSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        setSpeed(nextSpeed)
    }

    fun stop() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        currentFilePath = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
