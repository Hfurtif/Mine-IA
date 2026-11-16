package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("easymine_preferences", Context.MODE_PRIVATE)

    var electricityCostKwh: Float
        get() = prefs.getFloat(KEY_ELECTRICITY_COST, 0.22f)
        set(value) = prefs.edit().putFloat(KEY_ELECTRICITY_COST, value).apply()

    var hardwareWatts: Int
        get() = prefs.getInt(KEY_HARDWARE_WATTS, 110)
        set(value) = prefs.edit().putInt(KEY_HARDWARE_WATTS, value).apply()

    var maxSafeTemp: Float
        get() = prefs.getFloat(KEY_MAX_SAFE_TEMP, 75.0f)
        set(value) = prefs.edit().putFloat(KEY_MAX_SAFE_TEMP, value).apply()

    var cpuLoadPercent: Int
        get() = prefs.getInt(KEY_CPU_LOAD_PERCENT, 50)
        set(value) = prefs.edit().putInt(KEY_CPU_LOAD_PERCENT, value).apply()

    var aiApiKey: String
        get() = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_API_KEY, value).apply()

    var hasAcceptedDisclaimer: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, value).apply()

    var lastSelectedCryptoId: String
        get() = prefs.getString(KEY_LAST_CRYPTO, "monero") ?: "monero"
        set(value) = prefs.edit().putString(KEY_LAST_CRYPTO, value).apply()

    companion object {
        private const val KEY_ELECTRICITY_COST = "key_electricity_cost"
        private const val KEY_HARDWARE_WATTS = "key_hardware_watts"
        private const val KEY_MAX_SAFE_TEMP = "key_max_safe_temp"
        private const val KEY_CPU_LOAD_PERCENT = "key_cpu_load_percent"
        private const val KEY_AI_API_KEY = "key_ai_api_key"
        private const val KEY_DISCLAIMER_ACCEPTED = "key_disclaimer_accepted"
        private const val KEY_LAST_CRYPTO = "key_last_crypto"
    }
}
