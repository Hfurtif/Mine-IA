package com.minerrelay.app.mining

import com.minerrelay.app.blockchain.BlockchainTransaction
import com.minerrelay.app.blockchain.MempoolSpaceClient
import com.minerrelay.app.compute.ComputeBatchRequest
import com.minerrelay.app.compute.ComputeBatchResult
import com.minerrelay.app.compute.RemoteComputeClient
import com.minerrelay.app.data.model.LogLevel
import com.minerrelay.app.data.model.MiningLogMessage
import com.minerrelay.app.data.model.MiningStatus
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.network.PoolConnectionState
import com.minerrelay.app.network.ShareSubmissionResult
import com.minerrelay.app.network.StratumClient
import com.minerrelay.app.network.StratumEventListener
import com.minerrelay.app.network.StratumJob
import com.minerrelay.app.security.EncryptedKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class AcceptedShareRecord(
    val timestampMs: Long,
    val difficulty: Double
)

data class DebugLogEntry(
    val id: Long = System.nanoTime(),
    val timestampMs: Long = System.currentTimeMillis(),
    val direction: String,
    val message: String
)

data class MiningTelemetry(
    val status: MiningStatus = MiningStatus.IDLE,
    val poolConnectionState: PoolConnectionState = PoolConnectionState.DISCONNECTED,
    val poolDetail: String = "Non connecté",
    val activePoolEndpoint: String = "",
    val isUsingBackupPool: Boolean = false,
    val isTestMode: Boolean = false,
    val currentJobId: String? = null,
    val currentDifficulty: Double = 0.0,
    val acceptedShares: Long = 0L,
    val rejectedShares: Long = 0L,
    val staleShares: Long = 0L,
    val locallyRejectedCandidates: Long = 0L,
    val batchesDispatched: Long = 0L,
    val hashrate15mHps: Double = 0.0,
    val hashrate24hHps: Double = 0.0,
    val hashrateHistoryHps: List<Float> = emptyList(),
    val serviceLatencyMs: Long? = null,
    val accumulatedServiceCostUsd: Double? = null,
    val computeErrorMessage: String? = null,
    val isServiceKeyUnresponsive: Boolean = false,
    val recentTransactions: List<BlockchainTransaction> = emptyList(),
    val totalConfirmedPayoutsBtc: Double = 0.0,
    val btcPriceEur: Double? = null,
    val isFetchingBlockchain: Boolean = false,
    val blockchainError: String? = null,
    val rawTestResponses: List<String> = emptyList(),
    val debugLogs: List<DebugLogEntry> = emptyList(),
    val activityLogs: List<MiningLogMessage> = emptyList()
)

/**
 * Orchestrateur réel :
 * Pool Stratum v1 ⇄ App (construction header 80 octets + vérif locale double SHA-256) ⇄ Service de calcul distant.
 * Aucune valeur aléatoire ni donnée factice : le hashrate est strictement calculé à partir des parts acceptées.
 */
