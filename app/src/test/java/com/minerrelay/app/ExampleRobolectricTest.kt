package com.minerrelay.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.minerrelay.app.data.local.AppPreferences
import com.minerrelay.app.mining.AddressValidationResult
import com.minerrelay.app.mining.SecurityValidator
import com.minerrelay.app.security.EncryptedKeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name and empty default pool settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Miner IA", context.getString(R.string.app_name))

        val prefs = AppPreferences(context)
        val config = prefs.loadRelayConfig()
        assertEquals("", config.poolHost)
        assertEquals(null, config.poolPort)
        assertEquals("", config.btcAddress)
        assertFalse(config.isPoolConfigured)
    }

    @Test
    fun `bitcoin address validator verifies Base58Check and Bech32 Bech32m checksums strictly`() {
        // Adresses mainnet valides (P2PKH, P2SH, P2WPKH, Taproot P2TR)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa") is AddressValidationResult.Valid)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy") is AddressValidationResult.Valid)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq") is AddressValidationResult.Valid)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr") is AddressValidationResult.Valid)

        // Checksums corrompus
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb") is AddressValidationResult.InvalidFormat)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdr") is AddressValidationResult.InvalidFormat)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcs") is AddressValidationResult.InvalidFormat)
    }

    @Test
    fun `bitcoin address validator blocks seed phrases and WIF private keys`() {
        val seedPhrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress(seedPhrase) is AddressValidationResult.DangerousSeedPhraseDetected)

        val wifKey = "5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ"
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress(wifKey) is AddressValidationResult.DangerousPrivateKeyDetected)
    }

    @Test
    fun `encrypted key store stores compute and gemini keys separately and masks logs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyStore = EncryptedKeyStore(context)
        keyStore.saveComputeApiKey("compute-secret-12345")
        keyStore.saveGeminiApiKey("gemini-secret-67890")

        assertEquals("compute-secret-12345", keyStore.getComputeApiKey())
        assertEquals("gemini-secret-67890", keyStore.getGeminiApiKey())

        val maskedLog = EncryptedKeyStore.redactSensitive(
            message = "Auth header compute-secret-12345 for bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
            secrets = listOf("compute-secret-12345"),
            addresses = listOf("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")
        )
        assertFalse(maskedLog.contains("compute-secret-12345"))
        assertFalse(maskedLog.contains("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
    }
}
