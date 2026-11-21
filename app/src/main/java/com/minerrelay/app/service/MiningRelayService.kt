package com.minerrelay.app.service

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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.minerrelay.app.MainActivity
import com.minerrelay.app.R
import com.minerrelay.app.data.model.MiningStatus
import com.minerrelay.app.mining.MiningEngine
import com.minerrelay.app.mining.MiningTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Service en avant-plan (FOREGROUND_SERVICE_DATA_SYNC) :
 * - Affiche une notification permanente (état + parts acceptées + bouton Arrêter)
 * - Gère les contraintes Wi-Fi uniquement et En charge uniquement
 * - Gère la reconnexion lors des changements réseau et acquiert le WakeLock avec parcimonie
 */
class MiningRelayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telemetryJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallbackRegistered = false
    private var powerReceiverRegistered = false

    @Volatile
    private var lastWifiConnected = false

    @Volatile
    private var lastCharging = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateEnvironment(networkSwitched = true)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateEnvironment(networkSwitched = false)
        }

        override fun onLost(network: Network) {
            updateEnvironment(networkSwitched = false)
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateEnvironment(networkSwitched = false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        registerEnvironmentMonitors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RELAY -> {
                RelayCoordinator.getEngine(this).stopRelay()
                releaseWakeLock()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_RELAY, null -> {
                val engine = RelayCoordinator.getEngine(this)
                val prefs = RelayCoordinator.getPreferences(this)
                val config = prefs.loadRelayConfig()
                val computeKey = prefs.getComputeApiKey()

                startForegroundWithNotification(buildNotification(engine.telemetry.value))
                observeTelemetry(engine)
                refreshInitialEnvironmentState()
                engine.onEnvironmentChanged(lastWifiConnected, lastCharging)
                engine.startRelay(config, computeKey)
            }
        }
        return START_STICKY
    }

    private fun observeTelemetry(engine: MiningEngine) {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            engine.telemetry.collectLatest { telemetry ->
                val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mgr.notify(NOTIFICATION_ID, buildNotification(telemetry))

                // WakeLock utilisé avec parcimonie : uniquement lorsque le relais est actif ou en connexion
                if (telemetry.status == MiningStatus.ACTIVE || telemetry.status == MiningStatus.CONNECTING) {
                    acquirePartialWakeLock()
                } else {
                    releaseWakeLock()
                }
            }
        }
    }

    private fun startForegroundWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildNotification(telemetry: MiningTelemetry): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MiningRelayService::class.java).apply { action = ACTION_STOP_RELAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hashrateText = MiningEngine.formatHashrate(telemetry.hashrate15mHps)
        val contentText = "État : ${telemetry.status.labelFr} • Parts acceptées : ${telemetry.acceptedShares} • $hashrateText"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n${telemetry.poolDetail}"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.relay_action_stop),
                stopIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.relay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.relay_notification_channel_desc)
            }
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }

    private fun registerEnvironmentMonitors() {
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        } catch (_: Exception) {
            networkCallbackRegistered = false
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(powerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(powerReceiver, filter)
        }
        powerReceiverRegistered = true
        refreshInitialEnvironmentState()
    }

    private fun refreshInitialEnvironmentState() {
        lastWifiConnected = isWifiCurrentlyConnected()
        lastCharging = isDeviceCurrentlyCharging()
    }

    private fun updateEnvironment(networkSwitched: Boolean) {
        val wifi = isWifiCurrentlyConnected()
        val charging = isDeviceCurrentlyCharging()
        val changed = (wifi != lastWifiConnected) || (charging != lastCharging) || networkSwitched
        lastWifiConnected = wifi
        lastCharging = charging
        if (changed) {
            RelayCoordinator.getEngine(this).onEnvironmentChanged(wifi, charging, networkSwitched)
        }
    }

    private fun isWifiCurrentlyConnected(): Boolean {
        val cm = connectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isDeviceCurrentlyCharging(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun acquirePartialWakeLock() {
        val existing = wakeLock
        if (existing != null && existing.isHeld) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MinerRelay:StratumWakeLock").apply {
            setReferenceCounted(false)
            // Timeout de sécurité de 15 minutes pour éviter toute consommation excessive
            acquire(15L * 60L * 1000L)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        if (networkCallbackRegistered) {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
            }
        }
        if (powerReceiverRegistered) {
            try {
                unregisterReceiver(powerReceiver)
            } catch (_: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "miner_relay_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_RELAY = "com.minerrelay.app.ACTION_START_RELAY"
        const val ACTION_STOP_RELAY = "com.minerrelay.app.ACTION_STOP_RELAY"

        fun start(context: Context) {
            val intent = Intent(context, MiningRelayService::class.java).apply {
                action = ACTION_START_RELAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MiningRelayService::class.java).apply {
                action = ACTION_STOP_RELAY
            }
            context.startService(intent)
        }
    }
}
