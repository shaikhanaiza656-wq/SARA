package com.yourorg.systemcore.data.network

import com.yourorg.systemcore.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val dataSource: NetworkDataSource
) : NetworkRepository {
    override fun observeStatus(): Flow<NetworkStatus> = dataSource.observeNetworkStatus()
}
