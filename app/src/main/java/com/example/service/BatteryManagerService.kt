package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BatteryAlertLogEntity
import com.example.data.BatteryCareDatabase
import com.example.data.BatteryPreferencesRepository
import com.example.diagnostics.BatteryTelemetrySnapshot
import com.example.diagnostics.KernelBatteryDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class BatteryManagerService : Service() {

    companion object {
        const val CHANNEL_ID_MONITOR = "battery_care_monitor_channel"
        const val CHANNEL_ID_ALERTS = "battery_care_critical_alerts_channel"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_THERMAL_ALERT = 2001
        const val NOTIFICATION_ID_CHARGE_LIMIT_ALERT = 2002

        const val ACTION_START_MONITORING = "com.example.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.action.STOP_MONITORING"
        const val ACTION_TEST_THERMAL_ALERT = "com.example.action.TEST_THERMAL_ALERT"
        const val ACTION_TEST_80_LIMIT_ALERT = "com.example.action.TEST_80_LIMIT_ALERT"

        fun startService(context: Context) {
            val intent = Intent(context, BatteryManagerService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BatteryManagerService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        fun triggerTestAlert(context: Context, isThermal: Boolean) {
            val intent = Intent(context, BatteryManagerService::class.java).apply {
                action = if (isThermal) ACTION_TEST_THERMAL_ALERT else ACTION_TEST_80_LIMIT_ALERT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var diagnostics: KernelBatteryDiagnostics
    private lateinit var preferencesRepository: BatteryPreferencesRepository
    private lateinit var database: BatteryCareDatabase
    private lateinit var notificationManager: NotificationManager

    private var lastThermalAlertTimestamp = 0L
    private var lastChargeLimitAlertSessionCharged = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                serviceScope.launch {
                    inspectAndEvaluateBatteryState()
                }
            }
        }
    }

    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        diagnostics = KernelBatteryDiagnostics(applicationContext)
        preferencesRepository = BatteryPreferencesRepository(applicationContext)
        database = BatteryCareDatabase.getInstance(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                unregisterBatteryReceiverSafely()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TEST_THERMAL_ALERT -> {
                startForegroundSafely(diagnostics.captureSnapshot())
                serviceScope.launch {
                    val snapshot = diagnostics.captureSnapshot()
                    dispatchThermalAlert(
                        temperature = maxOf(snapshot.temperatureCelsius, 42.5f),
                        level = snapshot.levelPercent,
                        isSimulation = true
                    )
                }
            }
            ACTION_TEST_80_LIMIT_ALERT -> {
                startForegroundSafely(diagnostics.captureSnapshot())
                serviceScope.launch {
                    val snapshot = diagnostics.captureSnapshot()
                    dispatchChargeLimitAlert(
                        level = maxOf(snapshot.levelPercent, 80),
                        limitTarget = 80,
                        temp = snapshot.temperatureCelsius,
                        isSimulation = true
                    )
                }
            }
            else -> {
                val initialSnapshot = diagnostics.captureSnapshot()
                startForegroundSafely(initialSnapshot)
                registerBatteryReceiverSafely()
                startPeriodicKernelTelemetryLoop()
            }
        }
        return START_STICKY
    }

    private fun startForegroundSafely(snapshot: BatteryTelemetrySnapshot) {
        val notification = buildOngoingMonitorNotification(snapshot)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID_FOREGROUND,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID_FOREGROUND, notification)
            }
        } catch (_: Exception) {
            // In case foreground service launch restrictions apply on specific OEM states
        }
    }

    private fun startPeriodicKernelTelemetryLoop() {
        serviceScope.launch {
            while (isActive) {
                inspectAndEvaluateBatteryState()
                delay(12_000L)
            }
        }
    }

    private suspend fun inspectAndEvaluateBatteryState() {
        val prefs = preferencesRepository.preferencesFlow.first()
        val customMah = prefs.customDesignCapacityMah.takeIf { it > 0 }
        val snapshot = diagnostics.captureSnapshot(customMah)

        // Update ongoing telemetry notification
        try {
            notificationManager.notify(
                NOTIFICATION_ID_FOREGROUND,
                buildOngoingMonitorNotification(snapshot)
            )
        } catch (_: SecurityException) {
        }

        val now = System.currentTimeMillis()

        // 1. Check Smart Thermal Protection Alert (>= threshold, default 40.0°C)
        if (prefs.isThermalAlertEnabled && snapshot.temperatureCelsius >= prefs.thermalThresholdCelsius) {
            // Cooldown of 3 minutes between duplicate real thermal notifications
            if (now - lastThermalAlertTimestamp > 180_000L) {
                lastThermalAlertTimestamp = now
                dispatchThermalAlert(
                    temperature = snapshot.temperatureCelsius,
                    level = snapshot.levelPercent,
                    isSimulation = false
                )
            }
        }

        // 2. Check 80% Charge Limit Protection Alert
        if (!snapshot.isCharging) {
            lastChargeLimitAlertSessionCharged = false
        } else if (prefs.isChargeLimit80Enabled &&
            snapshot.levelPercent >= prefs.chargeLimitPercentage &&
            !lastChargeLimitAlertSessionCharged
        ) {
            lastChargeLimitAlertSessionCharged = true
            dispatchChargeLimitAlert(
                level = snapshot.levelPercent,
                limitTarget = prefs.chargeLimitPercentage,
                temp = snapshot.temperatureCelsius,
                isSimulation = false
            )
        }
    }

    private suspend fun dispatchThermalAlert(
        temperature: Float,
        level: Int,
        isSimulation: Boolean
    ) {
        val tempFormatted = String.format(Locale.US, "%.1f°C", temperature)
        val title = if (isSimulation) {
            "⚠️ تحذير حراري تجريبي ($tempFormatted)"
        } else {
            "⚠️ تحذير: حرارة البطارية مرتفعة ($tempFormatted)"
        }
        val message = "تجاوزت حرارة الخلية الحد الآمن أثناء الشحن/الاستخدام. يوصى بفصل الشاحن أو إيقاف الألعاب الثقيلة فوراً لحماية كيمياء الليثيوم."

        database.alertLogDao().insertAlert(
            BatteryAlertLogEntity(
                title = title,
                message = message,
                alertType = "THERMAL",
                temperatureCelsius = temperature,
                batteryLevel = level
            )
        )

        val pendingIntent = createMainActivityPendingIntent()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_THERMAL_ALERT, notification)
        } catch (_: SecurityException) {
        }
    }

    private suspend fun dispatchChargeLimitAlert(
        level: Int,
        limitTarget: Int,
        temp: Float,
        isSimulation: Boolean
    ) {
        val title = if (isSimulation) {
            "🔋 تنبيه حماية الشحن الذكي ($level%)"
        } else {
            "🔋 وصلت البطارية إلى الحد الصحي ($level%)"
        }
        val message = "اكتملت نسبة الشحن المستهدفة ($limitTarget%). افصل الشاحن الآن لتقليل إجهاد الجهد العالي ومضاعفة عمر دورات البطارية."

        database.alertLogDao().insertAlert(
            BatteryAlertLogEntity(
                title = title,
                message = message,
                alertType = "CHARGE_LIMIT_80",
                temperatureCelsius = temp,
                batteryLevel = level
            )
        )

        val pendingIntent = createMainActivityPendingIntent()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_CHARGE_LIMIT_ALERT, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun buildOngoingMonitorNotification(snapshot: BatteryTelemetrySnapshot): Notification {
        val tempStr = String.format(Locale.US, "%.1f°C", snapshot.temperatureCelsius)
        val currentSign = if (snapshot.currentNowMilliamps > 0) "+" else ""
        val contentTitle = "BatteryCare Pro • ${snapshot.levelPercent}% (${snapshot.statusTextAr})"
        val contentText = "الحرارة: $tempStr | التيار: $currentSign${snapshot.currentNowMilliamps} mA | الجهد: ${snapshot.voltageMillivolts} mV"

        return NotificationCompat.Builder(this, CHANNEL_ID_MONITOR)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createMainActivityPendingIntent())
            .build()
    }

    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_ID_MONITOR,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "التنبيهات الحرارية وحماية الشحن 80%",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية عند تجاوز الحرارة 40°C أو وصول الشحن إلى 80%"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    private fun registerBatteryReceiverSafely() {
        if (!isReceiverRegistered) {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            isReceiverRegistered = true
        }
    }

    private fun unregisterBatteryReceiverSafely() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
            }
            isReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        unregisterBatteryReceiverSafely()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
