package com.minerrelay.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.security.EncryptedKeyStore

/**
 * Stocke la configuration utilisateur non secrète (aucun défaut ressemblant à un vrai pool)
 * et délègue les clés API et le mot de passe pool optionnel à EncryptedKeyStore.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val encryptedKeyStore = EncryptedKeyStore(context.applicationContext)

    var poolHost: String
        get() = prefs.getString(KEY_POOL_HOST, "") ?: ""
        set(value) = prefs.edit().putString(KEY_POOL_HOST, value.trim()).apply()

    var poolPortText: String
        get() = prefs.getString(KEY_POOL_PORT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_POOL_PORT, value.trim()).apply()

    var poolLogin: String
        get() = prefs.getString(KEY_POOL_LOGIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_POOL_LOGIN, value.trim()).apply()

    var backupPoolEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_POOL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP_POOL_ENABLED, value).apply()

    var backupPoolPortText: String
        get() = prefs.getString(KEY_BACKUP_POOL_PORT, DEFAULT_BACKUP_PORT) ?: DEFAULT_BACKUP_PORT
        set(value) = prefs.edit().putString(KEY_BACKUP_POOL_PORT, value.trim()).apply()

    var backupPoolUseTls: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_POOL_USE_TLS, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP_POOL_USE_TLS, value).apply()

    var btcAddress: String
        get() = prefs.getString(KEY_BTC_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BTC_ADDRESS, value.trim()).apply()

    var computeApiUrl: String
        get() = prefs.getString(KEY_COMPUTE_API_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COMPUTE_API_URL, value.trim()).apply()

    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    var chargingOnly: Boolean
        get() = prefs.getBoolean(KEY_CHARGING_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_CHARGING_ONLY, value).apply()

    var testMode: Boolean
        get() = prefs.getBoolean(KEY_TEST_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_TEST_MODE, value).apply()

    fun savePoolPassword(password: String) = encryptedKeyStore.savePoolPassword(password)
    fun getPoolPassword(): String = encryptedKeyStore.getPoolPassword()

    fun saveComputeApiKey(key: String) = encryptedKeyStore.saveComputeApiKey(key)
    fun getComputeApiKey(): String = encryptedKeyStore.getComputeApiKey()

    fun saveGeminiApiKey(key: String) = encryptedKeyStore.saveGeminiApiKey(key)
    fun getGeminiApiKey(): String = encryptedKeyStore.getGeminiApiKey()

    fun loadRelayConfig(): RelayConfig = RelayConfig(
        poolHost = poolHost,
        poolPort = poolPortText.toIntOrNull(),
        poolLogin = poolLogin,
        backupPoolEnabled = backupPoolEnabled,
        backupPoolPort = backupPoolPortText.toIntOrNull() ?: 443,
        backupPoolUseTls = backupPoolUseTls,
        btcAddress = btcAddress,
        computeApiUrl = computeApiUrl,
        wifiOnly = wifiOnly,
        chargingOnly = chargingOnly,
        testMode = testMode
    )

    fun saveRelaySettings(
        poolHost: String,
        poolPortText: String,
        poolLogin: String,
        poolPassword: String,
        backupPoolEnabled: Boolean,
        backupPoolPortText: String,
        backupPoolUseTls: Boolean,
        btcAddress: String,
        computeApiUrl: String,
        computeApiKey: String,
        geminiApiKey: String,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        testMode: Boolean
    ) {
        this.poolHost = poolHost
        this.poolPortText = poolPortText
        this.poolLogin = poolLogin
        this.backupPoolEnabled = backupPoolEnabled
        this.backupPoolPortText = backupPoolPortText.trim().ifEmpty { DEFAULT_BACKUP_PORT }
        this.backupPoolUseTls = backupPoolUseTls
        this.btcAddress = btcAddress
        this.computeApiUrl = computeApiUrl
        this.wifiOnly = wifiOnly
        this.chargingOnly = chargingOnly
        this.testMode = testMode
        savePoolPassword(poolPassword)
        saveComputeApiKey(computeApiKey)
        saveGeminiApiKey(geminiApiKey)
    }

    companion object {
        private const val PREFS_NAME = "minerrelay_preferences"
        private const val DEFAULT_BACKUP_PORT = "443"
        private const val KEY_POOL_HOST = "pool_host"
        private const val KEY_POOL_PORT = "pool_port"
        private const val KEY_POOL_LOGIN = "pool_login"
        private const val KEY_BACKUP_POOL_ENABLED = "backup_pool_enabled"
        private const val KEY_BACKUP_POOL_PORT = "backup_pool_port"
        private const val KEY_BACKUP_POOL_USE_TLS = "backup_pool_use_tls"
        private const val KEY_BTC_ADDRESS = "btc_address"
        private const val KEY_COMPUTE_API_URL = "compute_api_url"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_CHARGING_ONLY = "charging_only"
        private const val KEY_TEST_MODE = "test_mode"
    }
}
