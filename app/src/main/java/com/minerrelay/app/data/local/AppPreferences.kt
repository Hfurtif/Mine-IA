package com.minerrelay.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.security.EncryptedKeyStore

/**
 * Stocke la configuration utilisateur non secrète (aucun défaut ressemblant à un vrai pool)
 * et délègue les clés API chiffrées à AndroidKeyStore via EncryptedKeyStore.
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

    var workerName: String
        get() = prefs.getString(KEY_WORKER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WORKER_NAME, value.trim()).apply()

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

    fun saveComputeApiKey(key: String) = encryptedKeyStore.saveComputeApiKey(key)
    fun getComputeApiKey(): String = encryptedKeyStore.getComputeApiKey()

    fun saveGeminiApiKey(key: String) = encryptedKeyStore.saveGeminiApiKey(key)
    fun getGeminiApiKey(): String = encryptedKeyStore.getGeminiApiKey()

    fun loadRelayConfig(): RelayConfig = RelayConfig(
        poolHost = poolHost,
        poolPort = poolPortText.toIntOrNull(),
        workerName = workerName,
        btcAddress = btcAddress,
        computeApiUrl = computeApiUrl,
        wifiOnly = wifiOnly,
        chargingOnly = chargingOnly,
        testMode = testMode
    )

    fun saveRelaySettings(
        poolHost: String,
        poolPortText: String,
        workerName: String,
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
        this.workerName = workerName
        this.btcAddress = btcAddress
        this.computeApiUrl = computeApiUrl
        this.wifiOnly = wifiOnly
        this.chargingOnly = chargingOnly
        this.testMode = testMode
        saveComputeApiKey(computeApiKey)
        saveGeminiApiKey(geminiApiKey)
    }

    companion object {
        private const val PREFS_NAME = "minerrelay_preferences"
        private const val KEY_POOL_HOST = "pool_host"
        private const val KEY_POOL_PORT = "pool_port"
        private const val KEY_WORKER_NAME = "worker_name"
        private const val KEY_BTC_ADDRESS = "btc_address"
        private const val KEY_COMPUTE_API_URL = "compute_api_url"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_CHARGING_ONLY = "charging_only"
        private const val KEY_TEST_MODE = "test_mode"
    }
}
