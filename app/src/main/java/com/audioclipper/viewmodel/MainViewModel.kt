package com.audioclipper.viewmodel

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Done(val path: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

class MainViewModel : ViewModel() {

    private val _audioUri = MutableStateFlow<Uri?>(null)
    val audioUri: StateFlow<Uri?> = _audioUri

    private val _audioFileName = MutableStateFlow("")
    val audioFileName: StateFlow<String> = _audioFileName

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _inPointMs = MutableStateFlow<Long?>(null)
    val inPointMs: StateFlow<Long?> = _inPointMs

    private val _outPointMs = MutableStateFlow<Long?>(null)
    val outPointMs: StateFlow<Long?> = _outPointMs

    private val _fadeInDuration = MutableStateFlow(1f)
    val fadeInDuration: StateFlow<Float> = _fadeInDuration

    private val _fadeOutDuration = MutableStateFlow(1f)
    val fadeOutDuration: StateFlow<Float> = _fadeOutDuration

    private val _fadeInEnabled = MutableStateFlow(false)
    val fadeInEnabled: StateFlow<Boolean> = _fadeInEnabled

    private val _fadeOutEnabled = MutableStateFlow(false)
    val fadeOutEnabled: StateFlow<Boolean> = _fadeOutEnabled

    private val _pitchSemitones = MutableStateFlow(0)
    val pitchSemitones: StateFlow<Int> = _pitchSemitones

    private val _volumePercent = MutableStateFlow(100)
    val volumePercent: StateFlow<Int> = _volumePercent

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun setAudioUri(uri: Uri, fileName: String) {
        _audioUri.value = uri
        _audioFileName.value = fileName
        _inPointMs.value = null
        _outPointMs.value = null
    }

    fun setDuration(ms: Long) {
        _durationMs.value = ms
    }

    fun clearAudio() {
        _audioUri.value = null
        _audioFileName.value = ""
        _durationMs.value = 0L
        _inPointMs.value = null
        _outPointMs.value = null
        _fadeInEnabled.value = false
        _fadeOutEnabled.value = false
        _fadeInDuration.value = 1f
        _fadeOutDuration.value = 1f
        _pitchSemitones.value = 0
        _volumePercent.value = 100
        _exportState.value = ExportState.Idle
    }

    fun setInPoint(ms: Long) {
        _inPointMs.value = ms
        val out = _outPointMs.value
        if (out != null && ms >= out) {
            _outPointMs.value = null
        }
    }

    fun setOutPoint(ms: Long) {
        _outPointMs.value = ms
        val inp = _inPointMs.value
        if (inp != null && ms <= inp) {
            _inPointMs.value = null
        }
    }

    fun setFadeInEnabled(enabled: Boolean) {
        _fadeInEnabled.value = enabled
    }

    fun setFadeOutEnabled(enabled: Boolean) {
        _fadeOutEnabled.value = enabled
    }

    fun setFadeInDuration(seconds: Float) {
        _fadeInDuration.value = seconds.coerceIn(0f, 5f)
    }

    fun setFadeOutDuration(seconds: Float) {
        _fadeOutDuration.value = seconds.coerceIn(0f, 5f)
    }

    fun pitchUp() {
        _pitchSemitones.value = (_pitchSemitones.value + 1).coerceAtMost(12)
    }

    fun pitchDown() {
        _pitchSemitones.value = (_pitchSemitones.value - 1).coerceAtLeast(-12)
    }

    fun resetPitch() {
        _pitchSemitones.value = 0
    }

    fun setVolumePercent(percent: Int) {
        _volumePercent.value = percent.coerceIn(0, 200)
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun export(context: Context) {
        val uri = _audioUri.value ?: return
        val inMs = _inPointMs.value ?: 0L
        val outMs = _outPointMs.value ?: _durationMs.value
        if (outMs <= inMs) return

        _exportState.value = ExportState.Exporting

        viewModelScope.launch(Dispatchers.IO) {
            var tempInputFile: File? = null
            try {
                // Copy input to temp file — /proc/self/fd/ is unreliable with FFmpegKit on many devices
                tempInputFile = File(context.cacheDir, "audioclipper_input_${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempInputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    _exportState.value = ExportState.Error("Cannot open audio file")
                    return@launch
                }

                val inputPath = tempInputFile.absolutePath

                val outputDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "AudioClipper"
                )
                if (!outputDir.exists()) outputDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val baseName = _audioFileName.value
                    .substringBeforeLast(".")
                    .replace(" ", "_")
                val outputFile = File(outputDir, "${baseName}_clip_${timestamp}.mp3")
                val outputPath = outputFile.absolutePath

                val startSec = inMs / 1000.0
                val endSec = outMs / 1000.0
                val clipDuration = endSec - startSec

                val audioFilters = mutableListOf<String>()

                // Pitch shift
                val semitones = _pitchSemitones.value
                if (semitones != 0) {
                    val factor = 2.0.pow(semitones / 12.0)
                    val newRate = (44100 * factor).toInt()
                    audioFilters.add("asetrate=$newRate,aresample=44100")
                }

                // Volume
                val vol = _volumePercent.value
                if (vol != 100) {
                    val volFloat = vol / 100.0
                    audioFilters.add("volume=${"%.2f".format(volFloat)}")
                }

                // Fade in
                if (_fadeInEnabled.value && _fadeInDuration.value > 0f) {
                    audioFilters.add("afade=t=in:d=${_fadeInDuration.value}")
                }

                // Fade out
                if (_fadeOutEnabled.value && _fadeOutDuration.value > 0f) {
                    val fadeOutStart = clipDuration - _fadeOutDuration.value
                    if (fadeOutStart > 0) {
                        audioFilters.add("afade=t=out:st=${"%.2f".format(fadeOutStart)}:d=${_fadeOutDuration.value}")
                    }
                }

                val command = buildString {
                    append("-ss $startSec -to $endSec ")
                    append("-i \"$inputPath\" ")
                    if (audioFilters.isNotEmpty()) {
                        append("-af \"${audioFilters.joinToString(",")}\" ")
                    }
                    append("-acodec libmp3lame -b:a 192k ")
                    append("-y \"$outputPath\"")
                }

                val session = FFmpegKit.execute(command)

                if (ReturnCode.isSuccess(session.returnCode)) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(outputPath),
                        arrayOf("audio/mpeg"),
                        null
                    )
                    _exportState.value = ExportState.Done(outputPath)
                } else {
                    val logs = session.allLogsAsString ?: "Unknown error"
                    _exportState.value = ExportState.Error("Export failed: $logs")
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Error: ${e.message}")
            } finally {
                tempInputFile?.delete()
            }
        }
    }
}
