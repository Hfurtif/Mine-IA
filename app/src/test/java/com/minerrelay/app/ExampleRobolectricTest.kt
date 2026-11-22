package com.minerrelay.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.minerrelay.app.data.local.AppPreferences
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.mining.AddressValidationResult
import com.minerrelay.app.mining.SecurityValidator
import com.minerrelay.app.network.PoolConnectionState
import com.minerrelay.app.network.ShareSubmissionResult
import com.minerrelay.app.network.StratumClient
import com.minerrelay.app.network.StratumEventListener
import com.minerrelay.app.network.StratumJob
import com.minerrelay.app.security.EncryptedKeyStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

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
        assertEquals("", config.poolLogin)
        assertEquals(false, config.backupPoolEnabled)
        assertEquals(443, config.backupPoolPort)
        assertEquals("", config.btcAddress)
        assertFalse(config.isPoolConfigured)
    }

    @Test
    fun `save and load poolLogin encrypted poolPassword and backup pool 443`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AppPreferences(context)

        prefs.saveRelaySettings(
            poolHost = "stratum.example.invalid",
            poolPortText = "3333",
            poolLogin = "Hfurtif.worker1",
            poolPassword = "optional-pool-pass",
            backupPoolEnabled = true,
            backupPoolPortText = "443",
            backupPoolUseTls = true,
            btcAddress = "",
            computeApiUrl = "",
            computeApiKey = "compute-key-1234",
            geminiApiKey = "",
            wifiOnly = false,
            chargingOnly = false,
            testMode = false
        )

        val loaded = prefs.loadRelayConfig()
        assertEquals("stratum.example.invalid", loaded.poolHost)
        assertEquals(3333, loaded.poolPort)
        assertEquals("Hfurtif.worker1", loaded.poolLogin)
        assertTrue(loaded.backupPoolEnabled)
        assertEquals(443, loaded.backupPoolPort)
        assertTrue(loaded.backupPoolUseTls)
        assertTrue(loaded.isPoolConfigured)
        assertTrue(loaded.isBackupPoolConfigured)
        assertEquals("optional-pool-pass", prefs.getPoolPassword())
        assertEquals("compute-key-1234", prefs.getComputeApiKey())
    }

    @Test
    fun `bitcoin address validator verifies Base58Check and Bech32 Bech32m checksums strictly`() {
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa") is AddressValidationResult.Valid)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy") is AddressValidationResult.Valid)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq") is AddressValidationResult.Valid)
        assertTrue(SecurityValidator.validateBitcoinMainnetAddress("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr") is AddressValidationResult.Valid)

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
    fun `encrypted key store stores compute gemini and pool password separately and masks logs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyStore = EncryptedKeyStore(context)
        keyStore.saveComputeApiKey("compute-secret-12345")
        keyStore.saveGeminiApiKey("gemini-secret-67890")
        keyStore.savePoolPassword("pool-secret-abcde")

        assertEquals("compute-secret-12345", keyStore.getComputeApiKey())
        assertEquals("gemini-secret-67890", keyStore.getGeminiApiKey())
        assertEquals("pool-secret-abcde", keyStore.getPoolPassword())

        val maskedLog = EncryptedKeyStore.redactSensitive(
            message = "Auth header compute-secret-12345 pass pool-secret-abcde for bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
            secrets = listOf("compute-secret-12345", "pool-secret-abcde"),
            addresses = listOf("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")
        )
        assertFalse(maskedLog.contains("compute-secret-12345"))
        assertFalse(maskedLog.contains("pool-secret-abcde"))
        assertFalse(maskedLog.contains("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
    }

    @Test
    fun `stratum authorize and submit use poolLogin verbatim and optional password defaults to x`() {
        val defaultPassParams = StratumClient.buildAuthorizeParams(" Hfurtif.worker1 ", "")
        assertEquals("Hfurtif.worker1", defaultPassParams.getString(0))
        assertEquals("x", defaultPassParams.getString(1))

        val customPassParams = StratumClient.buildAuthorizeParams("Hfurtif.worker1", "secret123")
        assertEquals("Hfurtif.worker1", customPassParams.getString(0))
        assertEquals("secret123", customPassParams.getString(1))

        val submitParams = StratumClient.buildSubmitParams(
            poolLogin = "Hfurtif.worker1",
            jobId = "job42",
            extranonce2Hex = "00000001",
            ntimeHex = "65000001",
            nonceHex = "deadbeef"
        )
        assertEquals("Hfurtif.worker1", submitParams.getString(0))
        assertEquals("job42", submitParams.getString(1))
        assertEquals("00000001", submitParams.getString(2))
        assertEquals("65000001", submitParams.getString(3))
        assertEquals("deadbeef", submitParams.getString(4))

        val configWithoutBtc = RelayConfig(
            poolHost = "pool.example.invalid",
            poolPort = 3333,
            poolLogin = "Hfurtif.worker1",
            backupPoolEnabled = true,
            backupPoolPort = 443,
            btcAddress = ""
        )
        assertTrue(configWithoutBtc.isPoolConfigured)
        assertTrue(configWithoutBtc.isBackupPoolConfigured)

        val endpoints = StratumClient.buildEndpointList("pool.example.invalid", 3333, 443, backupUseTls = true)
        assertEquals(2, endpoints.size)
        assertEquals(3333, endpoints[0].port)
        assertFalse(endpoints[0].isBackup)
        assertEquals(443, endpoints[1].port)
        assertTrue(endpoints[1].isBackup)
        assertTrue(endpoints[1].useTls)
    }

    @Test
    fun `stratum client automatically fails over to backup port and uses poolLogin verbatim`() = runBlocking {
        val closedSocket = ServerSocket(0)
        val deadPrimaryPort = closedSocket.localPort
        closedSocket.close()

        val backupServer = ServerSocket(0)
        val backupPort = backupServer.localPort

        val observedAuthorizeUser = CompletableDeferred<String>()
        val observedAuthorizePass = CompletableDeferred<String>()
        val observedSubmitUser = CompletableDeferred<String>()
        val backupActivated = CompletableDeferred<Boolean>()
        val jobReceived = CompletableDeferred<StratumJob>()

        val serverThread = thread(start = true) {
            backupServer.use { srv ->
                val clientSock = srv.accept()
                clientSock.use { sock ->
                    val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                    val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
                    while (true) {
                        val line = reader.readLine() ?: break
                        val req = JSONObject(line)
                        val id = req.getInt("id")
                        val method = req.getString("method")
                        val params = req.getJSONArray("params")
                        when (method) {
                            "mining.subscribe" -> {
                                val resp = JSONObject().apply {
                                    put("id", id)
                                    put("result", JSONArray().put(JSONArray()).put("08000001").put(4))
                                    put("error", JSONObject.NULL)
                                }
                                writer.write(resp.toString() + "\n")
                                writer.flush()
                            }
                            "mining.authorize" -> {
                                observedAuthorizeUser.complete(params.getString(0))
                                observedAuthorizePass.complete(params.getString(1))
                                val resp = JSONObject().apply {
                                    put("id", id)
                                    put("result", true)
                                    put("error", JSONObject.NULL)
                                }
                                writer.write(resp.toString() + "\n")
                                writer.flush()

                                val notify = JSONObject().apply {
                                    put("id", JSONObject.NULL)
                                    put("method", "mining.notify")
                                    put("params", JSONArray().apply {
                                        put("job-failover-1")
                                        put("00".repeat(32))
                                        put("01000000")
                                        put("ffffffff")
                                        put(JSONArray())
                                        put("20000000")
                                        put("1d00ffff")
                                        put("65000001")
                                        put(true)
                                    })
                                }
                                writer.write(notify.toString() + "\n")
                                writer.flush()
                            }
                            "mining.submit" -> {
                                observedSubmitUser.complete(params.getString(0))
                                val resp = JSONObject().apply {
                                    put("id", id)
                                    put("result", true)
                                    put("error", JSONObject.NULL)
                                }
                                writer.write(resp.toString() + "\n")
                                writer.flush()
                                break
                            }
                        }
                    }
                }
            }
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val client = StratumClient(scope, object : StratumEventListener {
            override fun onConnectionStateChanged(state: PoolConnectionState, detail: String) = Unit
            override fun onActiveEndpointChanged(endpointLabel: String, isBackup: Boolean) {
                if (isBackup && !backupActivated.isCompleted) {
                    backupActivated.complete(true)
                }
            }
            override fun onJobReceived(job: StratumJob) {
                if (!jobReceived.isCompleted) jobReceived.complete(job)
            }
            override fun onDifficultyChanged(difficulty: Double) = Unit
            override fun onDebugLog(direction: String, maskedPayload: String) = Unit
            override fun onRawTestResponse(rawLine: String) = Unit
        })

        try {
            client.start(
                rawHost = "127.0.0.1",
                port = deadPrimaryPort,
                poolLogin = "Hfurtif.worker1",
                poolPassword = "",
                backupPort = backupPort,
                backupUseTls = false,
                testModeLowDifficulty = false
            )

            withTimeout(10_000L) {
                assertTrue(backupActivated.await())
                assertEquals("Hfurtif.worker1", observedAuthorizeUser.await())
                assertEquals("x", observedAuthorizePass.await())
                val job = jobReceived.await()
                assertEquals("job-failover-1", job.jobId)

                val submitResult = client.submitShare(job.jobId, "00000001", job.ntimeHex, "00000001")
                assertTrue(submitResult is ShareSubmissionResult.Accepted)
                assertEquals("Hfurtif.worker1", observedSubmitUser.await())
            }
        } finally {
            client.stop()
            scope.cancel()
            serverThread.join(2_000L)
        }
    }
}
