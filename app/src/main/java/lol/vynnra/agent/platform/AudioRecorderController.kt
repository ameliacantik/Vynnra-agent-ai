package lol.vynnra.agent.platform

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioRecorderController(context: Context) {
    private val appContext = context.applicationContext
    private val recordingsDir = File(appContext.filesDir, "recordings").apply { mkdirs() }
    private val handler = Handler(Looper.getMainLooper())
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var startedAt: Long = 0L

    private val _state = MutableStateFlow(AudioRecorderState())
    val state: StateFlow<AudioRecorderState> = _state.asStateFlow()

    private val ticker = object : Runnable {
        override fun run() {
            if (_state.value.recording) {
                _state.value = _state.value.copy(durationMs = System.currentTimeMillis() - startedAt)
                handler.postDelayed(this, 500L)
            }
        }
    }

    fun start(): Boolean {
        if (_state.value.recording) return false
        stopPlayback()
        return runCatching {
            val file = File(recordingsDir, "vynnra_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentFile = file
            startedAt = System.currentTimeMillis()
            _state.value = AudioRecorderState(
                recording = true,
                fileName = file.name,
                startedAt = startedAt,
                durationMs = 0L
            )
            handler.post(ticker)
            true
        }.getOrElse { error ->
            _state.value = AudioRecorderState(
                error = error.message ?: "Unable to start audio recording."
            )
            recorder?.release()
            recorder = null
            currentFile = null
            false
        }
    }

    fun stop(): AudioRecording? {
        if (!_state.value.recording) return null
        val file = currentFile
        val duration = System.currentTimeMillis() - startedAt
        handler.removeCallbacks(ticker)

        val saved = runCatching {
            recorder?.stop()
            recorder?.reset()
            file?.takeIf { it.exists() && it.length() > 0L }?.let {
                AudioRecording(
                    name = it.name,
                    path = it.absolutePath,
                    durationMs = duration.coerceAtLeast(1L),
                    sizeBytes = it.length()
                )
            }
        }.getOrNull()

        recorder?.release()
        recorder = null
        currentFile = null
        _state.value = AudioRecorderState(
            recording = false,
            lastRecording = saved,
            error = if (saved == null) "Recording was too short or could not be saved." else null
        )
        return saved
    }

    fun listRecordings(): List<AudioRecording> =
        recordingsDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("m4a", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .map {
                AudioRecording(
                    name = it.name,
                    path = it.absolutePath,
                    durationMs = 0L,
                    sizeBytes = it.length()
                )
            }

    fun play(recording: AudioRecording): Boolean {
        return runCatching {
            stopPlayback()
            player = MediaPlayer().apply {
                setDataSource(recording.path)
                setOnCompletionListener {
                    stopPlayback()
                }
                prepare()
                start()
            }
            _state.value = _state.value.copy(playingPath = recording.path, error = null)
            true
        }.getOrElse {
            _state.value = _state.value.copy(error = it.message ?: "Unable to play recording.")
            false
        }
    }

    fun stopPlayback() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        _state.value = _state.value.copy(playingPath = null)
    }

    fun delete(recording: AudioRecording): Boolean {
        if (_state.value.recording || _state.value.playingPath == recording.path) {
            stopPlayback()
        }
        return File(recording.path).delete()
    }

    fun shutdown() {
        handler.removeCallbacks(ticker)
        if (_state.value.recording) stop()
        stopPlayback()
    }
}

data class AudioRecorderState(
    val recording: Boolean = false,
    val fileName: String? = null,
    val startedAt: Long? = null,
    val durationMs: Long = 0L,
    val lastRecording: AudioRecording? = null,
    val playingPath: String? = null,
    val error: String? = null
)

data class AudioRecording(
    val name: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long
)
