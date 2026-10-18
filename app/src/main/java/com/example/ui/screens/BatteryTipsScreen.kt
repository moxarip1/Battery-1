package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diagnostics.BatteryTelemetrySnapshot
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

data class ScientificTipModel(
    val id: String,
    val title: String,
    val engineeringRule: String,
    val scientificExplanation: String,
    val lifeMultiplierBadge: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun BatteryTipsScreen(
    snapshot: BatteryTelemetrySnapshot,
    modifier: Modifier = Modifier
) {
    val scientificTips = listOf(
        ScientificTipModel(
            id = "tip_20_80_rule",
            title = "قاعدة 20% - 80% لتقليل إجهاد الجهد (Voltage Stress)",
            engineeringRule = "IEEE / Battery University • Depth of Discharge (DoD)",
            scientificExplanation = "عند شحن بطارية الليثيوم-آيون فوق 80%، يرتفع جهد الخلية إلى ~4.20V - 4.40V، مما يسرّع أكسدة الإلكتروليت وتكوين طبقة SEI على الأنود. التوقف عند 80% يضاعف عدد الدورات الفعالة من 500 إلى أكثر من 1200 دورة.",
            lifeMultiplierBadge = "+200% عمر أطول",
            icon = Icons.Default.BatteryChargingFull,
            accentColor = EmeraldPrimary
        ),
        ScientificTipModel(
            id = "tip_overnight_trickle",
            title = "تجنب ترك الهاتف على الشاحن طوال الليل (Trickle & Parasitic Load)",
            engineeringRule = "Electrochemical Plating & High-SoC Dwell Time",
            scientificExplanation = "بقاء البطارية عند نسبة 100% لساعات طويلة تحت جهد التعويم العالي (Float Voltage) مع وجود أحمال خلفية يسبب ترسب الليثيوم المعدني (Lithium Plating) وفقداناً دائماً في السعة الاستيعابية.",
            lifeMultiplierBadge = "منع تآكل 15% سنوياً",
            icon = Icons.Default.NightlightRound,
            accentColor = CyanSecondary
        ),
        ScientificTipModel(
            id = "tip_thermal_arrhenius",
            title = "قانون أرهينيوس الحراري: العدو الأول لخلايا الليثيوم",
            engineeringRule = "Arrhenius Equation • Thermal Degradation > 40°C",
            scientificExplanation = "تتضاعف سرعة التفاعلات الكيميائية غير المرغوبة داخل البطارية مع كل ارتفاع بمقدار 10°C فوق درجة حرارة الغرفة. تجنب تشغيل الألعاب الثقيلة أثناء الشحن السريع (Turbo Charging) لمنع تجاوز حاجز 40°C.",
            lifeMultiplierBadge = "حماية حرجة للخلايا",
            icon = Icons.Default.DeviceThermostat,
            accentColor = CrimsonAlert
        ),
        ScientificTipModel(
            id = "tip_oled_brightness",
            title = "الإدارة الذكية لسطوع شاشات OLED ومعدل التحديث",
            engineeringRule = "Display Power Draw & C-Rate Discharge Reduction",
            scientificExplanation = "تستهلك شاشات OLED في السطوع الأقصى تياراً يتجاوز 700mA إلى 1100mA، مما يرفع معدل التفريغ (C-Rate) وحرارة الجهاز. تفعيل الوضع الداكن والسطوع التكيفي يقلل تيار التفريغ بنسبة 35%.",
            lifeMultiplierBadge = "-35% استهلاك تيار",
            icon = Icons.Default.BrightnessMedium,
            accentColor = AmberWarning
        ),
        ScientificTipModel(
            id = "tip_fast_charge_c_rate",
            title = "الاستخدام المتوازن للشحن فائق السرعة (C-Rate Control)",
            engineeringRule = "Lithium-Ion Intercalation Kinetics",
            scientificExplanation = "الشحن السريع جداً (أعلى من 1.5C أو 40W+) يدفع أيونات الليثيوم بسرعة عالية نحو طبقات الجرافيت، مما يولد مقاومة داخلية وحرارة. استخدم الشحن القياسي عند عدم الاستعجال للحفاظ على سلامة الأقطاب.",
            lifeMultiplierBadge = "استقرار المقاومة الداخلية",
            icon = Icons.Default.ElectricBolt,
            accentColor = EmeraldPrimary
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tips_screen_list")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "ELECTRICAL ENGINEERING • LI-ION LONGEVITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "إرشادات الهندسة الكهربائية لإطالة عمر البطارية",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
        }

        // Custom Hero Engineering Schematic Banner (Canvas + Live Voltage Stress Analyzer)
        item {
            EngineeringHeroSchematicCard(snapshot = snapshot)
        }

        items(scientificTips, key = { it.id }) { tip ->
            ScientificTipCard(tip = tip)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EngineeringHeroSchematicCard(snapshot: BatteryTelemetrySnapshot) {
    val voltageVolts = snapshot.voltageMillivolts / 1000f
    // Approximate Li-ion cell voltage stress factor (3.70V nominal = low stress, >4.20V = high anode stress)
    val stressStatus = when {
        voltageVolts >= 4.22f -> "إجهاد جهد مرتفع (High Voltage Stress)" to AmberWarning
        voltageVolts >= 3.70f -> "نطاق الجهد الكيميائي المثالي (Optimal SoC)" to EmeraldPrimary
        else -> "منطقة تفريغ عميق (Deep Discharge Zone)" to CrimsonAlert
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("engineering_schematic_card"),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = CyanSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "محلل إجهاد خلية الليثيوم اللحظي (Real-time Cell Stress)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Schematic Illustration drawn on Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(14.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw subtle engineering grid lines
                    val cols = 8
                    val stepX = size.width / cols
                    for (i in 1 until cols) {
                        drawLine(
                            color = ObsidianCardBorder.copy(alpha = 0.5f),
                            start = Offset(i * stepX, 0f),
                            end = Offset(i * stepX, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw 20% - 80% Optimal Safe Zone Bar
                    val barHeight = 26.dp.toPx()
                    val barY = (size.height - barHeight) / 2f

                    // Full Track (0% to 100%)
                    drawRoundRect(
                        color = ObsidianCardBorder,
                        topLeft = Offset(0f, barY),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )

                    // Optimal 20%..80% Emerald Zone
                    val start20 = size.width * 0.20f
                    val width60 = size.width * 0.60f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                CyanSecondary.copy(alpha = 0.45f),
                                EmeraldPrimary.copy(alpha = 0.65f),
                                CyanSecondary.copy(alpha = 0.45f)
                            )
                        ),
                        topLeft = Offset(start20, barY),
                        size = Size(width60, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Current Battery Level Marker
                    val markerX = (snapshot.levelPercent / 100f).coerceIn(0.02f, 0.98f) * size.width
                    drawLine(
                        color = Color.White,
                        start = Offset(markerX, barY - 10.dp.toPx()),
                        end = Offset(markerX, barY + barHeight + 10.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = 6.dp.toPx(),
                        center = Offset(markerX, barY + barHeight / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stressStatus.first,
                        style = MaterialTheme.typography.titleMedium,
                        color = stressStatus.second,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "النطاق الأخضر (20% - 80%) يمثل أقل إجهاد كيميائي للأقطاب",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, stressStatus.second.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2fV", voltageVolts),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = stressStatus.second,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScientificTipCard(tip: ScientificTipModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scientific_tip_${tip.id}"),
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
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tip.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            tint = tip.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tip.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tip.engineeringRule,
                            style = MaterialTheme.typography.labelMedium,
                            color = tip.accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = tip.accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tip.lifeMultiplierBadge,
                        style = MaterialTheme.typography.labelLarge,
                        color = tip.accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tip.scientificExplanation,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}
