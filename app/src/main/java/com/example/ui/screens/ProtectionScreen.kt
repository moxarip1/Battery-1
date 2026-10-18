package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.BatteryAlertLogEntity
import com.example.data.ProtectionPreferencesState
import com.example.diagnostics.BatteryTelemetrySnapshot
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProtectionScreen(
    snapshot: BatteryTelemetrySnapshot,
    prefs: ProtectionPreferencesState,
    alertLogs: List<BatteryAlertLogEntity>,
    onToggleBackgroundService: (Boolean) -> Unit,
    onToggleThermalAlert: (Boolean) -> Unit,
    onChangeThermalThreshold: (Float) -> Unit,
    onToggleChargeLimit80: (Boolean) -> Unit,
    onChangeChargeLimitPct: (Int) -> Unit,
    onTriggerTestAlert: (Boolean) -> Unit,
    onClearAlertLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onToggleBackgroundService(true)
    }

    val requestAndStartGuard: (Boolean) -> Unit = { enable ->
        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onToggleBackgroundService(true)
            }
        } else {
            onToggleBackgroundService(enable)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("protection_screen_list")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "FOREGROUND GUARD • SMART ALERTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "تنبيهات الحماية الذكية والخدمة الخلفية",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
        }

        // Foreground Service Master Control Card
        item {
            BackgroundServiceGuardCard(
                isEnabled = prefs.isBackgroundGuardEnabled,
                onToggle = requestAndStartGuard
            )
        }

        // Smart Thermal Alert Configuration Card
        item {
            ThermalProtectionConfigCard(
                currentTemp = snapshot.temperatureCelsius,
                isEnabled = prefs.isThermalAlertEnabled,
                thresholdCelsius = prefs.thermalThresholdCelsius,
                onToggle = onToggleThermalAlert,
                onThresholdChange = onChangeThermalThreshold,
                onTestThermalAlert = { onTriggerTestAlert(true) }
            )
        }

        // Smart 80% Charge Limit Protection Card
        item {
            ChargeLimit80ConfigCard(
                currentLevel = snapshot.levelPercent,
                isEnabled = prefs.isChargeLimit80Enabled,
                targetPercentage = prefs.chargeLimitPercentage,
                onToggle = onToggleChargeLimit80,
                onTargetChange = onChangeChargeLimitPct,
                onTest80Alert = { onTriggerTestAlert(false) }
            )
        }

        // Persistent Room Database Alert Logs Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CyanSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سجل تنبيهات الحماية (${alertLogs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                if (alertLogs.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAlertLogs,
                        modifier = Modifier.testTag("clear_alerts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "مسح سجل التنبيهات",
                            tint = CrimsonAlert
                        )
                    }
                }
            }
        }

        if (alertLogs.isEmpty()) {
            item {
                EmptyAlertHistoryCard()
            }
        } else {
            items(alertLogs, key = { it.id }) { alertItem ->
                AlertLogEntryCard(alert = alertItem)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackgroundServiceGuardCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val accent = if (isEnabled) EmeraldPrimary else TextMuted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("background_guard_service_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            width = if (isEnabled) 1.5.dp else 1.dp,
            color = if (isEnabled) EmeraldPrimary.copy(alpha = 0.7f) else ObsidianCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.VerifiedUser else Icons.Default.Security,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "الحارس الخلفي النشط (Foreground Service)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnabled) {
                            "نشط الآن: يراقب حرارة البطارية ونسبة 80% في الخلفية باستمرار"
                        } else {
                            "متوقف: فعّل الخدمة لتلقي إشعارات فورية أثناء الألعاب أو الشحن السريع"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled) EmeraldPrimary else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianSurface,
                    checkedTrackColor = EmeraldPrimary
                ),
                modifier = Modifier.testTag("switch_background_service")
            )
        }
    }
}

@Composable
private fun ThermalProtectionConfigCard(
    currentTemp: Float,
    isEnabled: Boolean,
    thresholdCelsius: Float,
    onToggle: (Boolean) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onTestThermalAlert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("thermal_protection_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonAlert.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = CrimsonAlert
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "درع الحماية الحرارية أثناء اللعب والشحن",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "حرارة البطارية الحالية: ${String.format(Locale.US, "%.1f°C", currentTemp)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianSurface,
                        checkedTrackColor = CrimsonAlert
                    ),
                    modifier = Modifier.testTag("switch_thermal_alert")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "عتبة إطلاق الإنذار الحراري",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = String.format(Locale.US, "%.1f °C", thresholdCelsius),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = CrimsonAlert
                )
            }

            Slider(
                value = thresholdCelsius,
                onValueChange = onThresholdChange,
                valueRange = 37f..46f,
                steps = 8,
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = CrimsonAlert,
                    activeTrackColor = CrimsonAlert,
                    inactiveTrackColor = ObsidianSurfaceElevated
                ),
                modifier = Modifier.testTag("slider_thermal_threshold")
            )

            OutlinedButton(
                onClick = onTestThermalAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_thermal_alert_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = CrimsonAlert,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "محاكاة واختبار التنبيه الحراري الآن",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ChargeLimit80ConfigCard(
    currentLevel: Int,
    isEnabled: Boolean,
    targetPercentage: Int,
    onToggle: (Boolean) -> Unit,
    onTargetChange: (Int) -> Unit,
    onTest80Alert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("charge_limit_80_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanSecondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = CyanSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "تنبيه إيقاف الشحن الذكي (80% Charge Guard)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "نسبة الشحن الحالية: $currentLevel% • يمنع إجهاد الجهد العالي",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianSurface,
                        checkedTrackColor = CyanSecondary
                    ),
                    modifier = Modifier.testTag("switch_charge_limit_80")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "نسبة الشحن المستهدفة للتنبيه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "$targetPercentage%",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = CyanSecondary
                )
            }

            Slider(
                value = targetPercentage.toFloat(),
                onValueChange = { onTargetChange(it.roundToInt()) },
                valueRange = 70f..95f,
                steps = 4,
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = CyanSecondary,
                    activeTrackColor = CyanSecondary,
                    inactiveTrackColor = ObsidianSurfaceElevated
                ),
                modifier = Modifier.testTag("slider_charge_limit_pct")
            )

            Button(
                onClick = onTest80Alert,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_80_limit_alert_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanSecondary.copy(alpha = 0.18f),
                    contentColor = CyanSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = CyanSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اختبار إشعار الوصول إلى $targetPercentage% الآن",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun EmptyAlertHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لا توجد حوادث حرارية أو تجاوزات مسجلة",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "تعمل البطارية ضمن النطاق الحراري والكهربائي الآمن. يمكنك الضغط على أزرار الاختبار أعلاه لتجربة نظام التنبيهات الفوري.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AlertLogEntryCard(alert: BatteryAlertLogEntity) {
    val isThermal = alert.alertType == "THERMAL"
    val badgeColor = if (isThermal) CrimsonAlert else CyanSecondary
    val timeFormatter = SimpleDateFormat("HH:mm:ss • yyyy/MM/dd", Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f°C", alert.temperatureCelsius)} • ${alert.batteryLevel}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = timeFormatter.format(Date(alert.timestampMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }
    }
}
