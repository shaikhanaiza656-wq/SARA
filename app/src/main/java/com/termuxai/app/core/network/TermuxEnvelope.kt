package com.termuxai.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * Wire format for every message exchanged with the Termux AI server.
 *
 * This is intentionally generic — the APK never hardcodes which "type"
 * values exist (no hardcoded command list, per spec). Repositories for each
 * feature (commands, modules, stats, music, logs) decode [payload] into
 * their own typed model based on [type]. If Termux adds a new type tomorrow,
 * this envelope still parses it; only the feature-specific decoder needs to
 * know about it.
 *
 * Example outgoing envelope (client -> Termux):
 *   { "type": "command.execute", "id": "…", "timestamp": …, "payload": "time batao" }
 *
 * Example incoming envelope (Termux -> client):
 *   { "type": "stats.update", "id": "…", "timestamp": …, "payload": { "cpu": 12.4, "memMb": 340 } }
 */
@Serializable
data class TermuxEnvelope(
    val type: String,
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val payload: JsonElement? = null
)

/** Well-known envelope types the client itself needs to recognize at the transport layer. */
object EnvelopeType {
    const val HELLO = "system.hello"
    const val PING = "system.ping"
    const val PONG = "system.pong"
    const val ERROR = "system.error"
}
