package com.minerrelay.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minerrelay.app.ai.AiMessage
import com.minerrelay.app.ai.ApiKeyHealth
import com.minerrelay.app.ai.ApiKeyState
import com.minerrelay.app.compute.ComputeHealthResult
import com.minerrelay.app.data.local.AppDatabase
import com.minerrelay.app.data.local.WalletEntity
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.data.repository.MiningRepository
import com.minerrelay.app.mining.AddressValidationResult
import com.minerrelay.app.mining.MiningTelemetry
import com.minerrelay.app.mining.SecurityValidator
import com.minerrelay.app.security.EncryptedKeyStore
import com.minerrelay.app.service.MiningRelayService
import com.minerrelay.app.service.RelayCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class MiningViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val preferences = RelayCoordinator.getPreferences(application)
    val repository = MiningRepository(db.walletDao(), preferences)
    val engine = RelayCoordinator.getEngine(application)
    private val aiAdvisor = RelayCoordinator.aiAdvisor

    val telemetry: StateFlow<MiningTelemetry> = engine.telemetry

    val wallets: StateFlow<List<WalletEntity>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _relayConfig = MutableStateFlow(preferences.loadRelayConfig())
    val relayConfig: StateFlow<RelayConfig> = _relayConfig.asStateFlow()

    private val _computeApiKeyInput = MutableStateFlow(preferences.getComputeApiKey())
    val computeApiKeyInput: StateFlow<String> = _computeApiKeyInput.asStateFlow()

    private val _geminiApiKeyInput = MutableStateFlow(preferences.getGeminiApiKey())
    val geminiApiKeyInput: StateFlow<String> = _geminiApiKeyInput.asStateFlow()

    private val _computeKeyHealth = MutableStateFlow<ComputeHealthResult?>(null)
    val computeKeyHealth: StateFlow<ComputeHealthResult?> = _computeKeyHealth.asStateFlow()

    private val _geminiKeyHealth = MutableStateFlow(ApiKeyHealth())
    val geminiKeyHealth: StateFlow<ApiKeyHealth> = _geminiKeyHealth.asStateFlow()

    private val _isCheckingComputeKey = MutableStateFlow(false)
    val isCheckingComputeKey: StateFlow<Boolean> = _isCheckingComputeKey.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _aiMessages = MutableStateFlow(
        listOf(
            AiMessage(
                isUser = false,
                text = "Assistant analytique prêt. Je peux expliquer les chiffres réels observés par le relais (parts acceptées/rejetées, hashrate mesuré 15 min / 24 h, latence, coût vs paiements). Je ne mine pas et je ne verse aucun gain."
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        val currentAddr = preferences.btcAddress
        if (currentAddr.isNotBlank()) {
            engine.refreshBlockchainTransactions(currentAddr)
        }
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    fun saveSettings(
        poolHost: String,
        poolPortText: String,
        workerName: String,
        btcAddress: String,
        computeApiUrl: String,
        computeApiKey: String,
        geminiApiKey: String,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        testMode: Boolean
    ): Boolean {
        val cleanHost = poolHost.trim()
        val cleanPortText = poolPortText.trim()
        val parsedPort = cleanPortText.toIntOrNull()
        val cleanAddress = btcAddress.trim()

        if (cleanHost.isEmpty() || parsedPort == null || parsedPort !in 1..65535) {
            showNotification("Veuillez saisir un hôte et un port Stratum valides (1..65535).", isError = true)
            return false
        }

        when (val addrResult = SecurityValidator.validateBitcoinMainnetAddress(cleanAddress)) {
            is AddressValidationResult.DangerousSeedPhraseDetected -> {
                showNotification(addrResult.message, isError = true)
                return false
            }
            is AddressValidationResult.DangerousPrivateKeyDetected -> {
                showNotification(addrResult.message, isError = true)
                return false
            }
            is AddressValidationResult.InvalidFormat -> {
                showNotification(addrResult.reason, isError = true)
                return false
            }
            AddressValidationResult.Valid -> Unit
        }

        preferences.saveRelaySettings(
            poolHost = cleanHost,
            poolPortText = cleanPortText,
            workerName = workerName.trim().ifEmpty { "worker1" },
            btcAddress = cleanAddress,
            computeApiUrl = computeApiUrl.trim(),
            computeApiKey = computeApiKey.trim(),
            geminiApiKey = geminiApiKey.trim(),
            wifiOnly = wifiOnly,
            chargingOnly = chargingOnly,
            testMode = testMode
        )

        _relayConfig.value = preferences.loadRelayConfig()
        _computeApiKeyInput.value = preferences.getComputeApiKey()
        _geminiApiKeyInput.value = preferences.getGeminiApiKey()
        engine.refreshBlockchainTransactions(cleanAddress)
        showNotification("Configuration sauvegardée et clés chiffrées via AndroidKeyStore.")
        return true
    }

    fun startForegroundRelay(testModeOverride: Boolean? = null) {
        val cfg = preferences.loadRelayConfig().let {
            if (testModeOverride != null) {
                preferences.testMode = testModeOverride
                it.copy(testMode = testModeOverride)
            } else {
                it
            }
        }
        _relayConfig.value = cfg

        if (!cfg.isPoolConfigured) {
            showNotification("Configurez d'abord l'hôte du pool, le port et votre adresse BTC dans Réglages.", isError = true)
            return
        }
        if (SecurityValidator.validateBitcoinMainnetAddress(cfg.btcAddress) !is AddressValidationResult.Valid) {
            showNotification("Adresse Bitcoin invalide. Vérifiez-la dans Réglages.", isError = true)
            return
        }

        MiningRelayService.start(getApplication())
        showNotification(
            if (cfg.testMode) "Mode test démarré (faible difficulté) en avant-plan." else "Relais Stratum v1 démarré en avant-plan."
        )
    }

    fun stopForegroundRelay() {
        MiningRelayService.stop(getApplication())
        showNotification("Relais Stratum arrêté.")
    }

    fun testComputeApiHealth(customKey: String, customEndpoint: String) {
        val cleanKey = customKey.trim()
        val cleanEndpoint = customEndpoint.trim()
        preferences.computeApiUrl = cleanEndpoint
        preferences.saveComputeApiKey(cleanKey)
        _computeApiKeyInput.value = cleanKey
        _relayConfig.value = preferences.loadRelayConfig()

        viewModelScope.launch {
            _isCheckingComputeKey.value = true
            val result = RelayCoordinator.getComputeClient(getApplication()).checkHealth(cleanKey)
            _computeKeyHealth.value = result
            _isCheckingComputeKey.value = false
            when (result) {
                is ComputeHealthResult.Healthy -> showNotification("Service de calcul joignable : ${result.detail}")
                is ComputeHealthResult.IncompleteConfiguration -> showNotification(result.reason, isError = true)
                is ComputeHealthResult.Unhealthy -> showNotification("Alerte service de calcul : ${result.reason}", isError = true)
            }
        }
    }

    fun testGeminiKey(key: String) {
        val clean = key.trim()
        preferences.saveGeminiApiKey(clean)
        _geminiApiKeyInput.value = clean
        viewModelScope.launch {
            _geminiKeyHealth.value = ApiKeyHealth(ApiKeyState.CHECKING, "Vérification...")
            val health = aiAdvisor.verifyGeminiKey(clean)
            _geminiKeyHealth.value = health
            showNotification(
                message = health.message ?: "Statut inconnu",
                isError = health.state == ApiKeyState.UNRESPONSIVE
            )
        }
    }

    fun refreshMempoolTransactions() {
        val address = _relayConfig.value.btcAddress
        if (address.isBlank()) {
            showNotification("Saisissez une adresse Bitcoin dans Réglages pour interroger mempool.space.", isError = true)
            return
        }
        engine.refreshBlockchainTransactions(address)
    }

    fun clearLogs() = engine.clearLogs()

    fun addWallet(name: String, publicAddress: String): AddressValidationResult {
        val validation = SecurityValidator.validateBitcoinMainnetAddress(publicAddress)
        if (validation !is AddressValidationResult.Valid) return validation

        val cleanAddr = publicAddress.trim()
        viewModelScope.launch {
            repository.saveWallet(
                WalletEntity(
                    name = name.ifBlank { "Portefeuille BTC" },
                    cryptoSymbol = "BTC",
                    publicAddress = cleanAddr,
                    isDefault = true
                )
            )
        }
        return AddressValidationResult.Valid
    }

    fun useWalletForRelay(wallet: WalletEntity) {
        preferences.btcAddress = wallet.publicAddress
        _relayConfig.value = preferences.loadRelayConfig()
        engine.refreshBlockchainTransactions(wallet.publicAddress)
        showNotification("Adresse ${EncryptedKeyStore.maskAddress(wallet.publicAddress)} sélectionnée pour le relais.")
    }

    fun deleteWallet(wallet: WalletEntity) {
        viewModelScope.launch { repository.deleteWallet(wallet) }
    }

    fun sendAiPrompt(userPrompt: String) {
        val clean = userPrompt.trim()
        if (clean.isEmpty() || _isAiLoading.value) return

        _aiMessages.update { it + AiMessage(isUser = true, text = clean) }
        _isAiLoading.value = true

        viewModelScope.launch {
            val geminiKey = preferences.getGeminiApiKey()
            val replyResult = aiAdvisor.explainTelemetry(clean, geminiKey, telemetry.value)
            val replyText = replyResult.getOrElse { err ->
                err.message ?: "Impossible d'interroger Gemini."
            }
            if (replyResult.isFailure) {
                showNotification(replyText, isError = true)
            }
            _aiMessages.update { it + AiMessage(isUser = false, text = replyText) }
            _isAiLoading.value = false
        }
    }
}
