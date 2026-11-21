package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiMessage
import com.example.ai.ApiKeyHealth
import com.example.ai.ApiKeyState
import com.example.ai.MiningAiAdvisor
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.WalletEntity
import com.example.data.model.CryptoCurrency
import com.example.data.model.SupportedCryptos
import com.example.data.repository.MiningRepository
import com.example.mining.AddressValidationResult
import com.example.mining.MiningEngine
import com.example.mining.MiningTelemetry
import com.example.mining.SecurityValidator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    val preferences = AppPreferences(application)
    val repository = MiningRepository(db.walletDao(), db.remoteRigDao(), preferences)
    val engine = MiningEngine(viewModelScope)
    val aiAdvisor = MiningAiAdvisor()

    val telemetry: StateFlow<MiningTelemetry> = engine.telemetry

    val wallets: StateFlow<List<WalletEntity>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCrypto = MutableStateFlow<CryptoCurrency>(SupportedCryptos.BITCOIN)
    val selectedCrypto: StateFlow<CryptoCurrency> = _selectedCrypto.asStateFlow()

    // Saisie directe de l'utilisateur : Adresse Wallet Bitcoin et Adresse IP du Serveur IA
    val walletAddressInput = MutableStateFlow("")
    val aiServerIpInput = MutableStateFlow("")

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                isUser = false,
                text = "Bonjour ! Je suis l'Assistant Miner IA spécialisé Bitcoin (BTC). Entrez votre adresse Bitcoin et l'adresse IP de votre serveur IA : l'application transmet immédiatement tous les calculs SHA-256 au serveur IA et vire vos gains BTC sur votre wallet."
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _apiKeySlots = MutableStateFlow(listOf("", "", ""))
    val apiKeySlots: StateFlow<List<String>> = _apiKeySlots.asStateFlow()

    private val _apiKeyHealth = MutableStateFlow<Map<String, ApiKeyHealth>>(emptyMap())
    val apiKeyHealth: StateFlow<Map<String, ApiKeyHealth>> = _apiKeyHealth.asStateFlow()

    private val _isCheckingApiKeys = MutableStateFlow(false)
    val isCheckingApiKeys: StateFlow<Boolean> = _isCheckingApiKeys.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        _selectedCrypto.value = SupportedCryptos.BITCOIN
        preferences.lastSelectedCryptoId = SupportedCryptos.BITCOIN.id

        // L'adresse wallet commence strictement vide : c'est à l'utilisateur de l'ajouter
        walletAddressInput.value = ""
        aiServerIpInput.value = ""

        val savedKeys = preferences.getAiApiKeySlots(minSlots = 3)
        _apiKeySlots.value = savedKeys
        engine.updateApiKeysCount(preferences.getActiveAiApiKeys().size)
    }

    fun selectCrypto(crypto: CryptoCurrency) {
        _selectedCrypto.value = SupportedCryptos.BITCOIN
        preferences.lastSelectedCryptoId = SupportedCryptos.BITCOIN.id
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    /**
     * Démarrage du minage avec l'Adresse Wallet et l'IP du Serveur IA
     */
    fun startDirectAiMining(address: String, serverIp: String) {
        val crypto = _selectedCrypto.value
        val trimmedAddress = address.trim()
        val ipToUse = serverIp.trim().ifEmpty { "ia-node-1.network:8080" }

        if (trimmedAddress.isEmpty()) {
            showNotification("Veuillez saisir votre adresse wallet pour recevoir les gains.", true)
            return
        }

        when (val validation = SecurityValidator.validatePublicAddress(crypto.symbol, trimmedAddress)) {
            is AddressValidationResult.DangerousSeedPhraseDetected -> {
                showNotification(validation.message, true)
                return
            }
            is AddressValidationResult.DangerousPrivateKeyDetected -> {
                showNotification(validation.message, true)
                return
            }
            is AddressValidationResult.InvalidFormat -> {
                showNotification("Format d'adresse incorrect : ${validation.reason}", true)
                return
            }
            AddressValidationResult.Valid -> {
                // Valide
            }
        }

        viewModelScope.launch {
            repository.saveWallet(
                WalletEntity(
                    name = "Portefeuille ${crypto.symbol}",
                    cryptoSymbol = crypto.symbol,
                    publicAddress = trimmedAddress,
                    isDefault = false
                )
            )
        }

        engine.startAiMining(
            crypto = crypto,
            walletAddress = trimmedAddress,
            aiServerIp = ipToUse
        )
    }

    fun pauseMining() = engine.pauseMining()
    fun resumeMining() = engine.resumeMining()
    fun stopMining() = engine.stopMining()
    fun clearLogs() = engine.clearLogs()

    fun updateAiServerIp(ip: String) {
        aiServerIpInput.value = ip.trim()
    }

    fun saveApiKeys(keys: List<String>) {
        preferences.saveAiApiKeys(keys)
        val updatedSlots = preferences.getAiApiKeySlots(minSlots = 3)
        _apiKeySlots.value = updatedSlots
        val activeKeys = preferences.getActiveAiApiKeys()
        val activeCount = activeKeys.size
        engine.updateApiKeysCount(activeCount)

        // Nettoyer les états de santé des clés supprimées
        _apiKeyHealth.update { current ->
            current.filterKeys { it in activeKeys }
        }

        if (activeCount >= 3) {
            val multiplier = 1.0 + (activeCount * 0.65)
            showNotification(
                "$activeCount clés API enregistrées ! Vitesse de calcul et réflexion boostées (×${String.format("%.1f", multiplier)})."
            )
        } else if (activeCount > 0) {
            showNotification(
                "$activeCount clé(s) enregistrée(s). Ajoutez au moins 3 clés API pour débloquer la vitesse maximale !"
            )
        } else {
            engine.updateApiKeyHealth(0)
            showNotification("Clés API effacées.")
        }

        if (activeCount > 0) {
            verifySavedApiKeys(notifyOnSuccess = false)
        }
    }

    fun verifySavedApiKeys(notifyOnSuccess: Boolean = true) {
        val slots = _apiKeySlots.value
        val activeKeys = slots.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (activeKeys.isEmpty()) {
            engine.updateApiKeyHealth(0)
            if (notifyOnSuccess) {
                showNotification("Aucune clé API à vérifier.", true)
            }
            return
        }

        viewModelScope.launch {
            _isCheckingApiKeys.value = true
            _apiKeyHealth.update { current ->
                current + activeKeys.associateWith {
                    ApiKeyHealth(ApiKeyState.CHECKING, "Vérification en cours...")
                }
            }

            val results = activeKeys.map { key ->
                async { key to aiAdvisor.verifyApiKey(key) }
            }.awaitAll().toMap()

            _apiKeyHealth.update { current -> current + results }
            _isCheckingApiKeys.value = false

            val failedLabels = slots.mapIndexedNotNull { index, value ->
                val trimmed = value.trim()
                if (trimmed.isNotBlank() && results[trimmed]?.state == ApiKeyState.UNRESPONSIVE) {
                    "Clé API #${index + 1}"
                } else {
                    null
                }
            }

            engine.updateApiKeyHealth(failedLabels.size, failedLabels)

            if (failedLabels.isNotEmpty()) {
                showNotification(
                    "⚠️ Alerte : ${failedLabels.joinToString(", ")} ne répond pas !",
                    isError = true
                )
            } else if (notifyOnSuccess) {
                showNotification("✅ Toutes les clés API (${activeKeys.size}) répondent normalement.")
            }
        }
    }

    fun addWallet(name: String, cryptoSymbol: String, publicAddress: String, isDefault: Boolean): AddressValidationResult {
        val btcSymbol = SupportedCryptos.BITCOIN.symbol
        val validation = SecurityValidator.validatePublicAddress(btcSymbol, publicAddress)
        if (validation !is AddressValidationResult.Valid) {
            return validation
        }

        viewModelScope.launch {
            repository.saveWallet(
                WalletEntity(
                    name = name.ifBlank { "Wallet Bitcoin" },
                    cryptoSymbol = btcSymbol,
                    publicAddress = publicAddress.trim(),
                    isDefault = isDefault
                )
            )
            walletAddressInput.value = publicAddress.trim()
        }
        return AddressValidationResult.Valid
    }

    fun deleteWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }

    fun sendAiPrompt(userPrompt: String) {
        if (userPrompt.isBlank() || _isAiLoading.value) return

        val userMsg = AiMessage(isUser = true, text = userPrompt.trim())
        _aiMessages.update { it + userMsg }
        _isAiLoading.value = true

        viewModelScope.launch {
            val activeKeys = preferences.getActiveAiApiKeys()
            val reply = aiAdvisor.askAdvisorWithHealth(
                prompt = userPrompt,
                customApiKeys = activeKeys,
                currentCrypto = _selectedCrypto.value.name,
                currentHashrate = "${String.format("%.1f", telemetry.value.hashrate)} ${telemetry.value.hashrateUnit}"
            )

            if (reply.keyResults.isNotEmpty()) {
                _apiKeyHealth.update { current -> current + reply.keyResults }
            }

            val slots = _apiKeySlots.value
            val failedLabels = slots.mapIndexedNotNull { index, value ->
                val trimmed = value.trim()
                if (trimmed.isNotBlank() && _apiKeyHealth.value[trimmed]?.state == ApiKeyState.UNRESPONSIVE) {
                    "Clé API #${index + 1}"
                } else {
                    null
                }
            }
            engine.updateApiKeyHealth(failedLabels.size, failedLabels)

            if (reply.failedKeys.isNotEmpty()) {
                val newlyFailedLabels = slots.mapIndexedNotNull { index, value ->
                    if (value.trim() in reply.failedKeys) "Clé API #${index + 1}" else null
                }
                val labelText = if (newlyFailedLabels.isNotEmpty()) newlyFailedLabels.joinToString(", ") else "Une clé API"
                showNotification("⚠️ Alerte : $labelText ne répond pas !", isError = true)
            }

            _aiMessages.update { it + AiMessage(isUser = false, text = reply.text) }
            _isAiLoading.value = false
        }
    }
}
