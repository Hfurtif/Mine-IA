package com.minerrelay.app.network

import com.minerrelay.app.security.EncryptedKeyStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class StratumJob(
    val jobId: String,
    val prevHashHex: String,
    val coinb1Hex: String,
    val coinb2Hex: String,
    val merkleBranchHex: List<String>,
    val versionHex: String,
    val nbitsHex: String,
    val ntimeHex: String,
    val cleanJobs: Boolean,
    val extranonce1Hex: String,
    val extranonce2Size: Int,
    val difficulty: Double
)

data class StratumEndpoint(
    val rawHost: String,
    val port: Int,
    val isBackup: Boolean = false,
    val forceTls: Boolean = false
) {
    val cleanHost: String
        get() = StratumClient.cleanStratumHost(rawHost)

    val useTls: Boolean
        get() = forceTls ||
            rawHost.trim().startsWith("stratum+ssl://", ignoreCase = true) ||
            rawHost.trim().startsWith("ssl://", ignoreCase = true)

    fun displayLabel(): String {
        val role = if (isBackup) "Secours" else "Principal"
        val proto = if (useTls) "SSL" else "TCP"
        return "$cleanHost:$port ($role • $proto)"
    }
}

sealed class ShareSubmissionResult {
    object Accepted : ShareSubmissionResult()
    data class Stale(val reason: String) : ShareSubmissionResult()
    data class Rejected(val reason: String) : ShareSubmissionResult()
}

enum class PoolConnectionState(val labelFr: String) {
    DISCONNECTED("Déconnecté"),
    CONNECTING("Connexion TCP au pool..."),
    SUBSCRIBING("Souscription Stratum..."),
    AUTHORIZING("Authentification worker..."),
    CONNECTED("Connecté au pool"),
    RECONNECTING("Reconnexion (backoff)...")
}

interface StratumEventListener {
    fun onConnectionStateChanged(state: PoolConnectionState, detail: String)
    fun onActiveEndpointChanged(endpointLabel: String, isBackup: Boolean) {}
    fun onJobReceived(job: StratumJob)
    fun onDifficultyChanged(difficulty: Double)
    fun onDebugLog(direction: String, maskedPayload: String)
    fun onRawTestResponse(rawLine: String)
}

/**
 * Client Stratum v1 sur socket TCP pur (`java.net.Socket`, sans OkHttp).
 * - Utilise l'identifiant du pool TEL QUEL dans `mining.authorize` et `mining.submit` (sans adresse BTC).
 * - Mot de passe facultatif (« x » par défaut si laissé vide).
 * - Supporte un pool de secours (même hôte, ex: port 443) avec bascule automatique en cas d'échec.
 */
