package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.diagnostics.BatteryTelemetrySnapshot
import com.example.diagnostics.KernelNodeEntry
import com.example.ui.theme.AmberWarning
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

@Composable
fun DiagnosticsScreen(
    snapshot: BatteryTelemetrySnapshot,
    customDesignCapacityMah: Int,
    onUpdateCustomDesignCapacity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalibrateDialog by remember { mutableStateOf(false) }

    if (showCalibrateDialog) {
        CalibrateDesignCapacityDialog(
            currentDesignMah = snapshot.designCapacityMah,
            onDismiss = { showCalibrateDialog = false },
            onSave = { newMah ->
                onUpdateCustomDesignCapacity(newMah)
                showCalibrateDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("diagnostics_screen_list")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "LINUX SYSFS • /sys/class/power_supply/battery/",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "محرك تشخيص الكيرنل المتقدم",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
        }

        // 1. Cycle Count Card
        item {
            CycleCountDiagnosticCard(snapshot = snapshot)
        }

        // 2. Technology & Full Charge Capacity vs Design Capacity Card
        item {
            CapacityAndTechnologyCard(
                snapshot = snapshot,
                isCustomCalibrated = customDesignCapacityMah > 0,
                onOpenCalibrate = { showCalibrateDialog = true }
            )
        }

        // 3. Real-time Charging Speed & Fast/Turbo Detection Card
        item {
            FastChargeAnalyzerCard(snapshot = snapshot)
        }

        // 4. Raw Kernel File Explorer (/sys/class/power_supply/battery/*)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = CyanSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "قارئ عقد الكيرنل المباشر (Sysfs Nodes)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        items(snapshot.kernelNodes, key = { it.fileName }) { node ->
            KernelNodeItemCard(node = node)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CycleCountDiagnosticCard(snapshot: BatteryTelemetrySnapshot) {
    // Typical standard lithium-ion rated cycle lifespan is 500 to 800 cycles before reaching 80% capacity
    val cycleProgress = (snapshot.cycleCount / 800f).coerceIn(0.02f, 1f)
    val cycleAccent = when {
        snapshot.cycleCount < 300 -> EmeraldPrimary
        snapshot.cycleCount < 600 -> CyanSecondary
        else -> AmberWarning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_count_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cycleAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "عدد دورات الشحن",
                            tint = cycleAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "عدد دورات الشحن الفعلية (Cycle Count)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = snapshot.cycleCountSource,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = cycleAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, cycleAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${snapshot.cycleCount} دورة",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = cycleAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { cycleProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = cycleAccent,
                trackColor = ObsidianSurfaceElevated,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 دورة (جديدة)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Text(
                    text = "المعيار القياسي: 500 - 800 دورة شحن كاملة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CapacityAndTechnologyCard(
    snapshot: BatteryTelemetrySnapshot,
    isCustomCalibrated: Boolean,
    onOpenCalibrate: () -> Unit
) {
    val retentionRatio = (snapshot.fullChargeCapacityMah.toFloat() / snapshot.designCapacityMah.toFloat())
        .coerceIn(0.3f, 1.0f)
    val lostMah = (snapshot.designCapacityMah - snapshot.fullChargeCapacityMah).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("capacity_comparison_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanSecondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "التكنولوجيا والسعة",
                            tint = CyanSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "التكنولوجيا وقدرة الاستيعاب",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "التركيب الكيميائي: ${snapshot.technology}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenCalibrate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.testTag("calibrate_capacity_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCustomCalibrated) "تعديل السعة" else "معايرة السعة",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Charge Capacity Bar vs Design Capacity Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "قدرة الاستيعاب الكاملة الحالية (Full Charge)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${snapshot.fullChargeCapacityMah} mAh",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = EmeraldPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { retentionRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = EmeraldPrimary,
                    trackColor = ObsidianCardBorder,
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "سعة التصنيع القياسية (Design Capacity)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${snapshot.designCapacityMah} mAh",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = CyanSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "مقدار التآكل الكيميائي المقدر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        text = "-$lostMah mAh (${String.format(Locale.US, "%.1f%%", 100f - snapshot.healthPercentage)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun FastChargeAnalyzerCard(snapshot: BatteryTelemetrySnapshot) {
    val tier = snapshot.chargingSpeedTier
    val badgeColor = when {
        tier.isFastCharging -> EmeraldPrimary
        snapshot.isCharging -> CyanSecondary
        else -> AmberWarning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fast_charge_analyzer_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            width = if (tier.isFastCharging) 1.5.dp else 1.dp,
            color = if (tier.isFastCharging) EmeraldPrimary.copy(alpha = 0.7f) else ObsidianCardBorder
        )
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "سرعة الشحن اللحظية",
                            tint = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "كاشف الشحن السريع (Fast / Turbo Detector)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = tier.labelAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = badgeColor
                        )
                    }
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tier.badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "التيار اللحظي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${abs(snapshot.currentNowMilliamps)} mA",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "متوسط التيار",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${abs(snapshot.currentAverageMilliamps)} mA",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = CyanSecondary
                    )
                }
                Column {
                    Text(
                        text = "قدرة الشحن",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f W", snapshot.wattageWatts),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = EmeraldPrimary
                    )
                }
                Column {
                    Text(
                        text = "حالة الشحن السريع",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (tier.isFastCharging) "مفعّل (Active)" else "غير مفعّل",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (tier.isFastCharging) EmeraldPrimary else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun KernelNodeItemCard(node: KernelNodeEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (node.isReadable) EmeraldPrimary else CyanSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.fullPath,
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.interpretedValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ObsidianCardBorder)
            ) {
                Text(
                    text = node.rawValue,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CalibrateDesignCapacityDialog(
    currentDesignMah: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf(currentDesignMah.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeveloperBoard,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "معايرة سعة التصنيع الأصلية",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "أدخل السعة الاسمية للبطارية كما هي محددة في مواصفات هاتفك (بالملي أمبير mAh) لحساب نسبة الصحة بأعلى دقة:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("السعة الأصلية (mAh)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("design_capacity_input")
                )
                TextButton(
                    onClick = { onSave(0) } // 0 resets to automatic kernel/PowerProfile detection
                ) {
                    Text(
                        text = "استعادة القراءة التلقائية من النظام",
                        color = CyanSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputText.toIntOrNull() ?: currentDesignMah
                    if (parsed in 1000..25000) {
                        onSave(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("حفظ المعايرة", color = ObsidianSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}
