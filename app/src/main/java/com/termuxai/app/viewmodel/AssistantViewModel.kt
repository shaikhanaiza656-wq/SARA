package com.termuxai.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.termuxai.app.TermuxApplication
import com.termuxai.app.core.AiState
import com.termuxai.app.core.AiStateHolder
import com.termuxai.app.core.network.TermuxEnvelope
import com.termuxai.app.core.repository.TermuxRepository
import com.termuxai.app.core.service.WakeWordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * Drives the dashboard's AI-state avatar (Sleeping/Listening/Thinking/
 * Speaking), the "start listening" control for [WakeWordService], and a
 * short live log of envelopes received from Termux. Every value here comes
 * from a real source: [AiStateHolder] is only ever mutated by
 * [com.termuxai.app.core.wakeword.WakeWordListener] and
 * [com.termuxai.app.core.service.WakeWordService]; the log is only ever
 * appended to from real [TermuxRepository.incomingMessages] emissions — the
 * UI never invents entries.
 */
class AssistantViewModel(
    private val appContext: Context,
    aiStateHolder: AiStateHolder,
    private val repository: TermuxRepository
) : ViewModel() {

    val aiState: StateFlow<AiState> = aiStateHolder.state

    private val _messageLog = MutableStateFlow<List<String>>(emptyList())
    val messageLog: StateFlow<List<String>> = _messageLog.asStateFlow()

    init {
        repository.incomingMessages
            .onEach { envelope -> appendToLog(envelope) }
            .launchIn(viewModelScope)
    }

    private fun appendToLog(envelope: TermuxEnvelope) {
        val entry = "${envelope.type}: ${envelope.payload ?: "null"}"
        _messageLog.value = (_messageLog.value + entry).takeLast(MAX_LOG_ENTRIES)
    }

    fun startWakeWordEngine() {
        WakeWordService.start(appContext)
    }

    fun stopWakeWordEngine() {
        WakeWordService.stop(appContext)
    }

    /** Manual text command from the dashboard input field (fallback for when voice isn't used). */
    fun sendCommand(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.send(
                TermuxEnvelope(
                    type = "command.execute",
                    payload = JsonPrimitive(text)
                )
            )
        }
    }

    companion object {
        // Cap the in-memory log so a chatty Termux server can't grow this list
        // unbounded for the lifetime of the process.
        private const val MAX_LOG_ENTRIES = 200

        fun factory(appContext: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = appContext.applicationContext as TermuxApplication
                return AssistantViewModel(
                    appContext = appContext,
                    aiStateHolder = app.container.aiStateHolder,
                    repository = app.container.termuxRepository
                ) as T
            }
        }
    }
}
