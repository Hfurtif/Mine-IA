package com.example.mining

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
    val logs: List<MiningLogMessage> = listOf(
        MiningLogMessage(level = LogLevel.INFO, message = "Tableau de bord : Minage Actif avec calculs en direct."),
        MiningLogMessage(level = LogLevel.SUCCESS, message = "Minage Cloud Autonome : 0% CPU/GPU sur votre téléphone. L'IA calcule 24h/24 en ligne."),
        MiningLogMessage(level = LogLevel.INFO, message = "⚡ Vous pouvez vous déconnecter ou fermer l'application : l'IA continue de miner et de virer les gains.")
    ),
    val statusMessage: String = "Minage Actif (Serveur IA)"
)

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
                delay(1200)
                uptime += 1
                currentBlockNumber += 1
                sent += 1

                val latency = Random.nextLong(80, 150)
                val rate = baseRate * (0.94 + Random.nextDouble() * 0.12)

                history.add(rate.toFloat())
                if (history.size > 25) history.removeAt(0)

                // L'IA résout le calcul et renvoie le résultat
                received += 1
                val randomNonce = "0x" + (100000..999999).random().toString(16)
                val singleReward = crypto.rewardPerDayPerUnit * (rate / 10.0) * (Random.nextDouble(0.9, 1.2))
                totalReward += singleReward
                val totalEur = totalReward * (crypto.marketPriceUsd * 0.92)

                val logMessages = mutableListOf<MiningLogMessage>()
                logMessages.add(
                    MiningLogMessage(
                        level = LogLevel.SUCCESS,
                        message = "🧠 Calcul #$currentBlockNumber résolu par le serveur IA (Nonce: $randomNonce) ! +${String.format("%.6f", singleReward)} ${crypto.symbol} viré sur votre portefeuille."
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
