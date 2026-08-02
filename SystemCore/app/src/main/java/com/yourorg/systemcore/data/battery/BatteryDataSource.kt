package com.yourorg.systemcore.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.yourorg.systemcore.domain.model.BatteryHealth
import com.yourorg.systemcore.domain.model.BatteryStatus
import com.yourorg.systemcore.domain.model.ChargePlug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads real battery data from the platform. ACTION_BATTERY_CHANGED is a sticky broadcast,
 * meaning registering the receiver immediately delivers the current battery state - no
 * polling, no fabricated defaults while waiting for the first update.
 */
@Singleton
class BatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun observeBatteryStatus(): Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { trySend(it.toBatteryStatus()) }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = context.registerReceiver(receiver, filter)
        sticky?.let { trySend(it.toBatteryStatus()) }

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun Intent.toBatteryStatus(): BatteryStatus {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargePlug = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargePlug.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargePlug.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargePlug.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK -> ChargePlug.DOCK
            0 -> ChargePlug.NONE
            else -> ChargePlug.UNKNOWN
        }

        val health = when (getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }

        val tempTenths = getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val temperatureCelsius = tempTenths / 10f

        val voltage = getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val technology = getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

        return BatteryStatus(
            percentage = percentage,
            isCharging = isCharging,
            chargePlug = chargePlug,
            health = health,
            temperatureCelsius = temperatureCelsius,
            voltageMillivolts = voltage,
            technology = technology
        )
    }
}
