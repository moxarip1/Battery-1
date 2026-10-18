package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diagnostics.BatteryConditionGrade
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    snapshot: BatteryTelemetrySnapshot,
    currentHistoryMa: List<Int>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen_list")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            DashboardHeaderBar(
                snapshot = snapshot,
                onRefresh = onRefresh
            )
        }

        // Visual Thermal Alert Banner if temperature >= 40°C
        item {
            AnimatedVisibility(visible = snapshot.isThermalWarning) {
                ThermalDangerBanner(temperatureCelsius = snapshot.temperatureCelsius)
            }
        }

        // Circular Health Score & Condition Gauge Card
        item {
            HealthCircularGaugeCard(snapshot = snapshot)
        }

        // Primary 4-Grid Live Telemetry Cards (Status, Temperature, Voltage, Current)
        item {
            LiveTelemetryMetricsGrid(snapshot = snapshot)
        }

        // Capacity Comparison & Live Current Sparkline Card
        item {
            CapacityAndWaveformCard(
                snapshot = snapshot,
                currentHistoryMa = currentHistoryMa
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHeaderBar(
    snapshot: BatteryTelemetrySnapshot,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (snapshot.isThermalWarning) CrimsonAlert else EmeraldPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BATTERYCARE PRO • TELEMETRY",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "لوحة الفحص وصحة البطارية",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurfaceElevated)
                .testTag("refresh_telemetry_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "تحديث القراءات اللحظية",
                tint = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun ThermalDangerBanner(temperatureCelsius: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "thermalPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("thermal_warning_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CrimsonAlert.copy(alpha = pulseAlpha)
        ),
        border = BorderStroke(1.5.dp, CrimsonAlert)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "تنبيه حرارة مرتفعة",
                tint = CrimsonAlert,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تحذير حرارة حرجة: ${String.format(Locale.US, "%.1f°C", temperatureCelsius)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تجاوزت حرارة البطارية 40°C! افصل الشاحن السريع أو أوقف الألعاب لتفادي تلف خلايا الليثيوم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD6DA)
                )
            }
        }
    }
}

