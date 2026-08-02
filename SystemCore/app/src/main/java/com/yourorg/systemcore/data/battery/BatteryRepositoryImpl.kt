package com.yourorg.systemcore.data.battery

import com.yourorg.systemcore.domain.model.BatteryStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val dataSource: BatteryDataSource
) : BatteryRepository {
    override fun observeStatus(): Flow<BatteryStatus> = dataSource.observeBatteryStatus()
}
