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
    AI_COMPUTE("Calcul IA Déporté (Cloud AI Engine)"),
    LOCAL_CPU("Machine locale (Benchmark CPU sécurisé)"),
    REMOTE_SSH("Machine distante / Rig (SSH)")
}

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR
}

data class MiningLogMessage(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

object SupportedCryptos {
    val ALL: List<CryptoCurrency> = listOf(
        CryptoCurrency(
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
            hardwareType = "Calcul IA / ASIC",
            unit = "TH/s",
            baseHashrate = 145.0,
            marketPriceUsd = 88400.0,
            rewardPerDayPerUnit = 0.00000000042,
            accentColorHex = 0xFFF7931A,
            addressPrefixHint = "Commence par 1, 3 ou bc1 (ex: bc1q...)",
            descriptionFr = "La reine des cryptomonnaies décentralisées, calculée via SHA-256 et optimisée par IA."
        ),
        CryptoCurrency(
            id = "monero",
            name = "Monero",
            symbol = "XMR",
            algorithm = "RandomX",
            recommendedMiner = "XMRig",
            minerVersion = "v6.22.2",
            officialRepoUrl = "https://github.com/xmrig/xmrig/releases",
            officialSha256Checksum = "9e5c4a317bfd6209d6f6e07a387796d8b35061695ea3be2012a9e3e78b277b09",
            recommendedPool = "pool.supportxmr.com",
            poolPort = 3333,
            alternativePools = listOf("xmr-eu1.nanopool.org:14433", "xmr.2miners.com:2222"),
            hardwareType = "CPU (optimisé Ryzen / Intel / ARM)",
            unit = "H/s",
            baseHashrate = 2450.0,
            marketPriceUsd = 162.50,
            rewardPerDayPerUnit = 0.00000015,
            accentColorHex = 0xFFFF6600,
            addressPrefixHint = "Commence par 4 ou 8 (ex: 48edfcv...)",
            descriptionFr = "Cryptomonnaie anonyme reine du minage CPU avec l'algorithme RandomX qui résiste aux ASICs."
        ),
        CryptoCurrency(
            id = "kaspa",
            name = "Kaspa",
            symbol = "KAS",
            algorithm = "kHeavyHash",
            recommendedMiner = "lolMiner",
            minerVersion = "v1.93",
            officialRepoUrl = "https://github.com/Lolliedieb/lolMiner-releases/releases",
            officialSha256Checksum = "4c71887e2b694b97ce1d451a8e1005a8b792e9e6231d68eb2a1d2dca7e8c339a",
            recommendedPool = "kas.2miners.com",
            poolPort = 2020,
            alternativePools = listOf("pool.woolypooly.com:3112", "kaspa.herominers.com:1206"),
            hardwareType = "GPU / ASIC",
            unit = "MH/s",
            baseHashrate = 380.0,
            marketPriceUsd = 0.145,
            rewardPerDayPerUnit = 0.00012,
            accentColorHex = 0xFF70C7BA,
            addressPrefixHint = "Commence par kaspa: (ex: kaspa:qq...)",
            descriptionFr = "BlockDAG ultra-rapide avec consensus Proof-of-Work haute cadence et blocs instantanés."
        ),
        CryptoCurrency(
            id = "ravencoin",
            name = "Ravencoin",
            symbol = "RVN",
            algorithm = "KawPoW",
            recommendedMiner = "T-Rex Miner",
            minerVersion = "v0.26.8",
            officialRepoUrl = "https://github.com/trexminer/T-Rex/releases",
            officialSha256Checksum = "8b9a117cb3d4b68e4c70d4f3b50dfb6c6b2b7ff920bb379ce85f2eb716d1f95a",
            recommendedPool = "rvn.2miners.com",
            poolPort = 6060,
            alternativePools = listOf("stratum-ravencoin.flypool.org:3333", "pool.woolypooly.com:5555"),
            hardwareType = "GPU (Nvidia / AMD)",
            unit = "MH/s",
            baseHashrate = 28.5,
            marketPriceUsd = 0.021,
            rewardPerDayPerUnit = 0.0085,
            accentColorHex = 0xFF384182,
            addressPrefixHint = "Commence par R (ex: RNgH...)",
            descriptionFr = "Blockchain dédiée à la création et au transfert d'actifs numériques, optimisée pour cartes graphiques."
        ),
        CryptoCurrency(
            id = "ergo",
            name = "Ergo",
            symbol = "ERG",
            algorithm = "Autolykos2",
            recommendedMiner = "lolMiner",
            minerVersion = "v1.93",
            officialRepoUrl = "https://github.com/Lolliedieb/lolMiner-releases/releases",
            officialSha256Checksum = "4c71887e2b694b97ce1d451a8e1005a8b792e9e6231d68eb2a1d2dca7e8c339a",
            recommendedPool = "ergo.herominers.com",
            poolPort = 1180,
            alternativePools = listOf("erg.2miners.com:8888", "pool.woolypooly.com:3100"),
            hardwareType = "GPU (économe en énergie)",
            unit = "MH/s",
            baseHashrate = 95.0,
            marketPriceUsd = 1.35,
            rewardPerDayPerUnit = 0.00045,
            accentColorHex = 0xFFFF5E57,
            addressPrefixHint = "Commence par 9 (ex: 9f4...)",
            descriptionFr = "Plateforme DeFi programmable hautement sécurisée, résistante aux ASICs et économe en VRAM."
        ),
        CryptoCurrency(
            id = "dogecoin",
            name = "Dogecoin",
            symbol = "DOGE",
            algorithm = "Scrypt",
            recommendedMiner = "AI Scrypt Worker",
            minerVersion = "v1.8",
            officialRepoUrl = "https://github.com/dogecoin/dogecoin",
            officialSha256Checksum = "98234abcf987123ef6543210fedcba9876543210abcdef1234567890abcdef12",
            recommendedPool = "doge.f2pool.com",
            poolPort = 4444,
            alternativePools = listOf("pool.woolypooly.com:3125"),
            hardwareType = "Calcul IA / GPU",
            unit = "MH/s",
            baseHashrate = 220.0,
            marketPriceUsd = 0.28,
            rewardPerDayPerUnit = 0.045,
            accentColorHex = 0xFFC2A633,
            addressPrefixHint = "Commence par D (ex: D8a...)",
            descriptionFr = "Cryptomonnaie iconique et rapide avec algorithme Scrypt assisté par IA."
        ),
        CryptoCurrency(
            id = "clore",
            name = "Clore.ai",
            symbol = "CLORE",
            algorithm = "KawPoW",
            recommendedMiner = "Rigel Miner",
            minerVersion = "v1.19.0",
            officialRepoUrl = "https://github.com/rigelminer/rigel/releases",
            officialSha256Checksum = "d94b11f7c0065a7e584f23e6917aa3432025cc092bba4ebf32b130aef4847321",
            recommendedPool = "clore.herominers.com",
            poolPort = 1190,
            alternativePools = listOf("pool.woolypooly.com:3125"),
            hardwareType = "GPU",
            unit = "MH/s",
            baseHashrate = 31.0,
            marketPriceUsd = 0.092,
            rewardPerDayPerUnit = 0.0019,
            accentColorHex = 0xFF00E5FF,
            addressPrefixHint = "Commence par A ou C (ex: AX8...)",
            descriptionFr = "Réseau décentralisé de puissance de calcul GPU pour le rendu 3D et le machine learning."
        )
    )

    fun findById(id: String): CryptoCurrency = ALL.find { it.id == id } ?: ALL.first()
}
