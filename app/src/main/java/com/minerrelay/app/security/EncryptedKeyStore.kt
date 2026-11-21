package com.minerrelay.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Stockage chiffré AES/GCM via AndroidKeyStore pour les clés API sensibles.
 * - Ne journalise jamais aucune clé en clair.
 * - Sépare la clé du service de calcul de la clé optionnelle Gemini.
 */
class EncryptedKeyStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveComputeApiKey(apiKey: String) {
        saveEncrypted(KEY_COMPUTE_API, apiKey.trim())
    }

    fun getComputeApiKey(): String = readDecrypted(KEY_COMPUTE_API)

    fun saveGeminiApiKey(apiKey: String) {
        saveEncrypted(KEY_GEMINI_API, apiKey.trim())
    }

    fun getGeminiApiKey(): String = readDecrypted(KEY_GEMINI_API)

    fun clearAllKeys() {
        prefs.edit()
            .remove(KEY_COMPUTE_API)
            .remove(KEY_GEMINI_API)
            .apply()
    }

    private fun saveEncrypted(prefKey: String, plaintext: String) {
        if (plaintext.isEmpty()) {
            prefs.edit().remove(prefKey).apply()
            return
        }
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(1 + iv.size + cipherBytes.size)
        payload[0] = iv.size.toByte()
        System.arraycopy(iv, 0, payload, 1, iv.size)
        System.arraycopy(cipherBytes, 0, payload, 1 + iv.size, cipherBytes.size)
        val encoded = Base64.encodeToString(payload, Base64.NO_WRAP)
        prefs.edit().putString(prefKey, encoded).apply()
    }

    private fun readDecrypted(prefKey: String): String {
        val encoded = prefs.getString(prefKey, null) ?: return ""
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            if (payload.size < 2) return ""
            val ivLen = payload[0].toInt() and 0xFF
            if (payload.size <= 1 + ivLen) return ""
            val iv = payload.copyOfRange(1, 1 + ivLen)
            val cipherBytes = payload.copyOfRange(1 + ivLen, payload.size)
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!isRobolectricEnvironment()) {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                if (existing != null) return existing

                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                return keyGenerator.generateKey()
            } catch (_: Exception) {
                // Fallback uniquement si AndroidKeyStore indisponible (tests JVM)
            }
        }
        return getOrCreateJvmTestKey()
    }

    private fun getOrCreateJvmTestKey(): SecretKey {
        val existingHex = prefs.getString(KEY_TEST_FALLBACK, null)
        if (!existingHex.isNullOrBlank()) {
            val bytes = Base64.decode(existingHex, Base64.NO_WRAP)
            return SecretKeySpec(bytes, "AES")
        }
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        prefs.edit().putString(KEY_TEST_FALLBACK, Base64.encodeToString(keyBytes, Base64.NO_WRAP)).apply()
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun isRobolectricEnvironment(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "minerrelay_aes_gcm_master_key"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PREFS_NAME = "minerrelay_encrypted_secrets"
        private const val KEY_COMPUTE_API = "enc_compute_api_key"
        private const val KEY_GEMINI_API = "enc_gemini_api_key"
        private const val KEY_TEST_FALLBACK = "jvm_test_only_aes_key"

        fun maskSecret(secret: String): String {
            val trimmed = secret.trim()
            if (trimmed.isEmpty()) return "(non configurée)"
            if (trimmed.length <= 8) return "••••"
            return "${trimmed.take(4)}••••${trimmed.takeLast(4)}"
        }

        fun maskAddress(address: String): String {
            val trimmed = address.trim()
            if (trimmed.isEmpty()) return "(aucune)"
            if (trimmed.length <= 10) return "${trimmed.take(3)}•••"
            return "${trimmed.take(6)}••••${trimmed.takeLast(4)}"
        }

        fun redactSensitive(
            message: String,
            secrets: List<String>,
            addresses: List<String>
        ): String {
            var sanitized = message
            secrets.map { it.trim() }.filter { it.length >= 4 }.forEach { secret ->
                sanitized = sanitized.replace(secret, maskSecret(secret))
            }
            addresses.map { it.trim() }.filter { it.length >= 10 }.forEach { addr ->
                sanitized = sanitized.replace(addr, maskAddress(addr))
            }
            return sanitized
        }
    }
}
