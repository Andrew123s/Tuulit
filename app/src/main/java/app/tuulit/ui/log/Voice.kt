package app.tuulit.ui.log

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.tuulit.data.remote.ApiError
import app.tuulit.data.remote.AuthState
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.launch
import java.io.File

/**
 * Voice journaling. "Dictate" turns speech into the day's note (on-device recogniser when the
 * phone has one). "Record" keeps an audio note in app-private storage; signed-in users can choose
 * to back it up to private R2 storage. Microphone permission is asked for at the moment of use.
 */
@Composable
fun VoiceJournal(day: Long, onTranscript: (String) -> Unit, hasKey: Boolean, onBackedUp: (String) -> Unit) {
    val ctx = LocalContext.current
    val c = container()
    val scope = rememberCoroutineScope()
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    var mode by remember { mutableStateOf<String?>(null) } // "dictate" | "record" | "play"
    var status by remember { mutableStateOf<String?>(null) }
    var hasRecording by remember(day) { mutableStateOf(c.media.hasLocal(day)) }
    var pending by remember { mutableStateOf<String?>(null) }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val recognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun stopAll() {
        runCatching { recorder.value?.stop() }; recorder.value?.release(); recorder.value = null
        player.value?.release(); player.value = null
        recognizer.value?.destroy(); recognizer.value = null
        if (mode == "record") hasRecording = c.media.hasLocal(day)
        mode = null
    }
    DisposableEffect(day) { onDispose { stopAll() } }

    fun startDictation() {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { status = "Speech recognition isn't available on this phone."; return }
        val sr = SpeechRecognizer.createSpeechRecognizer(ctx)
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onTranscript)
                status = "Added to your note."; stopAll()
            }
            override fun onError(error: Int) {
                status = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) "Didn't catch that. Try again." else "Dictation stopped."
                stopAll()
            }
            override fun onReadyForSpeech(params: Bundle?) { status = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status = "Processing…" }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer.value = sr; mode = "dictate"; sr.startListening(intent)
    }

    fun startRecording() {
        val file = c.media.localFile(day)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setMaxDuration(5 * 60_000)
            r.setOutputFile(file.absolutePath)
            r.prepare(); r.start()
            recorder.value = r; mode = "record"; status = "Recording… up to 5 minutes."
        } catch (e: Exception) {
            r.release(); status = "Couldn't start recording."
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { status = "Microphone access is off. You can allow it in system settings."; pending = null; return@rememberLauncherForActivityResult }
        when (pending) { "dictate" -> startDictation(); "record" -> startRecording() }
        pending = null
    }
    fun withMic(action: String) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (action == "dictate") startDictation() else startRecording()
        } else { pending = action; micPermission.launch(Manifest.permission.RECORD_AUDIO) }
    }

    Column(Modifier.padding(top = 12.dp)) {
        T("Voice journal", Tuulit.type.bodySm, Tuulit.colors.inkMuted)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mode == null) {
                TButton("Dictate", { withMic("dictate") }, kind = ButtonKind.QUIET, icon = TuulitIcons.Mic, small = true)
                TButton(if (hasRecording) "Re-record" else "Record", { withMic("record") }, kind = ButtonKind.QUIET, icon = TuulitIcons.Mic, small = true)
                if (hasRecording) TButton("Play", {
                    val mp = MediaPlayer()
                    runCatching {
                        mp.setDataSource(c.media.localFile(day).absolutePath); mp.prepare(); mp.start()
                        mp.setOnCompletionListener { stopAll() }
                        player.value = mp; mode = "play"
                    }.onFailure { mp.release(); status = "Couldn't play the recording." }
                }, kind = ButtonKind.QUIET, small = true)
            } else {
                TButton("Stop", { stopAll(); if (status?.startsWith("Recording") == true) status = "Recording saved on this phone." }, kind = ButtonKind.PRIMARY, small = true)
            }
        }
        if (hasRecording && mode == null) {
            val signedIn = auth is AuthState.SignedIn
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (signedIn && !hasKey) TButton("Back up recording", {
                    status = "Uploading…"
                    scope.launch {
                        status = try { onBackedUp(c.media.backup(day)); "Backed up privately." } catch (e: ApiError) { e.message } catch (e: Exception) { "Upload failed." }
                    }
                }, kind = ButtonKind.TEXT)
                TButton("Delete recording", { c.media.deleteLocal(day); hasRecording = false; status = "Recording deleted from this phone." }, kind = ButtonKind.TEXT)
            }
        }
        status?.let { T(it, Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 4.dp)) }
    }
}
