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

    @Test
    fun `mining engine exposes recent validated bitcoin transactions with date and amount`() {
        val engine = com.example.mining.MiningEngine()
        val transactions = engine.telemetry.value.recentTransactions

        assertTrue("Doit contenir les dernières transactions Bitcoin validées", transactions.isNotEmpty())
        transactions.forEach { tx ->
            assertTrue("Le montant BTC doit être positif", tx.amountBtc > 0.0)
            assertTrue("La date de transaction doit être valide", tx.timestamp > 0L)
            assertTrue("Le numéro de bloc doit être valide", tx.blockNumber > 0L)
        }
    }

    @Test
    fun `mining engine exposes positive hashrate history for real-time line chart`() {
        val engine = com.example.mining.MiningEngine()
        val telemetry = engine.telemetry.value

        assertTrue("L'historique du hashrate doit contenir au moins 2 points", telemetry.hashrateHistory.size >= 2)
        assertTrue("Les valeurs du hashrate doivent être positives", telemetry.hashrateHistory.all { it > 0f })
        assertTrue("Le pic de hashrate doit être supérieur ou égal au taux actuel", telemetry.peakHashrate >= telemetry.hashrate)
    }

    @Test
    fun `mining engine logs error alert when an API key is unresponsive`() {
        val engine = com.example.mining.MiningEngine()
        engine.updateApiKeyHealth(unresponsiveCount = 1, failedKeyLabels = listOf("Clé API #2"))

        val telemetry = engine.telemetry.value
        assertEquals(1, telemetry.unresponsiveApiKeysCount)
        assertTrue(
            "Doit contenir un log d'erreur signalant la clé API qui ne répond pas",
            telemetry.logs.any {
                it.level == com.example.data.model.LogLevel.ERROR && it.message.contains("Clé API #2")
            }
        )
    }

    @Test
    fun `mining ai advisor flags failing API keys as unresponsive`() = kotlinx.coroutines.test.runTest {
        val mockClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                val code = if (url.contains("bad-key")) 403 else 200
                val body = if (code == 200) {
                    """{"candidates":[{"content":{"parts":[{"text":"Réponse OK"}]}}]}"""
                } else {
                    """{"error":{"message":"API key invalid"}}"""
                }
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Forbidden")
                    .body(okhttp3.ResponseBody.create(null, body))
                    .build()
            }
            .build()

        val advisor = com.example.ai.MiningAiAdvisor(client = mockClient)
        val health = advisor.verifyApiKey("bad-key-123")
        assertEquals(com.example.ai.ApiKeyState.UNRESPONSIVE, health.state)

        val reply = advisor.askAdvisorWithHealth(
            prompt = "Test",
            customApiKeys = listOf("bad-key-123", "good-key-456")
        )
        assertTrue(
            "La clé invalide doit être détectée comme ne répondant pas",
            reply.keyResults["bad-key-123"]?.state == com.example.ai.ApiKeyState.UNRESPONSIVE
        )
    }
}
