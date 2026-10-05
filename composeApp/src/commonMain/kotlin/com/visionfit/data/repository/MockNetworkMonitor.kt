package com.visionfit.data.repository

import com.visionfit.core.time.TimeProvider
import com.visionfit.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Always online by default. Call [setOnline] (from a debug menu or a test) to see the offline
 * banner and the "waiting for network" meal card.
 */
class MockNetworkMonitor(private val timeProvider: TimeProvider) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow(timeProvider.now())
    override val lastSyncedAt = _lastSyncedAt.asStateFlow()

    fun setOnline(online: Boolean) {
        if (online) _lastSyncedAt.value = timeProvider.now()
        _isOnline.value = online
    }
}
