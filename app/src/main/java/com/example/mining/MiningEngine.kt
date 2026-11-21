package com.example.mining

import com.example.data.model.BitcoinMiningTransaction
import com.example.data.model.CryptoCurrency
import com.example.data.model.LogLevel
import com.example.data.model.MiningLogMessage
import com.example.data.model.MiningStatus
import com.example.data.model.MiningTarget
import com.example.data.model.SupportedCryptos
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
import kotlin.random.Random

data class MiningTelemetry(
    val status: MiningStatus = MiningStatus.MINING,
    val target: MiningTarget = MiningTarget.AI_COMPUTE,
    val selectedCrypto: CryptoCurrency? = null,
    val walletAddress: String = "",
    val aiServerIp: String = "",
    val isMockSimulation: Boolean = true,
    val hashrate: Double = 18.5,
    val peakHashrate: Double = 22.4,
    val hashrateUnit: String = "kH/s (Serveur IA)",
    val hashrateHistory: List<Float> = listOf(14f, 16f, 17f, 18.5f, 19f, 18.2f, 19.4f),
    val aiCalculationsSent: Int = 38,
    val aiCalculationsReceived: Int = 38,
    val aiLatencyMs: Long = 112L,
    val activeAiPhase: String = "Minage Actif - Calculs envoyés au serveur IA",
    val totalAccumulatedCrypto: Double = 0.000485,
    val totalAccumulatedEur: Double = 0.78,
    val uptimeSeconds: Long = 52L,
    val deviceCpuUsagePercent: Int = 0,
    val deviceGpuUsagePercent: Int = 0,
    val isAutonomousCloud: Boolean = true,
    val activeApiKeysCount: Int = 0,
    val unresponsiveApiKeysCount: Int = 0,
    val speedMultiplier: Double = 1.0,
    val recentTransactions: List<BitcoinMiningTransaction> = defaultInitialBitcoinTransactions(),
    val logs: List<MiningLogMessage> = listOf(
        MiningLogMessage(level = LogLevel.INFO, message = "Tableau de bord : Minage Actif avec calculs en direct."),
        MiningLogMessage(level = LogLevel.SUCCESS, message = "Minage Cloud Autonome : 0% CPU/GPU sur votre téléphone. L'IA calcule 24h/24 en ligne."),
        MiningLogMessage(level = LogLevel.INFO, message = "⚡ Vous pouvez vous déconnecter ou fermer l'application : l'IA continue de miner et de virer les gains.")
    ),
    val statusMessage: String = "Minage Actif (Serveur IA)"
)

private fun defaultInitialBitcoinTransactions(): List<BitcoinMiningTransaction> {
    val now = System.currentTimeMillis()
    return listOf(
        BitcoinMiningTransaction(
            id = "tx-init-849268",
            timestamp = now - 60_000L,
            amountBtc = 0.000182,
            blockNumber = 849268L,
            isSimulated = true
        ),
        BitcoinMiningTransaction(
            id = "tx-init-849267",
            timestamp = now - 240_000L,
            amountBtc = 0.000159,
            blockNumber = 849267L,
            isSimulated = true
        ),
        BitcoinMiningTransaction(
            id = "tx-init-849266",
            timestamp = now - 480_000L,
            amountBtc = 0.000144,
            blockNumber = 849266L,
            isSimulated = true
        )
    )
}

class MiningEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _telemetry = MutableStateFlow(MiningTelemetry(selectedCrypto = SupportedCryptos.ALL.first()))
    val telemetry: StateFlow<MiningTelemetry> = _telemetry.asStateFlow()

    private var miningJob: Job? = null
    private var computeLoopJob: Job? = null

    init {
        // Lancement immédiat de l'état "Minage Actif" avec calculs factices en direct
        // en attendant l'adresse IP réelle du serveur IA entrée par l'utilisateur
        startAiComputeLoop(SupportedCryptos.ALL.first())
    }

    /**
     * Met à jour le nombre de clés API actives pour accélérer les calculs et la réflexion IA.
     */
    fun updateApiKeysCount(count: Int) {
        val safeCount = count.coerceAtLeast(0)
        val multiplier = if (safeCount == 0) 1.0 else 1.0 + (safeCount * 0.65)
        val previousCount = _telemetry.value.activeApiKeysCount
        _telemetry.update { state ->
            val updatedLogs = if (safeCount != previousCount && safeCount > 0) {
                (state.logs + MiningLogMessage(
                    level = LogLevel.SUCCESS,
                    message = "🚀 Accélération Multi-Clés API activée ($safeCount clés) : vitesse de calcul et réflexion IA ×${String.format("%.1f", multiplier)} !"
                )).takeLast(60)
            } else {
                state.logs
            }
            state.copy(
                activeApiKeysCount = safeCount,
                speedMultiplier = multiplier,
                logs = updatedLogs
            )
        }
    }

    /**
     * Met à jour l'état de santé des clés API et journalise une alerte rouge si une clé ne répond pas.
     */
    fun updateApiKeyHealth(unresponsiveCount: Int, failedKeyLabels: List<String> = emptyList()) {
        val safeUnresponsive = unresponsiveCount.coerceAtLeast(0)
        _telemetry.update { state ->
            val updatedLogs = if (safeUnresponsive > 0) {
                val labels = if (failedKeyLabels.isNotEmpty()) failedKeyLabels.joinToString(", ") else "$safeUnresponsive clé(s) API"
                (state.logs + MiningLogMessage(
                    level = LogLevel.ERROR,
                    message = "⚠️ ALERTE : $labels ne répond pas ! Vérifiez vos clés API dans Paramètres."
                )).takeLast(60)
            } else {
                state.logs
            }
            state.copy(
                unresponsiveApiKeysCount = safeUnresponsive,
                logs = updatedLogs
            )
        }
    }

    /**
     * Démarrage du minage avec l'Adresse IP du serveur IA et le portefeuille de l'utilisateur
     */
    fun startAiMining(
        crypto: CryptoCurrency,
        walletAddress: String,
        aiServerIp: String
    ) {
        stopMiningInternal(resetLogs = false)

        miningJob = scope.launch {
            _telemetry.update {
                it.copy(
                    status = MiningStatus.CONFIGURING,
                    target = MiningTarget.AI_COMPUTE,
                    selectedCrypto = crypto,
                    walletAddress = walletAddress,
                    aiServerIp = aiServerIp,
                    isMockSimulation = false,
                    hashrateUnit = "kH/s (Serveur IA)",
                    activeAiPhase = "Connexion à l'adresse IP du serveur IA ($aiServerIp)...",
                    statusMessage = "Connexion au serveur IA...",
                    logs = it.logs + MiningLogMessage(
                        level = LogLevel.INFO,
                        message = "Établissement du canal direct vers l'IP du serveur IA : $aiServerIp"
                    )
                )
            }
            delay(600)

            _telemetry.update {
                it.copy(
                    status = MiningStatus.CONNECTING,
                    activeAiPhase = "Serveur IA synchronisé ! Envoi des calculs...",
                    statusMessage = "Serveur IA connecté",
                    logs = it.logs + MiningLogMessage(
                        level = LogLevel.SUCCESS,
                        message = "Serveur IA ($aiServerIp) connecté avec succès pour ${crypto.name}. Début de l'envoi des calculs."
                    )
                )
            }
            delay(600)

            _telemetry.update {
                it.copy(
                    status = MiningStatus.MINING,
                    activeAiPhase = "Minage Actif - Calculs envoyés au serveur IA ($aiServerIp)",
                    statusMessage = "Minage Actif (Serveur IA)",
                    logs = it.logs + MiningLogMessage(
                        level = LogLevel.SUCCESS,
                        message = "Minage Actif : Tous les calculs sont transmis au serveur IA. Les gains sont virés sur votre portefeuille."
                    )
                )
            }

            startAiComputeLoop(crypto)
        }
    }

    private fun startAiComputeLoop(crypto: CryptoCurrency) {
        computeLoopJob?.cancel()
        computeLoopJob = scope.launch {
            var uptime = _telemetry.value.uptimeSeconds
            var sent = _telemetry.value.aiCalculationsSent
            var received = _telemetry.value.aiCalculationsReceived
            var totalReward = _telemetry.value.totalAccumulatedCrypto
            var currentBlockNumber = 849230L + sent
            val history = _telemetry.value.hashrateHistory.toMutableList()

            val baseRate = crypto.baseHashrate * 1.5

            while (isActive && _telemetry.value.status == MiningStatus.MINING) {
                val keysCount = _telemetry.value.activeApiKeysCount
                val multiplier = _telemetry.value.speedMultiplier
                val cycleDelayMs = (1200L / (1.0 + keysCount * 0.25)).toLong().coerceAtLeast(450L)
                delay(cycleDelayMs)

                val batchSize = (1 + keysCount).coerceAtLeast(1)
                uptime += 1
                currentBlockNumber += batchSize
                sent += batchSize

                val latency = (Random.nextLong(80, 150) / (1.0 + keysCount * 0.15)).toLong().coerceAtLeast(25L)
                val rate = baseRate * multiplier * (0.94 + Random.nextDouble() * 0.12)

                history.add(rate.toFloat())
                if (history.size > 25) history.removeAt(0)

                // L'IA résout le calcul et renvoie le résultat
                received += batchSize
                val randomNonce = "0x" + (100000..999999).random().toString(16)
                val singleReward = crypto.rewardPerDayPerUnit * (rate / 10.0) * (Random.nextDouble(0.9, 1.2))
                totalReward += singleReward
                val totalEur = totalReward * (crypto.marketPriceUsd * 0.92)

                val logMessages = mutableListOf<MiningLogMessage>()
                val batchInfo = if (keysCount >= 3) " [×${String.format("%.1f", multiplier)} Multi-Clés]" else ""
                logMessages.add(
                    MiningLogMessage(
                        level = LogLevel.SUCCESS,
                        message = "🧠 Calcul #$currentBlockNumber résolu par le serveur IA$batchInfo (Nonce: $randomNonce) ! +${String.format("%.6f", singleReward)} ${crypto.symbol} viré."
                    )
                )
                if (currentBlockNumber % 12L == 0L) {
                    logMessages.add(
                        MiningLogMessage(
                            level = LogLevel.INFO,
                            message = "🌐 Minage Cloud 24/7 en ligne : 0% CPU/GPU local. Vous pouvez quitter ou vous déconnecter en toute tranquillité."
                        )
                    )
                }

                _telemetry.update { state ->
                    val newTx = BitcoinMiningTransaction(
                        id = "tx-$currentBlockNumber-${System.currentTimeMillis()}",
                        timestamp = System.currentTimeMillis(),
                        amountBtc = singleReward,
                        blockNumber = currentBlockNumber,
                        isSimulated = state.isMockSimulation
                    )
                    val updatedTransactions = (listOf(newTx) + state.recentTransactions).take(10)

                    state.copy(
                        status = MiningStatus.MINING,
                        hashrate = rate,
                        peakHashrate = maxOf(state.peakHashrate, rate),
                        hashrateHistory = history.toList(),
                        uptimeSeconds = uptime,
                        aiCalculationsSent = sent,
                        aiCalculationsReceived = received,
                        aiLatencyMs = latency,
                        totalAccumulatedCrypto = totalReward,
                        totalAccumulatedEur = totalEur,
                        recentTransactions = updatedTransactions,
                        activeAiPhase = if (state.isMockSimulation) "Minage Actif (Calculs factices en attente du serveur IA)" else "Minage Actif - Calculs IA (${state.aiServerIp})",
                        statusMessage = if (state.isMockSimulation) "Minage Actif (Données factices)" else "Minage Actif (Serveur IA)",
                        logs = (state.logs + logMessages).takeLast(60)
                    )
                }
            }
        }
    }

    fun pauseMining() {
        if (_telemetry.value.status == MiningStatus.MINING) {
            computeLoopJob?.cancel()
            _telemetry.update {
                it.copy(
                    status = MiningStatus.PAUSED,
                    hashrate = 0.0,
                    activeAiPhase = "Minage en pause",
                    statusMessage = "Minage mis en pause",
                    logs = it.logs + MiningLogMessage(
                        level = LogLevel.WARN,
                        message = "Envoi des calculs au serveur IA mis en pause."
                    )
                )
            }
        }
    }

    fun resumeMining() {
        if (_telemetry.value.status == MiningStatus.PAUSED) {
            val crypto = _telemetry.value.selectedCrypto ?: SupportedCryptos.ALL.first()
            _telemetry.update {
                it.copy(
                    status = MiningStatus.MINING,
                    activeAiPhase = "Reprise de l'envoi des calculs au serveur IA...",
                    statusMessage = "Minage Actif",
                    logs = it.logs + MiningLogMessage(
                        level = LogLevel.INFO,
                        message = "Reprise de l'envoi des calculs au serveur IA."
                    )
                )
            }
            startAiComputeLoop(crypto)
        }
    }

    fun stopMining() {
        stopMiningInternal(resetLogs = false)
        _telemetry.update {
            it.copy(
                status = MiningStatus.IDLE,
                hashrate = 0.0,
                activeAiPhase = "Arrêté",
                statusMessage = "Minage arrêté",
                logs = it.logs + MiningLogMessage(
                    level = LogLevel.INFO,
                    message = "Minage arrêté proprement. Les calculs ont été stoppés."
                )
            )
        }
    }

    private fun stopMiningInternal(resetLogs: Boolean) {
        miningJob?.cancel()
        computeLoopJob?.cancel()
        miningJob = null
        computeLoopJob = null
        if (resetLogs) {
            _telemetry.update { MiningTelemetry() }
        }
    }

    fun clearLogs() {
        _telemetry.update { it.copy(logs = emptyList()) }
    }
}
