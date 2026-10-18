package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "battery_protection_prefs")

data class ProtectionPreferencesState(
    val isBackgroundGuardEnabled: Boolean = false,
    val isThermalAlertEnabled: Boolean = true,
    val thermalThresholdCelsius: Float = 40.0f,
    val isChargeLimit80Enabled: Boolean = true,
    val chargeLimitPercentage: Int = 80,
    val customDesignCapacityMah: Int = 0 // 0 means auto-detect from kernel/PowerProfile
)

class BatteryPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_BG_GUARD_ENABLED = booleanPreferencesKey("bg_guard_enabled")
        private val KEY_THERMAL_ALERT_ENABLED = booleanPreferencesKey("thermal_alert_enabled")
        private val KEY_THERMAL_THRESHOLD = floatPreferencesKey("thermal_threshold_c")
        private val KEY_CHARGE_LIMIT_80_ENABLED = booleanPreferencesKey("charge_limit_80_enabled")
        private val KEY_CHARGE_LIMIT_PCT = intPreferencesKey("charge_limit_pct")
        private val KEY_CUSTOM_DESIGN_MAH = intPreferencesKey("custom_design_mah")
    }

    val preferencesFlow: Flow<ProtectionPreferencesState> = context.dataStore.data.map { prefs ->
        ProtectionPreferencesState(
            isBackgroundGuardEnabled = prefs[KEY_BG_GUARD_ENABLED] ?: false,
            isThermalAlertEnabled = prefs[KEY_THERMAL_ALERT_ENABLED] ?: true,
            thermalThresholdCelsius = prefs[KEY_THERMAL_THRESHOLD] ?: 40.0f,
            isChargeLimit80Enabled = prefs[KEY_CHARGE_LIMIT_80_ENABLED] ?: true,
            chargeLimitPercentage = prefs[KEY_CHARGE_LIMIT_PCT] ?: 80,
            customDesignCapacityMah = prefs[KEY_CUSTOM_DESIGN_MAH] ?: 0
        )
    }

    suspend fun setBackgroundGuardEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BG_GUARD_ENABLED] = enabled }
    }

    suspend fun setThermalAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_THERMAL_ALERT_ENABLED] = enabled }
    }

    suspend fun setThermalThresholdCelsius(threshold: Float) {
        context.dataStore.edit { it[KEY_THERMAL_THRESHOLD] = threshold.coerceIn(36.0f, 48.0f) }
    }

    suspend fun setChargeLimit80Enabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CHARGE_LIMIT_80_ENABLED] = enabled }
    }

    suspend fun setChargeLimitPercentage(limitPct: Int) {
        context.dataStore.edit { it[KEY_CHARGE_LIMIT_PCT] = limitPct.coerceIn(70, 95) }
    }

    suspend fun setCustomDesignCapacityMah(capacityMah: Int) {
        context.dataStore.edit { it[KEY_CUSTOM_DESIGN_MAH] = capacityMah }
    }
}
