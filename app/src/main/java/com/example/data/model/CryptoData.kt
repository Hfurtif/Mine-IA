package com.example.data.model

data class CryptoCurrency(
    val id: String,
    val name: String,
    val symbol: String,
    val algorithm: String,
    val recommendedMiner: String,
    val minerVersion: String,
    val officialRepoUrl: String,
    val officialSha256Checksum: String,
    val recommendedPool: String,
    val poolPort: Int,
    val alternativePools: List<String>,
    val hardwareType: String, // CPU, GPU, ASIC
    val unit: String, // H/s, KH/s, MH/s, GH/s
    val baseHashrate: Double,
    val marketPriceUsd: Double,
    val rewardPerDayPerUnit: Double,
    val accentColorHex: Long,
    val addressPrefixHint: String,
    val descriptionFr: String
)

enum class MiningStatus(val labelFr: String) {
    IDLE("En attente"),
    CONFIGURING("Connexion au service d'IA"),
    DOWNLOADING("Initialisation du pipeline IA"),
    VERIFYING("Validation des paramètres de calcul"),
    CONNECTING("Synchronisation avec le pool"),
    MINING("Calculs de minage par IA en cours"),
    PAUSED("Minage en pause"),
    OVERHEAT_PROTECTION("Arrêt d'urgence thermique"),
    ERROR("Erreur rencontrée")
}

enum class MiningTarget(val labelFr: String) {
    AI_COMPUTE("Calcul IA Déporté (Cloud AI Engine)")
}

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR
}

data class MiningLogMessage(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

data class BitcoinMiningTransaction(
    val id: String,
    val timestamp: Long,
    val amountBtc: Double,
    val blockNumber: Long,
    val isSimulated: Boolean = true
)

object SupportedCryptos {
    val BITCOIN = CryptoCurrency(
        id = "bitcoin",
        name = "Bitcoin",
        symbol = "BTC",
        algorithm = "SHA-256",
        recommendedMiner = "AI Hash Optimizer",
        minerVersion = "v2.4",
        officialRepoUrl = "https://github.com/bitcoin/bitcoin",
        officialSha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        recommendedPool = "btc.f2pool.com",
        poolPort = 3333,
        alternativePools = listOf("stratum.slushpool.com:3333", "pool.binance.com:3333"),
        hardwareType = "Serveur Cloud IA",
        unit = "TH/s",
        baseHashrate = 145.0,
        marketPriceUsd = 88400.0,
        rewardPerDayPerUnit = 0.00000000042,
        accentColorHex = 0xFFF7931A,
        addressPrefixHint = "Commence par 1, 3 ou bc1 (ex: bc1q...)",
        descriptionFr = "Minage cloud Bitcoin (BTC) 100% déporté sur serveur IA, optimisé pour SHA-256."
    )

    val ALL: List<CryptoCurrency> = listOf(BITCOIN)

    fun findById(id: String): CryptoCurrency = BITCOIN
}
