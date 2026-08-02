package com.termuxai.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The assistant's current activity, as shown in the Left Panel
 * (Listening / Thinking / Speaking / Sleeping) and used to gate wake-word
 * self-triggering: while state is Speaking, the wake word engine keeps its
 * microphone tap open (per spec: "mic starts only once", "always listening")
 * but ignores detections, so the assistant's own TTS output can never
 * re-trigger itself.
 */
enum class AiState { SLEEPING, LISTENING, THINKING, SPEAKING }

class AiStateHolder {
    private val _state = MutableStateFlow(AiState.SLEEPING)
    val state: StateFlow<AiState> = _state.asStateFlow()

    fun set(newState: AiState) {
        _state.value = newState
    }
}
