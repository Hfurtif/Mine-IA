package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiMessage
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

    private val _selectedCrypto = MutableStateFlow<CryptoCurrency>(SupportedCryptos.ALL.first())
    val selectedCrypto: StateFlow<CryptoCurrency> = _selectedCrypto.asStateFlow()

    // Saisie directe de l'utilisateur : Adresse Wallet et Adresse IP du Serveur IA
    val walletAddressInput = MutableStateFlow("")
    val aiServerIpInput = MutableStateFlow("")

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                isUser = false,
                text = "Bonjour ! Je suis l'Assistant Miner IA. Entrez l'adresse de votre portefeuille et l'adresse IP de votre serveur IA. L'application transmet immédiatement tous les calculs au serveur IA et vire les récompenses sur votre wallet."
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        val savedCryptoId = preferences.lastSelectedCryptoId
        val initialCrypto = SupportedCryptos.findById(savedCryptoId)
        _selectedCrypto.value = initialCrypto

        // L'adresse wallet commence strictement vide : c'est à l'utilisateur de l'ajouter
        walletAddressInput.value = ""
        aiServerIpInput.value = ""
    }

    fun selectCrypto(crypto: CryptoCurrency) {
        _selectedCrypto.value = crypto
        preferences.lastSelectedCryptoId = crypto.id
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

    fun addWallet(name: String, cryptoSymbol: String, publicAddress: String, isDefault: Boolean): AddressValidationResult {
        val validation = SecurityValidator.validatePublicAddress(cryptoSymbol, publicAddress)
        if (validation !is AddressValidationResult.Valid) {
            return validation
        }

        viewModelScope.launch {
            repository.saveWallet(
                WalletEntity(
                    name = name.ifBlank { "Wallet $cryptoSymbol" },
                    cryptoSymbol = cryptoSymbol,
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
            val answer = aiAdvisor.askAdvisor(
                prompt = userPrompt,
                customApiKey = aiServerIpInput.value,
                currentCrypto = _selectedCrypto.value.name,
                currentHashrate = "${String.format("%.1f", telemetry.value.hashrate)} ${telemetry.value.hashrateUnit}"
            )
            _aiMessages.update { it + AiMessage(isUser = false, text = answer) }
            _isAiLoading.value = false
        }
    }
}
