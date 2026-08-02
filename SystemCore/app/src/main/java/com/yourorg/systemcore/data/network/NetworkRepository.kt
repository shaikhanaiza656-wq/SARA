package com.yourorg.systemcore.data.network

import com.yourorg.systemcore.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun observeStatus(): Flow<NetworkStatus>
}
