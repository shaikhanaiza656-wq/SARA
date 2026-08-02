package com.yourorg.systemcore.data.battery

import com.yourorg.systemcore.domain.model.BatteryStatus
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun observeStatus(): Flow<BatteryStatus>
}