@Composable
private fun HealthCircularGaugeCard(snapshot: BatteryTelemetrySnapshot) {
    val gradeColor = when (snapshot.conditionGrade) {
        BatteryConditionGrade.GOOD -> EmeraldPrimary
        BatteryConditionGrade.FAIR -> AmberWarning
        BatteryConditionGrade.POOR -> CrimsonAlert
    }

    val animatedHealth by animateFloatAsState(
        targetValue = snapshot.healthPercentage,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "healthArcAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_score_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = gradeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(gradeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الحالة العامة: ${snapshot.conditionGrade.labelAr} (${snapshot.conditionGrade.labelEn})",
                            style = MaterialTheme.typography.labelLarge,
                            color = gradeColor
                        )
                    }
                }

                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "الشحن: ${snapshot.levelPercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Custom Canvas Radial Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 18.dp.toPx()
                    val diameter = size.minDimension - strokeWidth * 1.6f
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    // Outer decorative tick marks
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.minDimension / 2f - 4.dp.toPx()
                    val innerTickRadius = outerRadius - 6.dp.toPx()
                    for (i in 0..36) {
                        val angleDeg = 135f + (i * (270f / 36f))
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val start = Offset(
                            x = center.x + (innerTickRadius * cos(angleRad)).toFloat(),
                            y = center.y + (innerTickRadius * sin(angleRad)).toFloat()
                        )
                        val end = Offset(
                            x = center.x + (outerRadius * cos(angleRad)).toFloat(),
                            y = center.y + (outerRadius * sin(angleRad)).toFloat()
                        )
                        val isActiveTick = (i / 36f) * 100f <= animatedHealth
                        drawLine(
                            color = if (isActiveTick) gradeColor.copy(alpha = 0.65f) else ObsidianCardBorder,
                            start = start,
                            end = end,
                            strokeWidth = if (i % 6 == 0) 2.5.dp.toPx() else 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Background Track Arc
                    drawArc(
                        color = ObsidianSurfaceElevated,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Foreground Health Gradient Arc
                    val sweep = (animatedHealth / 100f).coerceIn(0f, 1f) * 270f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                gradeColor.copy(alpha = 0.75f),
                                gradeColor,
                                CyanSecondary,
                                gradeColor
                            )
                        ),
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "نسبة صحة البطارية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", animatedHealth),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        modifier = Modifier.testTag("health_percentage_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = gradeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = snapshot.conditionGrade.labelEn.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelMedium,
                            color = gradeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = snapshot.conditionGrade.descriptionAr,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Capacity comparison pills (Full Charge Capacity vs Design Capacity)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapacitySummaryItem(
                    label = "السعة الحالية الفعلية",
                    value = "${snapshot.fullChargeCapacityMah} mAh",
                    valueColor = EmeraldPrimary
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(ObsidianCardBorder)
                )
                CapacitySummaryItem(
                    label = "سعة التصنيع الأصلية",
                    value = "${snapshot.designCapacityMah} mAh",
                    valueColor = CyanSecondary
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(ObsidianCardBorder)
                )
                CapacitySummaryItem(
                    label = "دورات الشحن",
                    value = "${snapshot.cycleCount} دورة",
                    valueColor = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun CapacitySummaryItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiveTelemetryMetricsGrid(snapshot: BatteryTelemetrySnapshot) {
    val tempColor = when {
        snapshot.temperatureCelsius >= 40.0f -> CrimsonAlert
        snapshot.temperatureCelsius >= 36.5f -> AmberWarning
        else -> EmeraldPrimary
    }

    val currentColor = if (snapshot.currentNowMilliamps >= 0) EmeraldPrimary else CyanSecondary
    val currentPrefix = if (snapshot.currentNowMilliamps > 0) "+" else ""

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStatCard(
                title = "الحالة الحالية",
                primaryValue = snapshot.statusTextAr,
                secondarySubtitle = snapshot.plugTypeAr,
                icon = if (snapshot.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                accentColor = if (snapshot.isCharging) EmeraldPrimary else CyanSecondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_battery_status")
            )

            TelemetryStatCard(
                title = "حرارة البطارية",
                primaryValue = String.format(Locale.US, "%.1f °C", snapshot.temperatureCelsius),
                secondarySubtitle = if (snapshot.isThermalWarning) "⚠️ حرارة مرتفعة (>40°C)" else "ضمن النطاق الحراري الآمن",
                icon = Icons.Default.DeviceThermostat,
                accentColor = tempColor,
                isAlertHighlighted = snapshot.isThermalWarning,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_battery_temperature")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStatCard(
                title = "الجهد الكهربائي (Voltage)",
                primaryValue = "${snapshot.voltageMillivolts} mV",
                secondarySubtitle = String.format(Locale.US, "%.3f Volts • %s", snapshot.voltageMillivolts / 1000f, snapshot.technology),
                icon = Icons.Default.ElectricBolt,
                accentColor = CyanSecondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_battery_voltage")
            )

            TelemetryStatCard(
                title = "شدة التيار (Current)",
                primaryValue = "$currentPrefix${snapshot.currentNowMilliamps} mA",
                secondarySubtitle = String.format(Locale.US, "القدرة: %.2f W • %s", snapshot.wattageWatts, snapshot.chargingSpeedTier.badgeText),
                icon = Icons.Default.Bolt,
                accentColor = currentColor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_battery_current")
            )
        }
    }
}

@Composable
private fun TelemetryStatCard(
    title: String,
    primaryValue: String,
    secondarySubtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isAlertHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlertHighlighted) CrimsonAlert.copy(alpha = 0.14f) else ObsidianSurface
        ),
        border = BorderStroke(
            width = if (isAlertHighlighted) 1.5.dp else 1.dp,
            color = if (isAlertHighlighted) CrimsonAlert else ObsidianCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = secondarySubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAlertHighlighted) CrimsonAlert else TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CapacityAndWaveformCard(
    snapshot: BatteryTelemetrySnapshot,
    currentHistoryMa: List<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "المخطط اللحظي لتدفق التيار (mA)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Text(
                    text = snapshot.chargingSpeedTier.labelAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (snapshot.chargingSpeedTier.isFastCharging) EmeraldPrimary else CyanSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sparkline Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(12.dp)
            ) {
                val samples = if (currentHistoryMa.size >= 2) {
                    currentHistoryMa
                } else {
                    listOf(snapshot.currentNowMilliamps, snapshot.currentNowMilliamps)
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val magnitudes = samples.map { abs(it).toFloat() }
                    val maxVal = (magnitudes.maxOrNull() ?: 2000f).coerceAtLeast(500f)
                    val minVal = (magnitudes.minOrNull() ?: 0f).coerceAtMost(maxVal - 200f)
                    val range = (maxVal - minVal).coerceAtLeast(1f)

                    val path = Path()
                    val stepX = size.width / (magnitudes.size - 1).coerceAtLeast(1)

                    magnitudes.forEachIndexed { index, value ->
                        val x = index * stepX
                        val normalizedY = 1f - ((value - minVal) / range).coerceIn(0.05f, 0.95f)
                        val y = normalizedY * size.height
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = if (snapshot.isCharging) EmeraldPrimary else CyanSecondary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = snapshot.capacitySourceDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Text(
                    text = "الشحنة: ${snapshot.currentChargeCounterMah} mAh",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSecondary
                )
            }
        }
    }
}
