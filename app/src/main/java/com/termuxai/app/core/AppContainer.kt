package com.termuxai.app.core

import android.content.Context
import com.termuxai.app.core.local.LocalCommandHandler
import com.termuxai.app.core.network.TermuxWebSocketClient
import com.termuxai.app.core.network.buildTermuxOkHttpClient
import com.termuxai.app.core.repository.DefaultTermuxRepository
import com.termuxai.app.core.repository.TermuxRepository
import com.termuxai.app.core.voice.SpeechToTextEngine
import com.termuxai.app.core.voice.TextToSpeechEngine
import com.termuxai.app.core.wakeword.WakeWordListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/**
 * Deliberately simple, explicit dependency injection (a service locator
 * rather than Hilt/Dagger). Reasoning: this container has exactly one
 * process-scoped dependency graph -- there's no per-screen or per-Activity
 * scoping need here -- so hand-written DI keeps the graph obvious to read and
 * avoids pulling in an annotation-processing toolchain for a single-graph
 * app. If the project grows real scoping needs (e.g. per-window DI), this is
 * the place to introduce Hilt.
 *
 * Lives on [com.termuxai.app.TermuxApplication] and outlives every
 * Activity/Service, which is required: the WebSocket connection and the
 * wake word engine must both be able to keep running when the Activity is
 * destroyed and only a foreground Service is holding the process alive.
 */
class AppContainer(private val appContext: Context) {
    // Process-scoped coroutine scope. SupervisorJob so a failure in one
    // collector (e.g. a bad incoming message) doesn't cancel the reconnect
    // loop or any other independent coroutine sharing this scope.
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val json = Json {
        ignoreUnknownKeys = true // Termux can add new fields without breaking the APK
        explicitNulls = false
    }

    private val okHttpClient = buildTermuxOkHttpClient()

    val webSocketClient = TermuxWebSocketClient(
        okHttpClient = okHttpClient,
        json = json,
        scope = applicationScope
    )

    val termuxRepository: TermuxRepository = DefaultTermuxRepository(webSocketClient)

    val aiStateHolder = AiStateHolder()

    // No access key/account needed -- WakeWordListener uses Vosk, an
    // offline, open-source engine. See WakeWordListener.kt for the model
    // asset setup this still requires (a direct download, no signup).
    val wakeWordListener = WakeWordListener(
        context = appContext,
        scope = applicationScope,
        aiStateHolder = aiStateHolder
    )

    // Real on-device STT/TTS (android.speech.* framework APIs), wired to the
    // Termux round-trip: WakeWordService captures speech with this after a
    // wake word fires; assistantOrchestrator speaks Termux's replies aloud.
    val speechToTextEngine = SpeechToTextEngine(appContext)
    private val textToSpeechEngine = TextToSpeechEngine(appContext)

    // Answers time/date/battery/torch commands on-device, without needing
    // the Termux connection. See LocalCommandHandler for the full list.
    private val localCommandHandler = LocalCommandHandler(appContext)

    val assistantOrchestrator = AssistantOrchestrator(
        scope = applicationScope,
        repository = termuxRepository,
        aiStateHolder = aiStateHolder,
        textToSpeechEngine = textToSpeechEngine,
        speechToTextEngine = speechToTextEngine,
        localCommandHandler = localCommandHandler
    ).also { it.start() }
}
