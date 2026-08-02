package com.termuxai.app.core.local

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles simple device commands (time, date, battery, torch) entirely
 * on-device, without a round trip to the Termux server.
 *
 * Rationale: things like "what time is it" or "turn on the torch" don't
 * need an AI model or shell access -- they're plain Android API calls.
 * Keeping them local means:
 *   - they still work even if Termux isn't running / not connected
 *   - they respond instantly instead of waiting on a WebSocket round trip
 *
 * [AssistantOrchestrator] should call [handle] first; only if it returns
 * null does the recognized text get sent on to Termux as before.
 */
class LocalCommandHandler(private val appContext: Context) {

    private val cameraManager: CameraManager
        get() = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // Torch state is tracked here because CameraManager has no "getter" for
    // current flash state -- only setTorchMode(id, boolean).
    private var torchOn = false
    private val torchCameraId: String? by lazy {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * @return a spoken reply if [text] matched a local command, or null if
     * it didn't match anything and should be forwarded to Termux instead.
     */
    fun handle(text: String): String? {
        val normalized = text.trim().lowercase(Locale.getDefault())

        return when {
            containsAny(normalized, "time", "samay", "waqt") -> currentTime()
            containsAny(normalized, "date", "tareekh", "tarikh") -> currentDate()
            containsAny(normalized, "battery") -> batteryStatus()
            containsAny(normalized, "torch", "flash", "flashlight") -> toggleTorch(normalized)
            else -> null
        }
    }

    private fun containsAny(text: String, vararg keywords: String) =
        keywords.any { text.contains(it) }

    private fun currentTime(): String {
        val formatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return "It's $formatted"
    }

    private fun currentDate(): String {
        val formatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        return "Today is $formatted"
    }

    private fun batteryStatus(): String {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = appContext.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level == -1 || scale == -1 || scale == 0) return "I couldn't read the battery level."
        val percent = (level * 100) / scale
        val chargingStatus = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargingStatus == BatteryManager.BATTERY_STATUS_FULL
        return "Battery is at $percent%" + if (isCharging) ", and charging" else ""
    }

    private fun toggleTorch(normalized: String): String {
        val cameraId = torchCameraId ?: return "This device doesn't have a flash."
        val wantsOff = containsAny(normalized, "off", "band")
        val wantsOn = containsAny(normalized, "on", "chalu", "chalao")
        val newState = when {
            wantsOff -> false
            wantsOn -> true
            else -> !torchOn // plain "torch" toggles
        }
        return try {
            cameraManager.setTorchMode(cameraId, newState)
            torchOn = newState
            if (newState) "Torch on" else "Torch off"
        } catch (e: Exception) {
            "Couldn't switch the torch."
        }
    }
}
