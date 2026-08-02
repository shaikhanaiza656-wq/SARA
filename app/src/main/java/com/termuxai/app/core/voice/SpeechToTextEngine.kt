package com.termuxai.app.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Real, on-device Android speech recognition (android.speech.SpeechRecognizer)
 * -- no fake transcription, no simulated results. Requires RECORD_AUDIO
 * (already declared in the manifest) and a speech recognition service on the
 * device (present on virtually every phone with the Google app installed).
 *
 * SpeechRecognizer must be created and driven from the main thread per
 * Android's documented contract, so [start] always hops onto
 * Dispatchers.Main even though callers (e.g. WakeWordService) run on a
 * background service scope.
 */
class SpeechToTextEngine(private val context: Context) {

    companion object {
        private const val TAG = "SpeechToTextEngine"
    }

    private var activeRecognizer: SpeechRecognizer? = null

    /**
     * Starts one real listening session. [onResult] fires exactly once, with
     * the best transcription or null if recognition failed, timed out, or no
     * recognizer is available -- callers must not assume text always arrives.
     */
    fun start(scope: CoroutineScope, onResult: (String?) -> Unit) {
        scope.launch(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "No speech recognition service available on this device")
                onResult(null)
                return@launch
            }

            activeRecognizer?.destroy()
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            activeRecognizer = sr

            sr.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    onResult(matches?.firstOrNull())
                    sr.destroy()
                    if (activeRecognizer === sr) activeRecognizer = null
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "Speech recognition error code: $error")
                    onResult(null)
                    sr.destroy()
                    if (activeRecognizer === sr) activeRecognizer = null
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            sr.startListening(intent)
        }
    }

    fun stop() {
        activeRecognizer?.destroy()
        activeRecognizer = null
    }
}