class StratumClient(
    private val scope: CoroutineScope,
    private val listener: StratumEventListener
) {
    private val requestIdCounter = AtomicInteger(1)
    private val pendingResponses = ConcurrentHashMap<Int, CompletableDeferred<JSONObject>>()
    private val writeChannel = Channel<String>(Channel.BUFFERED)

    @Volatile
    private var socket: Socket? = null

    @Volatile
    private var sessionJob: Job? = null

    @Volatile
    private var shouldRun = false

    @Volatile
    private var currentDifficulty: Double = 1.0

    @Volatile
    private var extranonce1Hex: String = ""

    @Volatile
    private var extranonce2Size: Int = 4

    @Volatile
    private var authorizedUser: String = ""

    @Volatile
    private var activePasswordToMask: String = ""

    fun start(
        rawHost: String,
        port: Int,
        poolLogin: String,
        poolPassword: String = "",
        backupPort: Int? = null,
        backupUseTls: Boolean = false,
        testModeLowDifficulty: Boolean = false
    ) {
        stop()
        shouldRun = true
        authorizedUser = poolLogin.trim()
        val effectivePassword = resolveAuthorizePassword(poolPassword)
        activePasswordToMask = if (poolPassword.trim().isNotEmpty()) effectivePassword else ""

        val endpoints = buildEndpointList(
            rawHost = rawHost,
            primaryPort = port,
            backupPort = backupPort,
            backupUseTls = backupUseTls
        )

        sessionJob = scope.launch(Dispatchers.IO) {
            var backoffMs = 2_000L
            var endpointIndex = 0

            while (isActive && shouldRun) {
                val endpoint = endpoints[endpointIndex]
                val endpointLabel = endpoint.displayLabel()
                listener.onActiveEndpointChanged(endpointLabel, endpoint.isBackup)

                var connectionEstablished = false
                try {
                    connectAndRunLoop(
                        endpoint = endpoint,
                        workerLogin = authorizedUser,
                        workerPassword = effectivePassword,
                        testModeLowDifficulty = testModeLowDifficulty,
                        onSessionConnected = {
                            connectionEstablished = true
                            backoffMs = 2_000L
                        }
                    )
                } catch (e: Exception) {
                    if (!shouldRun) break
                    val errMsg = e.message ?: e.javaClass.simpleName
                    listener.onDebugLog("ERR", "Stratum déconnecté ($endpointLabel) : $errMsg")

                    if (endpoints.size > 1) {
                        val nextIndex = (endpointIndex + 1) % endpoints.size
                        val nextEndpoint = endpoints[nextIndex]
                        val switchingToBackup = !endpoint.isBackup && nextEndpoint.isBackup
                        val waitMs = if (switchingToBackup && !connectionEstablished) {
                            1_000L
                        } else {
                            backoffMs
                        }

                        listener.onDebugLog(
                            "FAILOVER",
                            "Bascule automatique vers ${nextEndpoint.displayLabel()} dans ${waitMs / 1000.0}s"
                        )
                        listener.onConnectionStateChanged(
                            PoolConnectionState.RECONNECTING,
                            "Bascule vers ${nextEndpoint.displayLabel()} ($errMsg)"
                        )
                        delay(waitMs)
                        if (!switchingToBackup || connectionEstablished) {
                            backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
                        }
                        endpointIndex = nextIndex
                    } else {
                        listener.onConnectionStateChanged(
                            PoolConnectionState.RECONNECTING,
                            "Reconnexion dans ${backoffMs / 1000}s ($errMsg)"
                        )
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
                    }
                } finally {
                    closeSocketQuietly()
                }
            }
            listener.onConnectionStateChanged(PoolConnectionState.DISCONNECTED, "Relais Stratum arrêté")
        }
    }

    fun stop() {
        shouldRun = false
        sessionJob?.cancel()
        sessionJob = null
        closeSocketQuietly()
        pendingResponses.values.forEach { it.cancel() }
        pendingResponses.clear()
        listener.onActiveEndpointChanged("", false)
        listener.onConnectionStateChanged(PoolConnectionState.DISCONNECTED, "Déconnecté")
    }

    suspend fun submitShare(
        jobId: String,
        extranonce2Hex: String,
        ntimeHex: String,
        nonceHex: String
    ): ShareSubmissionResult = withContext(Dispatchers.IO) {
        val params = buildSubmitParams(
            poolLogin = authorizedUser,
            jobId = jobId,
            extranonce2Hex = extranonce2Hex,
            ntimeHex = ntimeHex,
            nonceHex = nonceHex
        )
        val response = sendRequest("mining.submit", params, timeoutMs = 15_000L)
            ?: return@withContext ShareSubmissionResult.Rejected("Timeout pool lors de mining.submit")

        val accepted = response.optBoolean("result", false)
        if (accepted) {
            return@withContext ShareSubmissionResult.Accepted
        }

        val errObj = response.opt("error")
        val errText = errObj?.toString() ?: "Part rejetée par le pool"
        val isStale = errText.contains("stale", ignoreCase = true) ||
            errText.contains("job not found", ignoreCase = true) ||
            errText.contains("[21", ignoreCase = true)

        if (isStale) {
            ShareSubmissionResult.Stale(errText)
        } else {
            ShareSubmissionResult.Rejected(errText)
        }
    }

    private suspend fun connectAndRunLoop(
        endpoint: StratumEndpoint,
        workerLogin: String,
        workerPassword: String,
        testModeLowDifficulty: Boolean,
        onSessionConnected: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val cleanHost = endpoint.cleanHost
        val port = endpoint.port

        listener.onConnectionStateChanged(
            PoolConnectionState.CONNECTING,
            endpoint.displayLabel()
        )

        val rawTcpSocket = Socket().apply {
            connect(InetSocketAddress(cleanHost, port), 15_000)
            soTimeout = 120_000
            keepAlive = true
        }
        val createdSocket: Socket = if (endpoint.useTls) {
            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslFactory
                .createSocket(rawTcpSocket, cleanHost, port, true) as SSLSocket
            sslSocket.soTimeout = 120_000
            sslSocket.startHandshake()
            sslSocket
        } else {
            rawTcpSocket
        }
        socket = createdSocket

        val reader = BufferedReader(InputStreamReader(createdSocket.getInputStream(), Charsets.UTF_8))
        val writer = BufferedWriter(OutputStreamWriter(createdSocket.getOutputStream(), Charsets.UTF_8))

        val writerJob = scope.launch(Dispatchers.IO) {
            try {
                for (line in writeChannel) {
                    writer.write(line)
                    writer.write("\n")
                    writer.flush()
                }
            } catch (_: Exception) {
                // La boucle de lecture gèrera la fermeture
            }
        }

        val readerJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive && shouldRun) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    if (testModeLowDifficulty) {
                        listener.onRawTestResponse(maskLine(line))
                    }
                    listener.onDebugLog("RX", maskLine(line))
                    handleIncomingLine(line)
                }
            } catch (_: Exception) {
                // Sortie de lecture déclenche la reconnexion / bascule
            } finally {
                pendingResponses.values.forEach { it.cancel() }
                pendingResponses.clear()
            }
        }

        try {
            // 1. mining.subscribe
            listener.onConnectionStateChanged(
                PoolConnectionState.SUBSCRIBING,
                "${endpoint.displayLabel()} • mining.subscribe"
            )
            val subResp = sendRequest("mining.subscribe", JSONArray().put("MinerRelay/1.0"), 12_000L)
                ?: throw IllegalStateException("Aucune réponse à mining.subscribe")

            val subResult = subResp.optJSONArray("result")
                ?: throw IllegalStateException("Réponse mining.subscribe invalide : ${subResp.opt("error")}")
            extranonce1Hex = subResult.optString(1, "")
            extranonce2Size = subResult.optInt(2, 4).coerceIn(1, 8)
            if (extranonce1Hex.isEmpty()) {
                throw IllegalStateException("extranonce1 vide reçu du pool")
            }

            // 2. En mode test, demander une faible difficulté au pool
            if (testModeLowDifficulty) {
                sendRequest("mining.suggest_difficulty", JSONArray().put(0.01), 5_000L)
            }

            // 3. mining.authorize (identifiant du pool TEL QUEL, sans adresse BTC)
            listener.onConnectionStateChanged(
                PoolConnectionState.AUTHORIZING,
                workerLogin
            )
            val authParams = buildAuthorizeParams(workerLogin, workerPassword)
            val authResp = sendRequest("mining.authorize", authParams, 12_000L)
                ?: throw IllegalStateException("Aucune réponse à mining.authorize")
            if (!authResp.optBoolean("result", false)) {
                throw IllegalStateException("Identifiant refusé par le pool : ${authResp.opt("error")}")
            }

            onSessionConnected()
            listener.onConnectionStateChanged(
                PoolConnectionState.CONNECTED,
                "${endpoint.displayLabel()} (extranonce2=${extranonce2Size}o)"
            )

            readerJob.join()
        } finally {
            readerJob.cancel()
            writerJob.cancel()
        }
    }

    private suspend fun sendRequest(method: String, params: JSONArray, timeoutMs: Long): JSONObject? {
        val id = requestIdCounter.getAndIncrement()
        val deferred = CompletableDeferred<JSONObject>()
        pendingResponses[id] = deferred

        val payload = JSONObject().apply {
            put("id", id)
            put("method", method)
            put("params", params)
        }.toString()

        listener.onDebugLog("TX", maskLine(payload))
        writeChannel.send(payload)

        return try {
            withTimeoutOrNull(timeoutMs) { deferred.await() }
        } finally {
            pendingResponses.remove(id)
        }
    }

    private fun handleIncomingLine(line: String) {
        val json = try {
            JSONObject(line)
        } catch (_: Exception) {
            return
        }

        if (json.has("id") && !json.isNull("id") && !json.has("method")) {
            val id = json.optInt("id", -1)
            pendingResponses.remove(id)?.complete(json)
            return
        }

        val method = json.optString("method", "")
        val params = json.optJSONArray("params") ?: JSONArray()

        when (method) {
            "mining.set_difficulty" -> {
                val diff = params.optDouble(0, currentDifficulty)
                if (diff > 0.0) {
                    currentDifficulty = diff
                    listener.onDifficultyChanged(diff)
                }
            }
            "mining.set_extranonce" -> {
                val newEn1 = params.optString(0, "")
                val newEn2Size = params.optInt(1, extranonce2Size)
                if (newEn1.isNotEmpty()) {
                    extranonce1Hex = newEn1
                    extranonce2Size = newEn2Size.coerceIn(1, 8)
                }
            }
            "mining.notify" -> {
                if (params.length() >= 9) {
                    val merkleArray = params.optJSONArray(4) ?: JSONArray()
                    val branches = buildList {
                        for (i in 0 until merkleArray.length()) {
                            add(merkleArray.optString(i))
                        }
                    }
                    val job = StratumJob(
                        jobId = params.optString(0),
                        prevHashHex = params.optString(1),
                        coinb1Hex = params.optString(2),
                        coinb2Hex = params.optString(3),
                        merkleBranchHex = branches,
                        versionHex = params.optString(5),
                        nbitsHex = params.optString(6),
                        ntimeHex = params.optString(7),
                        cleanJobs = params.optBoolean(8, false),
                        extranonce1Hex = extranonce1Hex,
                        extranonce2Size = extranonce2Size,
                        difficulty = currentDifficulty
                    )
                    listener.onJobReceived(job)
                }
            }
            "client.reconnect" -> {
                closeSocketQuietly()
            }
        }
    }

    private fun maskLine(raw: String): String {
        val secrets = if (activePasswordToMask.isNotEmpty()) listOf(activePasswordToMask) else emptyList()
        return EncryptedKeyStore.redactSensitive(raw, secrets, emptyList())
    }

    private fun closeSocketQuietly() {
        try {
            socket?.close()
        } catch (_: Exception) {
        } finally {
            socket = null
        }
    }

    companion object {
        fun cleanStratumHost(rawHost: String): String {
            return rawHost.trim()
                .removePrefix("stratum+tcp://")
                .removePrefix("stratum+ssl://")
                .removePrefix("tcp://")
                .removePrefix("ssl://")
                .substringBefore("/")
                .substringBefore(":")
        }

        fun resolveAuthorizePassword(rawPassword: String): String {
            val trimmed = rawPassword.trim()
            return trimmed.ifEmpty { "x" }
        }

        fun buildAuthorizeParams(poolLogin: String, poolPassword: String): JSONArray {
            return JSONArray().apply {
                put(poolLogin.trim())
                put(resolveAuthorizePassword(poolPassword))
            }
        }

        fun buildSubmitParams(
            poolLogin: String,
            jobId: String,
            extranonce2Hex: String,
            ntimeHex: String,
            nonceHex: String
        ): JSONArray {
            return JSONArray().apply {
                put(poolLogin.trim())
                put(jobId)
                put(extranonce2Hex)
                put(ntimeHex)
                put(nonceHex)
            }
        }

        fun buildEndpointList(
            rawHost: String,
            primaryPort: Int,
            backupPort: Int?,
            backupUseTls: Boolean
        ): List<StratumEndpoint> {
            val primary = StratumEndpoint(rawHost = rawHost, port = primaryPort, isBackup = false, forceTls = false)
            if (backupPort != null && backupPort in 1..65535 && backupPort != primaryPort) {
                val backup = StratumEndpoint(
                    rawHost = rawHost,
                    port = backupPort,
                    isBackup = true,
                    forceTls = backupUseTls
                )
                return listOf(primary, backup)
            }
            return listOf(primary)
        }
    }
}
