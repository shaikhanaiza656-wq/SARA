package com.yourorg.systemcore.domain.model

/**
 * Real battery state read from [android.os.BatteryManager] / the ACTION_BATTERY_CHANGED
 * sticky broadcast. Every field here is sourced from the OS - nothing is fabricated
 * or estimated.
 */
data class BatteryStatus(
    val percentage: Int,          // 0-100, from EXTRA_LEVEL / EXTRA_SCALE
    val isCharging: Boolean,      // from EXTRA_STATUS == BATTERY_STATUS_CHARGING/FULL
    val chargePlug: ChargePlug,   // from EXTRA_PLUGGED
    val health: BatteryHealth,    // from EXTRA_HEALTH
    val temperatureCelsius: Float,// from EXTRA_TEMPERATURE (tenths of a degree in the raw extra)
    val voltageMillivolts: Int,   // from EXTRA_VOLTAGE
    val technology: String?       // from EXTRA_TECHNOLOGY, e.g. "Li-ion"
)

enum class ChargePlug { AC, USB, WIRELESS, DOCK, NONE, UNKNOWN }

enum class BatteryHealth { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD, UNKNOWN }
