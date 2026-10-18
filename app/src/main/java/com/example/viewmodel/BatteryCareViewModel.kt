package com.example.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryAlertLogEntity
import com.example.data.BatteryCareDatabase
import com.example.data.BatteryPreferencesRepository
import com.example.data.ProtectionPreferencesState
import com.example.diagnostics.BatteryConditionGrade
import com.example.diagnostics.BatteryTelemetrySnapshot
import com.example.diagnostics.ChargingSpeedTier
import com.example.diagnostics.KernelBatteryDiagnostics
import com.example.service.BatteryManagerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class BatteryAppTab {
    DASHBOARD,
    DIAGNOSTICS,
    PROTECTION,
    TIPS
}

class BatteryCareViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val diagnostics = KernelBatteryDiagnostics(appContext)
    private val preferencesRepo = BatteryPreferencesRepository(appContext)
    private val alertLogDao = BatteryCareDatabase.getInstance(appContext).alertLogDao()

    private val _selectedTab = MutableStateFlow(BatteryAppTab.DASHBOARD)
    val selectedTab: StateFlow<BatteryAppTab> = _selectedTab.asStateFlow()

    val protectionPrefs: StateFlow<ProtectionPreferencesState> = preferencesRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProtectionPreferencesState()
        )

    val alertLogs: StateFlow<List<BatteryAlertLogEntity>> = alertLogDao.observeRecentAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _telemetry = MutableStateFlow(diagnostics.captureSnapshot())
    val telemetry: StateFlow<BatteryTelemetrySnapshot> = _telemetry.asStateFlow()

    // Live history of recent current (mA) readings for the real-time telemetry sparkline
    private val _currentHistoryMa = MutableStateFlow<List<Int>>(emptyList())
    val currentHistoryMa: StateFlow<List<Int>> = _currentHistoryMa.asStateFlow()

    private val batteryBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                refreshTelemetryNow()
            }
        }
    }

    private var isReceiverRegistered = false

    init {
        registerLiveBatteryReceiver()
        startHighFrequencyTelemetryLoop()
    }

    private fun registerLiveBatteryReceiver() {
        if (!isReceiverRegistered) {
            appContext.registerReceiver(
                batteryBroadcastReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            isReceiverRegistered = true
        }
    }

    private fun startHighFrequencyTelemetryLoop() {
        viewModelScope.launch {
            while (isActive) {
                refreshTelemetryNow()
                delay(2_500L)
            }
        }
    }

    fun refreshTelemetryNow() {
        val customMah = protectionPrefs.value.customDesignCapacityMah.takeIf { it > 0 }
        val snap = diagnostics.captureSnapshot(customMah)
        _telemetry.value = snap

        val updatedHistory = (_currentHistoryMa.value + snap.currentNowMilliamps).takeLast(24)
        _currentHistoryMa.value = updatedHistory
    }

    fun selectTab(tab: BatteryAppTab) {
        _selectedTab.value = tab
    }

    fun toggleBackgroundProtectionService(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setBackgroundGuardEnabled(enabled)
            if (enabled) {
                BatteryManagerService.startService(appContext)
            } else {
                BatteryManagerService.stopService(appContext)
            }
        }
    }

    fun setThermalAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setThermalAlertEnabled(enabled)
        }
    }

    fun setThermalThresholdCelsius(threshold: Float) {
        viewModelScope.launch {
            preferencesRepo.setThermalThresholdCelsius(threshold)
        }
    }

    fun setChargeLimit80Enabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setChargeLimit80Enabled(enabled)
        }
    }

    fun setChargeLimitPercentage(limitPct: Int) {
        viewModelScope.launch {
            preferencesRepo.setChargeLimitPercentage(limitPct)
        }
    }

    fun updateCustomDesignCapacity(capacityMah: Int) {
        viewModelScope.launch {
            preferencesRepo.setCustomDesignCapacityMah(capacityMah)
            val snap = diagnostics.captureSnapshot(capacityMah.takeIf { it > 0 })
            _telemetry.value = snap
        }
    }

    fun triggerSampleAlert(isThermal: Boolean) {
        BatteryManagerService.triggerTestAlert(appContext, isThermal = isThermal)
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            alertLogDao.clearAllAlerts()
        }
    }

    override fun onCleared() {
        if (isReceiverRegistered) {
            try {
                appContext.unregisterReceiver(batteryBroadcastReceiver)
            } catch (_: Exception) {
            }
            isReceiverRegistered = false
        }
        super.onCleared()
    }
}
