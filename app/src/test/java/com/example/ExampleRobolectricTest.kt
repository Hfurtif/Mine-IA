package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.mining.AddressValidationResult
import com.example.mining.SecurityValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Miner IA", appName)
    }

    @Test
    fun `supported cryptos contains only Bitcoin BTC`() {
        val all = com.example.data.model.SupportedCryptos.ALL
        assertEquals(1, all.size)
        assertEquals("BTC", all.first().symbol)
        assertEquals("Bitcoin", all.first().name)
    }

    @Test
    fun `security validator blocks seed phrase with critical alert`() {
        // A 12-word mnemonic phrase
        val seedPhrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val result = SecurityValidator.validatePublicAddress("BTC", seedPhrase)

        assertTrue(
            "Must detect and block 12-word seed phrase",
            result is AddressValidationResult.DangerousSeedPhraseDetected
        )
    }

    @Test
    fun `security validator allows valid Bitcoin Bech32 and Legacy public addresses`() {
        val validBech32 = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
        val validLegacy = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"

        assertTrue(
            "Must allow valid Bitcoin Bech32 address",
            SecurityValidator.validatePublicAddress("BTC", validBech32) is AddressValidationResult.Valid
        )
        assertTrue(
            "Must allow valid Bitcoin Legacy address",
            SecurityValidator.validatePublicAddress("BTC", validLegacy) is AddressValidationResult.Valid
        )
    }

    @Test
    fun `security validator rejects non-Bitcoin addresses`() {
        val moneroAddress = "48edfHu7V9Z84YzzMa6fUuiLiCWq7OjebcZKqTpHJS32GU2bU5eu4N4BJuV64gBRqVtxQBPRqWT6Y6aFQFiR2En7V5bExm7"
        val result = SecurityValidator.validatePublicAddress("BTC", moneroAddress)

        assertTrue(
            "Must reject non-Bitcoin addresses",
            result is AddressValidationResult.InvalidFormat
        )
    }

    @Test
    fun `mining engine guarantees 0 percent local CPU and GPU usage for autonomous cloud mining`() {
        val engine = com.example.mining.MiningEngine()
        val telemetry = engine.telemetry.value

        assertEquals(0, telemetry.deviceCpuUsagePercent)
        assertEquals(0, telemetry.deviceGpuUsagePercent)
        assertTrue(telemetry.isAutonomousCloud)
    }

    @Test
    fun `preferences initialize at least 3 API key slots and persist multiple keys`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.local.AppPreferences(context)

        val defaultSlots = prefs.getAiApiKeySlots()
        assertTrue("Doit contenir au moins 3 emplacements de clés API", defaultSlots.size >= 3)

        prefs.saveAiApiKeys(listOf("AIzaKeyOne111", "AIzaKeyTwo222", "AIzaKeyThree333", "AIzaKeyFour444"))
        val active = prefs.getActiveAiApiKeys()
        assertEquals(4, active.size)
        assertTrue(active.contains("AIzaKeyThree333"))
    }

    @Test
    fun `mining engine boosts speed multiplier with 3 or more API keys`() {
        val engine = com.example.mining.MiningEngine()
        engine.updateApiKeysCount(3)
        val telemetry = engine.telemetry.value

        assertEquals(3, telemetry.activeApiKeysCount)
        assertTrue("Le multiplicateur doit être nettement supérieur à 1.0", telemetry.speedMultiplier >= 2.9)
    }
}
