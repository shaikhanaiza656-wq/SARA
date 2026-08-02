package com.termuxai.app.core

import com.termuxai.app.core.local.LocalCommandHandler
import com.termuxai.app.core.network.TermuxEnvelope
import com.termuxai.app.core.repository.TermuxRepository
import com.termuxai.app.core.voice.SpeechToTextEngine
import com.termuxai.app.core.voice.TextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Process-scoped sequencer for the whole wake-word -> greeting -> listen ->
 * command -> reply loop. Deliberately owns BOTH the TTS output and the STT
 * capture trigger, because the two must never run at the same time -- if
 * speech capture started while the greeting was still being spoken, the mic
 * would be picking up the assistant's own voice instead of the user's.
 *
 * Flow:
 *  1. [com.termuxai.app.core.service.WakeWordService] detects "Sara" and
 *     sends a "wakeword.detected" envelope to Termux.
 *  2. Termux replies "assistant.greeting" -> this class speaks it via TTS,
 *     and ONLY once that playback genuinely finishes does it start real
 *     speech capture via [SpeechToTextEngine].
 *  3. The transcription is sent to Termux as "command.execute".
 *  4. Termux replies "assistant.reply" -> spoken via TTS, then back to
 *     SLEEPING.
 */
class AssistantOrchestrator(
    private val scope: CoroutineScope,
    private val repository: TermuxRepository,
    private val aiStateHolder: AiStateHolder,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val speechToTextEngine: SpeechToTextEngine,
    private val localCommandHandler: LocalCommandHandler
) {
    /** Call once from AppContainer's init. */
    fun start() {
        repository.incomingMessages
            .onEach { envelope -> handleEnvelope(envelope) }
            .launchIn(scope)
    }

    /**
     * Skips the "wait for Termux's spoken greeting" step and opens the mic
     * immediately. Used when [WakeWordService] sees the Termux connection is
     * not currently Connected -- without this, a wake word while Termux is
     * offline would just sit waiting for a greeting that will never arrive,
     * and local-only commands (time/date/battery/torch) would never get a
     * chance to be heard at all.
     */
    fun listenDirectly() {
        aiStateHolder.set(AiState.LISTENING)
        captureCommand()
    }

    private fun handleEnvelope(envelope: TermuxEnvelope) {
        val text = (envelope.payload as? JsonPrimitive)?.contentOrNull ?: return

        when (envelope.type) {
            "assistant.greeting" -> {
                aiStateHolder.set(AiState.SPEAKING)
                textToSpeechEngine.speak(text) {
                    // Only now, after the greeting has genuinely finished
                    // playing, do we open the mic for the user's command.
                    aiStateHolder.set(AiState.LISTENING)
                    captureCommand()
                }
            }

            "assistant.reply" -> {
                aiStateHolder.set(AiState.SPEAKING)
                textToSpeechEngine.speak(text) {
                    aiStateHolder.set(AiState.SLEEPING)
                }
            }
        }
    }

    private fun captureCommand() {
        speechToTextEngine.start(scope) { recognizedText ->
            if (recognizedText.isNullOrBlank()) {
                aiStateHolder.set(AiState.SLEEPING)
                return@start
            }

            // Basic device commands (time/date/battery/torch) are answered
            // on-device -- no need to wait on Termux, and they still work
            // even if the Termux connection is down.
            val localReply = localCommandHandler.handle(recognizedText)
            if (localReply != null) {
                aiStateHolder.set(AiState.SPEAKING)
                textToSpeechEngine.speak(localReply) {
                    aiStateHolder.set(AiState.SLEEPING)
                }
                return@start
            }

            aiStateHolder.set(AiState.THINKING)
            val sent = repository.send(
                TermuxEnvelope(type = "command.execute", payload = JsonPrimitive(recognizedText))
            )
            if (!sent) {
                // No live Termux connection and this wasn't a local command
                // -- say so instead of leaving the UI stuck on THINKING with
                // no explanation.
                aiStateHolder.set(AiState.SPEAKING)
                textToSpeechEngine.speak("I'm not connected to Termux right now.") {
                    aiStateHolder.set(AiState.SLEEPING)
                }
            }
        }
    }
}
