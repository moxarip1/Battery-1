package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BatteryTipsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.ProtectionScreen
import com.example.ui.theme.BatteryCareProTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BatteryAppTab
import com.example.viewmodel.BatteryCareViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryCareProTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BatteryCareProApp()
                }
            }
        }
    }
}

private data class NavDestinationItem(
    val tab: BatteryAppTab,
    val labelAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_ITEMS = listOf(
    NavDestinationItem(
        tab = BatteryAppTab.DASHBOARD,
        labelAr = "الرئيسية",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_tab_dashboard"
    ),
    NavDestinationItem(
        tab = BatteryAppTab.DIAGNOSTICS,
        labelAr = "تشخيص الكيرنل",
        selectedIcon = Icons.Filled.Memory,
        unselectedIcon = Icons.Outlined.Memory,
        testTag = "nav_tab_diagnostics"
    ),
    NavDestinationItem(
        tab = BatteryAppTab.PROTECTION,
        labelAr = "الحماية الذكية",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        testTag = "nav_tab_protection"
    ),
    NavDestinationItem(
        tab = BatteryAppTab.TIPS,
        labelAr = "نصائح علمية",
        selectedIcon = Icons.Filled.Lightbulb,
        unselectedIcon = Icons.Outlined.Lightbulb,
        testTag = "nav_tab_tips"
    )
)

@Composable
fun BatteryCareProApp(
    viewModel: BatteryCareViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val snapshot by viewModel.telemetry.collectAsStateWithLifecycle()
    val currentHistoryMa by viewModel.currentHistoryMa.collectAsStateWithLifecycle()
    val protectionPrefs by viewModel.protectionPrefs.collectAsStateWithLifecycle()
    val alertLogs by viewModel.alertLogs.collectAsStateWithLifecycle()

    // Ensure Back button navigates back to Dashboard from secondary tabs
    BackHandler(enabled = selectedTab != BatteryAppTab.DASHBOARD) {
        viewModel.selectTab(BatteryAppTab.DASHBOARD)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val isSelected = selectedTab == item.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.labelAr
                                    )
                                },
                                label = { Text(text = item.labelAr) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val isSelected = selectedTab == item.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.labelAr
                                    )
                                },
                                label = { Text(text = item.labelAr) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    BatteryAppTab.DASHBOARD -> {
                        DashboardScreen(
                            snapshot = snapshot,
                            currentHistoryMa = currentHistoryMa,
                            onRefresh = { viewModel.refreshTelemetryNow() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BatteryAppTab.DIAGNOSTICS -> {
                        DiagnosticsScreen(
                            snapshot = snapshot,
                            customDesignCapacityMah = protectionPrefs.customDesignCapacityMah,
                            onUpdateCustomDesignCapacity = { viewModel.updateCustomDesignCapacity(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BatteryAppTab.PROTECTION -> {
                        ProtectionScreen(
                            snapshot = snapshot,
                            prefs = protectionPrefs,
                            alertLogs = alertLogs,
                            onToggleBackgroundService = { viewModel.toggleBackgroundProtectionService(it) },
                            onToggleThermalAlert = { viewModel.setThermalAlertEnabled(it) },
                            onChangeThermalThreshold = { viewModel.setThermalThresholdCelsius(it) },
                            onToggleChargeLimit80 = { viewModel.setChargeLimit80Enabled(it) },
                            onChangeChargeLimitPct = { viewModel.setChargeLimitPercentage(it) },
                            onTriggerTestAlert = { isThermal -> viewModel.triggerSampleAlert(isThermal) },
                            onClearAlertLogs = { viewModel.clearAlertHistory() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BatteryAppTab.TIPS -> {
                        BatteryTipsScreen(
                            snapshot = snapshot,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
