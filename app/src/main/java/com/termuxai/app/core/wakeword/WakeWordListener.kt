package com.termuxai.app.core.wakeword

import android.content.Context
import android.util.Log
import com.termuxai.app.core.AiState
import com.termuxai.app.core.AiStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Real, always-on, fully OFFLINE wake word detection using Vosk
 * (https://alphacephei.com/vosk/) — Apache 2.0 licensed, no account, no API
 * key, no signup wall of any kind. This replaces an earlier Picovoice
 * Porcupine-based version specifically because Picovoice Console's signup
 * flow was blocking on a "company email" requirement.
 *
 * This is a genuine speech recognizer (Kaldi-based) constrained to a tiny
 * vocabulary via a JSON grammar (`["sara", "[unk]"]`) so it behaves like a
 * lightweight wake-word engine instead of doing full transcription — not a
 * simulated microphone loop.
 *
 * ONE thing this class needs that cannot be fabricated here: the actual
 * Vosk model files. There is no signup involved, just a direct download:
 *
 *   1. Download (no account needed): https://alphacephei.com/vosk/models
 *      -> "vosk-model-small-en-us-0.15" (~40MB zip)
 *   2. Unzip it. You'll get a folder containing subfolders like am/, conf/,
 *      graph/, ivector/.
 *   3. Copy the CONTENTS of that folder (not the folder itself) into
 *      app/src/main/assets/model-en-us/ — so you end up with
 *      app/src/main/assets/model-en-us/am/, .../conf/, etc.
 *
 * Mic discipline (per spec): [start] is called exactly once, from
 * [com.termuxai.app.core.service.WakeWordService.onCreate]. It is never
 * stopped and restarted while the app is alive. Self-trigger prevention
 * during TTS playback is done by ignoring callbacks while [aiStateHolder]
 * reports SPEAKING (see [onDetected]), not by tearing down the audio tap.
 */
class WakeWordListener(
    private val context: Context,
    private val scope: CoroutineScope,
    private val aiStateHolder: AiStateHolder,
    private val wakeWord: String = "sara",
    private val modelAssetName: String = "model-en-us"
) {
    companion object {
        private const val TAG = "WakeWordListener"
        private const val SAMPLE_RATE = 16000.0f
    }

    private var speechService: SpeechService? = null
    private var currentAiState: AiState = AiState.SLEEPING

    private val _detections = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val detections: SharedFlow<Unit> = _detections.asSharedFlow()

    // Vosk's model loading is async (StorageService.unpack callbacks), so
    // failures can't be caught by a plain try/catch around start() the way
    // the old synchronous Picovoice API allowed. Callers (WakeWordService)
    // observe this to surface real failures to the user instead of silently
    // pretending to listen.
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        scope.launch {
            aiStateHolder.state.collect { currentAiState = it }
        }
    }

    /**
     * Call exactly once. Unpacks the bundled model into internal storage
     * (Vosk's real, documented mechanism — [StorageService.unpack] — copies
     * from the APK's assets to a writable directory the native decoder can
     * open; this only actually copies files the first time it runs) and
     * then opens the real mic tap.
     */
    fun start() {
        if (speechService != null) {
            Log.w(TAG, "start() called while already running — ignoring to avoid a duplicate mic tap")
            return
        }

        StorageService.unpack(
            context,
            modelAssetName,
            "model",
            { model -> startRecognizer(model) },
            { exception ->
                val message = "Failed to load wake word model from assets/$modelAssetName " +
                    "(did you add the Vosk model files? see WakeWordListener.kt): ${exception.message}"
                Log.e(TAG, message, exception)
                scope.launch { _errors.emit(message) }
            }
        )
    }

    private fun startRecognizer(model: Model) {
        try {
            // Restricting the grammar to just the wake word (+ an "unknown"
            // catch-all Vosk requires) is what keeps this lightweight and
            // wake-word-like, rather than doing full open vocabulary
            // transcription 24/7.
            val grammar = """["$wakeWord", "[unk]"]"""
            val recognizer = Recognizer(model, SAMPLE_RATE, grammar)

            val service = SpeechService(recognizer, SAMPLE_RATE)
            speechService = service
            service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    // Partial (in-progress) hypotheses are noisier than
                    // final ones; we deliberately only act on onResult /
                    // onFinalResult to avoid double-firing on one utterance.
                }

                override fun onResult(hypothesis: String?) {
                    if (containsWakeWord(hypothesis)) onDetected()
                }

                override fun onFinalResult(hypothesis: String?) {
                    if (containsWakeWord(hypothesis)) onDetected()
                }

                override fun onError(exception: Exception?) {
                    Log.e(TAG, "Vosk recognition error: ${exception?.message}", exception)
                }

                override fun onTimeout() {
                    // No-op: we want continuous always-on listening, not a
                    // one-shot session that needs manual restart.
                }
            })
            Log.i(TAG, "Wake word engine started (Vosk, offline), listening for \"$wakeWord\"")
        } catch (e: Exception) {
            val message = "Failed to start Vosk recognizer: ${e.message}"
            Log.e(TAG, message, e)
            scope.launch { _errors.emit(message) }
        }
    }

    private fun containsWakeWord(hypothesisJson: String?): Boolean {
        if (hypothesisJson.isNullOrBlank()) return false
        return try {
            val text = JSONObject(hypothesisJson).optString("text", "")
            text.contains(wakeWord, ignoreCase = true)
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse Vosk hypothesis JSON: $hypothesisJson", e)
            false
        }
    }

    private fun onDetected() {
        // Loop protection: while the assistant itself is speaking, its own
        // TTS output could otherwise re-trigger detection. We don't touch
        // the mic tap for this — we just drop the event.
        if (currentAiState == AiState.SPEAKING) {
            Log.d(TAG, "Ignored wake word detection while AI is SPEAKING (self-trigger guard)")
            return
        }
        Log.i(TAG, "Wake word \"$wakeWord\" detected")
        aiStateHolder.set(AiState.LISTENING)
        scope.launch { _detections.emit(Unit) }
    }

    /** Call exactly once, from service teardown (process death), never as part of normal operation. */
    fun stop() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }
}
