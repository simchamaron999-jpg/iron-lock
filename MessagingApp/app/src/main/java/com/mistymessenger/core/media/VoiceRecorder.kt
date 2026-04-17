package com.mistymessenger.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class RecorderState {
    object Idle : RecorderState()
    data class Recording(val durationMs: Long, val amplitudes: List<Int>) : RecorderState()
    data class Done(val file: File, val durationMs: Long, val amplitudes: List<Int>) : RecorderState()
}

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime = 0L
    private val amplitudes = mutableListOf<Int>()

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state = _state.asStateFlow()

    fun start() {
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file
        amplitudes.clear()
        startTime = System.currentTimeMillis()

        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        _state.value = RecorderState.Recording(0, emptyList())
    }

    fun sampleAmplitude() {
        val amp = recorder?.maxAmplitude ?: return
        amplitudes.add(amp)
        val duration = System.currentTimeMillis() - startTime
        _state.value = RecorderState.Recording(duration, amplitudes.toList())
    }

    fun stop(): File? {
        val file = outputFile ?: return null
        runCatching {
            recorder?.apply { stop(); release() }
        }
        recorder = null
        val duration = System.currentTimeMillis() - startTime
        _state.value = RecorderState.Done(file, duration, amplitudes.toList())
        return file
    }

    fun cancel() {
        runCatching { recorder?.apply { stop(); release() } }
        recorder = null
        outputFile?.delete()
        outputFile = null
        _state.value = RecorderState.Idle
    }

    fun reset() { _state.value = RecorderState.Idle }
}
