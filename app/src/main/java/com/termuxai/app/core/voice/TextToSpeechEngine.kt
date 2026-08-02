package com.termuxai.app.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Real Android TextToSpeech wrapper. Every utterance is synthesized on-device
 * by the configured system TTS engine at call time -- no pre-recorded audio,
 * no fake delay standing in for playback.
 */
class TextToSpeechEngine(context: Context) {

    companion object {
        private const val TAG = "TextToSpeechEngine"
    }

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ready = true
            } else {
                Log.e(TAG, "TextToSpeech engine failed to initialize (status=$status)")
            }
        }
    }

    /** [onDone] fires once real playback finishes, or immediately if TTS isn't ready/failed. */
    fun speak(text: String, onDone: () -> Unit) {
        val engine = tts
        if (!ready || engine == null) {
            Log.w(TAG, "TTS not ready, skipping speech for: $text")
            onDone()
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone()
            }
            @Deprecated("Deprecated in the platform API, still the callback that fires on real devices")
            override fun onError(utteranceId: String?) {
                onDone()
            }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
