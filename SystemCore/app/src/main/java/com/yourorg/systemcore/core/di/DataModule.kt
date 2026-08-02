package com.yourorg.systemcore.core.di

import com.yourorg.systemcore.data.battery.BatteryRepository
import com.yourorg.systemcore.data.battery.BatteryRepositoryImpl
import com.yourorg.systemcore.data.network.NetworkRepository
import com.yourorg.systemcore.data.network.NetworkRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepository

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(impl: NetworkRepositoryImpl): NetworkRepository
}