class MiningEngine(
    private val scope: CoroutineScope,
    private val computeClient: RemoteComputeClient,
    private val mempoolClient: MempoolSpaceClient
) : StratumEventListener {

    private val _telemetry = MutableStateFlow(MiningTelemetry())
    val telemetry: StateFlow<MiningTelemetry> = _telemetry.asStateFlow()

    private val stratumClient = StratumClient(scope, this)
    private val acceptedShareRecords = mutableListOf<AcceptedShareRecord>()
    private val extranonce2Counter = AtomicLong(1L)

    @Volatile
    private var activeConfig: RelayConfig = RelayConfig()

    @Volatile
    private var activeComputeApiKey: String = ""

    @Volatile
    private var activePoolPassword: String = ""

    @Volatile
    private var currentJob: StratumJob? = null

    @Volatile
    private var isWifiConnected: Boolean = true

    @Volatile
    private var isDeviceCharging: Boolean = true

    @Volatile
    private var userWantsRelayActive: Boolean = false

    private var batchWorkerJob: Job? = null
    private var hashrateTickerJob: Job? = null
    private var sessionStartMs: Long = 0L

    fun startRelay(config: RelayConfig, computeApiKey: String, poolPassword: String = "") {
        activeConfig = config
        activeComputeApiKey = computeApiKey.trim()
        activePoolPassword = poolPassword.trim()
        userWantsRelayActive = true
        sessionStartMs = System.currentTimeMillis()
        currentJob = null

        val port = config.poolPort ?: 0
        if (config.poolHost.isBlank() || port !in 1..65535) {
            appendActivityLog(LogLevel.ERROR, "Hôte ou port du pool invalide. Configurez-les dans Réglages.")
            _telemetry.update { it.copy(status = MiningStatus.ERROR, poolDetail = "Pool non configuré") }
            userWantsRelayActive = false
            return
        }

        if (config.poolLogin.isBlank()) {
            appendActivityLog(LogLevel.ERROR, "Identifiant du pool manquant. Configurez-le dans Réglages.")
            _telemetry.update { it.copy(status = MiningStatus.ERROR, poolDetail = "Identifiant du pool manquant") }
            userWantsRelayActive = false
            return
        }

        _telemetry.update {
            it.copy(
                isTestMode = config.testMode,
                computeErrorMessage = null,
                isServiceKeyUnresponsive = false,
                rawTestResponses = if (config.testMode) emptyList() else it.rawTestResponses
            )
        }

        if (!constraintsSatisfied()) {
            applyConstraintPauseState()
            return
        }

        launchStratumAndBatchLoops()
        if (config.btcAddress.isNotBlank()) {
            refreshBlockchainTransactions(config.btcAddress)
        }
    }

    fun stopRelay() {
        userWantsRelayActive = false
        batchWorkerJob?.cancel()
        hashrateTickerJob?.cancel()
        batchWorkerJob = null
        hashrateTickerJob = null
        currentJob = null
        stratumClient.stop()
        _telemetry.update {
            it.copy(
                status = MiningStatus.IDLE,
                poolConnectionState = PoolConnectionState.DISCONNECTED,
                poolDetail = "Relais arrêté",
                currentJobId = null
            )
        }
        appendActivityLog(LogLevel.INFO, "Relais Stratum et envoi de lots arrêtés.")
    }

    fun onEnvironmentChanged(wifiConnected: Boolean, charging: Boolean, networkSwitched: Boolean = false) {
        isWifiConnected = wifiConnected
        isDeviceCharging = charging
        if (!userWantsRelayActive) return

        if (!constraintsSatisfied()) {
            batchWorkerJob?.cancel()
            stratumClient.stop()
            applyConstraintPauseState()
        } else if (
            _telemetry.value.status == MiningStatus.WAITING_WIFI ||
            _telemetry.value.status == MiningStatus.WAITING_CHARGING ||
            networkSwitched
        ) {
            appendActivityLog(LogLevel.INFO, "Conditions réseau/énergie rétablies : reconnexion au pool...")
            launchStratumAndBatchLoops()
        }
    }

    fun refreshBlockchainTransactions(btcAddress: String) {
        val cleanAddress = btcAddress.trim()
        if (cleanAddress.isEmpty() ||
            SecurityValidator.validateBitcoinMainnetAddress(cleanAddress) !is AddressValidationResult.Valid
        ) {
            _telemetry.update {
                it.copy(
                    recentTransactions = emptyList(),
                    totalConfirmedPayoutsBtc = 0.0,
                    isFetchingBlockchain = false,
                    blockchainError = null
                )
            }
            return
        }

        scope.launch {
            _telemetry.update { it.copy(isFetchingBlockchain = true, blockchainError = null) }
            val result = mempoolClient.fetchAddressTransactions(cleanAddress)
            result.onSuccess { summary ->
                val confirmedTotal = summary.transactions
                    .filter { it.confirmed }
                    .sumOf { it.amountBtc }
                _telemetry.update {
                    it.copy(
                        recentTransactions = summary.transactions,
                        totalConfirmedPayoutsBtc = confirmedTotal,
                        btcPriceEur = summary.btcPriceEur,
                        isFetchingBlockchain = false,
                        blockchainError = null
                    )
                }
            }.onFailure { err ->
                _telemetry.update {
                    it.copy(
                        isFetchingBlockchain = false,
                        blockchainError = "Lecture mempool.space impossible (${err.message ?: err.javaClass.simpleName})"
                    )
                }
            }
        }
    }

    fun clearLogs() {
        _telemetry.update {
            it.copy(debugLogs = emptyList(), activityLogs = emptyList(), rawTestResponses = emptyList())
        }
    }

    override fun onConnectionStateChanged(state: PoolConnectionState, detail: String) {
        val relayStatus = when (state) {
            PoolConnectionState.DISCONNECTED ->
                if (!userWantsRelayActive) MiningStatus.IDLE else _telemetry.value.status
            PoolConnectionState.CONNECTING,
            PoolConnectionState.SUBSCRIBING,
            PoolConnectionState.AUTHORIZING,
            PoolConnectionState.RECONNECTING -> MiningStatus.CONNECTING
            PoolConnectionState.CONNECTED -> MiningStatus.ACTIVE
        }
        _telemetry.update {
            it.copy(
                status = relayStatus,
                poolConnectionState = state,
                poolDetail = detail
            )
        }
    }

    override fun onActiveEndpointChanged(endpointLabel: String, isBackup: Boolean) {
        val previousBackup = _telemetry.value.isUsingBackupPool
        _telemetry.update {
            it.copy(
                activePoolEndpoint = endpointLabel,
                isUsingBackupPool = isBackup
            )
        }
        if (isBackup && !previousBackup && endpointLabel.isNotBlank()) {
            appendActivityLog(LogLevel.WARN, "Bascule automatique vers le pool de secours : $endpointLabel")
        }
    }

    override fun onDifficultyChanged(difficulty: Double) {
        _telemetry.update { it.copy(currentDifficulty = difficulty) }
        appendActivityLog(LogLevel.INFO, "Difficulté pool mise à jour : $difficulty")
    }

    override fun onJobReceived(job: StratumJob) {
        currentJob = job
        _telemetry.update {
            it.copy(
                currentJobId = job.jobId,
                currentDifficulty = job.difficulty
            )
        }
        if (job.cleanJobs) {
            appendActivityLog(LogLevel.INFO, "Nouveau job Stratum #${job.jobId} (clean_jobs=true, diff=${job.difficulty})")
        }
    }

    override fun onDebugLog(direction: String, maskedPayload: String) {
        val redacted = EncryptedKeyStore.redactSensitive(
            message = maskedPayload,
            secrets = listOf(activeComputeApiKey, activePoolPassword),
            addresses = listOf(activeConfig.btcAddress)
        )
        _telemetry.update { state ->
            val updated = (state.debugLogs + DebugLogEntry(direction = direction, message = redacted)).takeLast(200)
            state.copy(debugLogs = updated)
        }
    }

    override fun onRawTestResponse(rawLine: String) {
        val redacted = EncryptedKeyStore.redactSensitive(
            message = rawLine,
            secrets = listOf(activeComputeApiKey, activePoolPassword),
            addresses = listOf(activeConfig.btcAddress)
        )
        _telemetry.update { state ->
            state.copy(rawTestResponses = (state.rawTestResponses + redacted).takeLast(50))
        }
    }

    private fun constraintsSatisfied(): Boolean {
        if (activeConfig.wifiOnly && !isWifiConnected) return false
        if (activeConfig.chargingOnly && !isDeviceCharging) return false
        return true
    }

    private fun applyConstraintPauseState() {
        val waitStatus = when {
            activeConfig.wifiOnly && !isWifiConnected -> MiningStatus.WAITING_WIFI
            activeConfig.chargingOnly && !isDeviceCharging -> MiningStatus.WAITING_CHARGING
            else -> MiningStatus.PAUSED
        }
        val msg = waitStatus.labelFr
        _telemetry.update {
            it.copy(
                status = waitStatus,
                poolConnectionState = PoolConnectionState.DISCONNECTED,
                poolDetail = msg
            )
        }
        appendActivityLog(LogLevel.WARN, "Relais suspendu : $msg")
    }

    private fun launchStratumAndBatchLoops() {
        val config = activeConfig
        val port = config.poolPort ?: return

        stratumClient.start(
            rawHost = config.poolHost,
            port = port,
            poolLogin = config.poolLogin,
            poolPassword = activePoolPassword,
            backupPort = if (config.backupPoolEnabled) config.backupPoolPort else null,
            backupUseTls = config.backupPoolUseTls,
            testModeLowDifficulty = config.testMode
        )

        startHashrateTicker()
        startBatchDispatchLoop()
    }

    private fun startHashrateTicker() {
        hashrateTickerJob?.cancel()
        hashrateTickerJob = scope.launch(Dispatchers.Default) {
            while (isActive && userWantsRelayActive) {
                delay(5_000L)
                recomputeMeasuredHashrate(appendChartSample = true)
            }
        }
    }

    private fun startBatchDispatchLoop() {
        batchWorkerJob?.cancel()
        batchWorkerJob = scope.launch(Dispatchers.IO) {
            var nonceCursor = 0L
            var lastJobId = ""

            while (isActive && userWantsRelayActive) {
                val job = currentJob
                if (job == null) {
                    delay(400L)
                    continue
                }

                if (job.jobId != lastJobId) {
                    lastJobId = job.jobId
                    nonceCursor = 0L
                }

                val extranonce2Bytes = extranonce2Counter.getAndIncrement()
                val extranonce2Hex = formatExtranonce2(extranonce2Bytes, job.extranonce2Size)
                val nonceEnd = (nonceCursor + NONCE_BATCH_SIZE - 1L).coerceAtMost(MAX_UINT32)

                val target = BitcoinCrypto.difficultyToTarget(job.difficulty.coerceAtLeast(1e-6))
                val templateHeader80 = BitcoinCrypto.buildBlockHeaderFromStratum(
                    versionHex = job.versionHex,
                    stratumPrevHashHex = job.prevHashHex,
                    coinb1Hex = job.coinb1Hex,
                    extranonce1Hex = job.extranonce1Hex,
                    extranonce2Hex = extranonce2Hex,
                    coinb2Hex = job.coinb2Hex,
                    merkleBranchHexList = job.merkleBranchHex,
                    ntimeHex = job.ntimeHex,
                    nbitsHex = job.nbitsHex,
                    nonce = 0L
                )
                val prefix76Hex = BitcoinCrypto.bytesToHex(templateHeader80.copyOfRange(0, 76))
                val targetHex = BitcoinCrypto.targetToHex64(target)

                val batchRequest = ComputeBatchRequest(
                    jobId = job.jobId,
                    headerPrefix76BytesHex = prefix76Hex,
                    targetHex = targetHex,
                    nonceStart = nonceCursor,
                    nonceEnd = nonceEnd
                )

                val batchResult = computeClient.solveBatch(batchRequest, activeComputeApiKey)

                when (batchResult) {
                    is ComputeBatchResult.Success -> {
                        _telemetry.update { state ->
                            val newCost = when {
                                batchResult.batchCostUsd == null -> state.accumulatedServiceCostUsd
                                state.accumulatedServiceCostUsd == null -> batchResult.batchCostUsd
                                else -> state.accumulatedServiceCostUsd + batchResult.batchCostUsd
                            }
                            state.copy(
                                batchesDispatched = state.batchesDispatched + 1L,
                                serviceLatencyMs = batchResult.latencyMs,
                                accumulatedServiceCostUsd = newCost,
                                computeErrorMessage = null,
                                isServiceKeyUnresponsive = false
                            )
                        }

                        // Re-vérification locale obligatoire par double SHA-256 AVANT soumission au pool
                        for (candidateNonce in batchResult.candidateNonces) {
                            val fullHeader = BitcoinCrypto.buildBlockHeaderFromStratum(
                                versionHex = job.versionHex,
                                stratumPrevHashHex = job.prevHashHex,
                                coinb1Hex = job.coinb1Hex,
                                extranonce1Hex = job.extranonce1Hex,
                                extranonce2Hex = extranonce2Hex,
                                coinb2Hex = job.coinb2Hex,
                                merkleBranchHexList = job.merkleBranchHex,
                                ntimeHex = job.ntimeHex,
                                nbitsHex = job.nbitsHex,
                                nonce = candidateNonce
                            )

                            if (!BitcoinCrypto.verifyHeaderAgainstTarget(fullHeader, target)) {
                                _telemetry.update {
                                    it.copy(locallyRejectedCandidates = it.locallyRejectedCandidates + 1L)
                                }
                                onDebugLog(
                                    "VERIFY",
                                    "Candidat nonce=${"%08x".format(candidateNonce and 0xFFFFFFFFL)} rejeté localement (hash > cible)"
                                )
                                continue
                            }

                            val nonceHex = "%08x".format(candidateNonce and 0xFFFFFFFFL)
                            when (val submitRes = stratumClient.submitShare(job.jobId, extranonce2Hex, job.ntimeHex, nonceHex)) {
                                is ShareSubmissionResult.Accepted -> {
                                    recordAcceptedShare(job.difficulty)
                                    appendActivityLog(
                                        LogLevel.SUCCESS,
                                        "Part acceptée par le pool (job=${job.jobId}, diff=${job.difficulty}, nonce=$nonceHex)"
                                    )
                                }
                                is ShareSubmissionResult.Stale -> {
                                    _telemetry.update { it.copy(staleShares = it.staleShares + 1L) }
                                    appendActivityLog(LogLevel.WARN, "Part périmée (stale) : ${submitRes.reason}")
                                }
                                is ShareSubmissionResult.Rejected -> {
                                    _telemetry.update { it.copy(rejectedShares = it.rejectedShares + 1L) }
                                    appendActivityLog(LogLevel.ERROR, "Part rejetée par le pool : ${submitRes.reason}")
                                }
                            }
                        }

                        nonceCursor = if (nonceEnd >= MAX_UINT32) 0L else nonceEnd + 1L
                    }

                    is ComputeBatchResult.InvalidApiKey -> {
                        handleFatalComputeError(batchResult.message, markKeyBad = true)
                        return@launch
                    }

                    is ComputeBatchResult.InsufficientBalance -> {
                        handleFatalComputeError(batchResult.message, markKeyBad = false)
                        return@launch
                    }

                    is ComputeBatchResult.IncompleteApiConfiguration -> {
                        handleFatalComputeError(batchResult.message, markKeyBad = false)
                        return@launch
                    }

                    is ComputeBatchResult.QuotaExceeded -> {
                        _telemetry.update {
                            it.copy(computeErrorMessage = batchResult.message, isServiceKeyUnresponsive = true)
                        }
                        appendActivityLog(LogLevel.ERROR, batchResult.message)
                        delay(5_000L)
                    }

                    is ComputeBatchResult.ServiceUnavailable -> {
                        _telemetry.update {
                            it.copy(computeErrorMessage = batchResult.message, isServiceKeyUnresponsive = true)
                        }
                        appendActivityLog(LogLevel.WARN, batchResult.message)
                        delay(3_000L)
                    }
                }
            }
        }
    }

    private fun handleFatalComputeError(message: String, markKeyBad: Boolean) {
        _telemetry.update {
            it.copy(
                computeErrorMessage = message,
                isServiceKeyUnresponsive = markKeyBad
            )
        }
        appendActivityLog(LogLevel.ERROR, message)
    }

    @Synchronized
    private fun recordAcceptedShare(difficulty: Double) {
        val now = System.currentTimeMillis()
        acceptedShareRecords.add(AcceptedShareRecord(now, difficulty))
        val cutoff24h = now - WINDOW_24H_MS
        acceptedShareRecords.removeAll { it.timestampMs < cutoff24h }
        _telemetry.update { it.copy(acceptedShares = it.acceptedShares + 1L) }
        recomputeMeasuredHashrate(appendChartSample = true)
    }

    @Synchronized
    private fun recomputeMeasuredHashrate(appendChartSample: Boolean) {
        val now = System.currentTimeMillis()
        val snapshot = acceptedShareRecords.toList()
        val h15m = calculateHashrateHps(snapshot, WINDOW_15M_MS, now, sessionStartMs)
        val h24h = calculateHashrateHps(snapshot, WINDOW_24H_MS, now, sessionStartMs)

        _telemetry.update { state ->
            val updatedHistory = if (appendChartSample) {
                (state.hashrateHistoryHps + h15m.toFloat()).takeLast(30)
            } else {
                state.hashrateHistoryHps
            }
            state.copy(
                hashrate15mHps = h15m,
                hashrate24hHps = h24h,
                hashrateHistoryHps = updatedHistory
            )
        }
    }

    private fun formatExtranonce2(counter: Long, sizeBytes: Int): String {
        val hex = counter.toString(16)
        val targetLen = sizeBytes * 2
        return hex.padStart(targetLen, '0').takeLast(targetLen)
    }

    private fun appendActivityLog(level: LogLevel, rawMessage: String) {
        val safe = EncryptedKeyStore.redactSensitive(
            message = rawMessage,
            secrets = listOf(activeComputeApiKey, activePoolPassword),
            addresses = listOf(activeConfig.btcAddress)
        )
        _telemetry.update { state ->
            state.copy(
                activityLogs = (state.activityLogs + MiningLogMessage(level = level, message = safe)).takeLast(100)
            )
        }
    }

    companion object {
        const val WINDOW_15M_MS = 15L * 60L * 1000L
        const val WINDOW_24H_MS = 24L * 60L * 60L * 1000L
        private const val NONCE_BATCH_SIZE = 100_000_000L
        private const val MAX_UINT32 = 0xFFFFFFFFL
        private const val HASHES_PER_DIFF1_SHARE = 4_294_967_296.0 // 2^32

        /**
         * Calcule le hashrate réel (H/s) exclusivement à partir des parts acceptées dans la fenêtre donnée.
         * Retourne strictement 0.0 si aucune part acceptée n'est présente dans la fenêtre.
         */
        fun calculateHashrateHps(
            shares: List<AcceptedShareRecord>,
            windowMs: Long,
            nowMs: Long,
            sessionStartMs: Long
        ): Double {
            val cutoff = nowMs - windowMs
            val inWindow = shares.filter { it.timestampMs in cutoff..nowMs }
            if (inWindow.isEmpty()) return 0.0

            val totalWorkHashes = inWindow.sumOf { it.difficulty * HASHES_PER_DIFF1_SHARE }
            val effectiveWindowMs = if (sessionStartMs > 0L) {
                (nowMs - sessionStartMs).coerceIn(1_000L, windowMs)
            } else {
                windowMs
            }
            return totalWorkHashes / (effectiveWindowMs / 1000.0)
        }

        fun formatHashrate(hps: Double): String {
            return when {
                hps <= 0.0 -> "0.00 H/s"
                hps < 1_000.0 -> "${String.format(Locale.US, "%.2f", hps)} H/s"
                hps < 1_000_000.0 -> "${String.format(Locale.US, "%.2f", hps / 1_000.0)} kH/s"
                hps < 1_000_000_000.0 -> "${String.format(Locale.US, "%.2f", hps / 1_000_000.0)} MH/s"
                hps < 1_000_000_000_000.0 -> "${String.format(Locale.US, "%.2f", hps / 1_000_000_000.0)} GH/s"
                else -> "${String.format(Locale.US, "%.2f", hps / 1_000_000_000_000.0)} TH/s"
            }
        }
    }
}
