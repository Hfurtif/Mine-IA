package com.minerrelay.app.data.model

enum class MiningStatus(val labelFr: String) {
    IDLE("En attente"),
    CONNECTING("Connexion au pool"),
    ACTIVE("Relais actif"),
    WAITING_WIFI("En attente du Wi-Fi"),
    WAITING_CHARGING("En attente de charge"),
    PAUSED("En pause"),
    ERROR("Erreur")
}

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR
}

data class MiningLogMessage(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

data class RelayConfig(
    val poolHost: String = "",
    val poolPort: Int? = null,
    val poolLogin: String = "",
    val backupPoolEnabled: Boolean = false,
    val backupPoolPort: Int? = 443,
    val backupPoolUseTls: Boolean = false,
    val btcAddress: String = "",
    val computeApiUrl: String = "",
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val testMode: Boolean = false
) {
    val isPoolConfigured: Boolean
        get() = poolHost.isNotBlank() &&
            (poolPort ?: 0) in 1..65535 &&
            poolLogin.isNotBlank()

    val isBackupPoolConfigured: Boolean
        get() = backupPoolEnabled &&
            poolHost.isNotBlank() &&
            (backupPoolPort ?: 0) in 1..65535 &&
            backupPoolPort != poolPort
}
