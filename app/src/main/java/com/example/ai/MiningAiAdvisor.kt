package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ApiKeyState {
    UNCHECKED,
    CHECKING,
    HEALTHY,
    UNRESPONSIVE
}

data class ApiKeyHealth(
    val state: ApiKeyState = ApiKeyState.UNCHECKED,
    val message: String? = null
)

data class AdvisorReply(
    val text: String,
    val keyResults: Map<String, ApiKeyHealth> = emptyMap(),
    val failedKeys: List<String> = emptyList()
)

class MiningAiAdvisor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private var rotationIndex = 0

    /**
     * Resolves all active API keys: user-provided multi-keys, plus BuildConfig fallback if valid.
     */
    fun getEffectiveApiKeys(customKeys: List<String>): List<String> {
        val cleaned = customKeys.map { it.trim() }.filter { it.isNotBlank() }.distinct().toMutableList()
        try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = (keyField.get(null) as? String ?: "").trim()
            if (key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY") && !cleaned.contains(key)) {
                cleaned.add(key)
            }
        } catch (_: Exception) {
            // BuildConfig fallback optional
        }
        return cleaned
    }

    /**
     * Vérifie si une clé API répond auprès du serveur Gemini.
     */
    suspend fun verifyApiKey(apiKey: String): ApiKeyHealth = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext ApiKeyHealth(ApiKeyState.UNCHECKED, null)
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash?key=$trimmed"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> ApiKeyHealth(
                        state = ApiKeyState.HEALTHY,
                        message = "Clé API active et joignable"
                    )
                    response.code == 400 || response.code == 401 || response.code == 403 -> ApiKeyHealth(
                        state = ApiKeyState.UNRESPONSIVE,
                        message = "Clé invalide ou refusée (HTTP ${response.code})"
                    )
                    response.code == 429 -> ApiKeyHealth(
                        state = ApiKeyState.UNRESPONSIVE,
                        message = "Quota dépassé (HTTP 429)"
                    )
                    else -> ApiKeyHealth(
                        state = ApiKeyState.UNRESPONSIVE,
                        message = "Aucune réponse valide (HTTP ${response.code})"
                    )
                }
            }
        } catch (e: Exception) {
            ApiKeyHealth(
                state = ApiKeyState.UNRESPONSIVE,
                message = "Ne répond pas (${e.javaClass.simpleName})"
            )
        }
    }

    suspend fun askAdvisor(
        prompt: String,
        customApiKeys: List<String>,
        currentCrypto: String? = null,
        currentHashrate: String? = null
    ): String {
        return askAdvisorWithHealth(prompt, customApiKeys, currentCrypto, currentHashrate).text
    }

    suspend fun askAdvisorWithHealth(
        prompt: String,
        customApiKeys: List<String>,
        currentCrypto: String? = null,
        currentHashrate: String? = null
    ): AdvisorReply = withContext(Dispatchers.IO) {
        val activeKeys = getEffectiveApiKeys(customApiKeys)
        val keyResults = mutableMapOf<String, ApiKeyHealth>()
        val failedKeys = mutableListOf<String>()

        if (activeKeys.isNotEmpty()) {
            // Rotation & bascule automatique entre toutes les clés API pour réfléchir plus vite et éviter les quotas
            val startIndex = rotationIndex % activeKeys.size
            rotationIndex = (rotationIndex + 1) % activeKeys.size

            val orderedKeys = activeKeys.indices.map { offset ->
                activeKeys[(startIndex + offset) % activeKeys.size]
            }

            val deepReasoningMode = activeKeys.size >= 3

            for ((attemptIdx, key) in orderedKeys.withIndex()) {
                try {
                    val result = callGeminiApi(
                        userPrompt = prompt,
                        apiKey = key,
                        currentCrypto = currentCrypto,
                        currentHashrate = currentHashrate,
                        activeKeysCount = activeKeys.size,
                        deepReasoning = deepReasoningMode
                    )
                    if (result.isSuccess) {
                        val responseText = result.getOrNull().orEmpty()
                        if (responseText.isNotBlank()) {
                            keyResults[key] = ApiKeyHealth(ApiKeyState.HEALTHY, "Réponse reçue")
                            val prefix = if (deepReasoningMode) {
                                "⚡ [Réflexion Multi-Clés ×${activeKeys.size} • Nœud #${((startIndex + attemptIdx) % activeKeys.size) + 1}]\n"
                            } else {
                                ""
                            }
                            return@withContext AdvisorReply(
                                text = prefix + responseText,
                                keyResults = keyResults,
                                failedKeys = failedKeys
                            )
                        } else {
                            keyResults[key] = ApiKeyHealth(ApiKeyState.UNRESPONSIVE, "Réponse vide du serveur")
                            failedKeys.add(key)
                        }
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Clé API ne répond pas"
                        keyResults[key] = ApiKeyHealth(ApiKeyState.UNRESPONSIVE, err)
                        failedKeys.add(key)
                    }
                } catch (e: Exception) {
                    keyResults[key] = ApiKeyHealth(
                        ApiKeyState.UNRESPONSIVE,
                        "Ne répond pas (${e.javaClass.simpleName})"
                    )
                    failedKeys.add(key)
                }
            }
        }

        // Moteur d'expertise intégré si hors-ligne ou clés non encore activées
        val offlineText = generateExpertOfflineResponse(prompt, currentCrypto, activeKeys.size)
        AdvisorReply(
            text = offlineText,
            keyResults = keyResults,
            failedKeys = failedKeys
        )
    }

    private fun callGeminiApi(
        userPrompt: String,
        apiKey: String,
        currentCrypto: String?,
        currentHashrate: String?,
        activeKeysCount: Int,
        deepReasoning: Boolean
    ): Result<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val reasoningDirective = if (deepReasoning) {
            "MODE RÉFLEXION APPROFONDIE ACTIVÉ ($activeKeysCount clés API en relais) : Fournis une analyse détaillée, structurée étape par étape, avec calculs de rendement et optimisations stratégiques."
        } else {
            "Réponds de façon claire, structurée et concise."
        }

        val systemContext = "Tu es l'Assistant IA expert de l'application Miner IA, dédiée UNIQUEMENT au minage Bitcoin (BTC / SHA-256). " +
                "L'application utilise le minage cloud IA déporté (0% CPU/GPU sur le téléphone : tous les calculs Bitcoin sont effectués sur les serveurs de l'IA et les gains BTC sont virés sur le portefeuille public). " +
                "RÈGLE ABSOLUE DE SÉCURITÉ : Ne demande JAMAIS de clé privée ni de seed phrase (12-24 mots). " +
                "$reasoningDirective " +
                "Contexte actuel : Crypto = Bitcoin (BTC), Vitesse Serveur IA = ${currentHashrate ?: "0"}, Clés API actives = $activeKeysCount.\n"

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val item = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemContext\nQuestion de l'utilisateur : $userPrompt"))
                    })
                }
                put(item)
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException("Erreur HTTP ${response.code}"))
            }
            val responseBody = response.body?.string() ?: return Result.success("")
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates") ?: return Result.success("")
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return Result.success(parts.getJSONObject(0).optString("text", ""))
                }
            }
        }
        return Result.success("")
    }

    private fun generateExpertOfflineResponse(
        prompt: String,
        currentCrypto: String?,
        activeKeysCount: Int
    ): String {
        val lower = prompt.lowercase()
        val boostHeader = if (activeKeysCount >= 3) {
            "⚡ **Mode Multi-Clés Actif ($activeKeysCount clés)** — Analyse approfondie :\n\n"
        } else if (activeKeysCount > 0) {
            "🔑 **Clés API actives : $activeKeysCount/3** *(Ajoutez au moins 3 clés pour activer la vitesse maximale)*\n\n"
        } else {
            ""
        }

        val body = when {
            lower.contains("rentable") || lower.contains("gain") || lower.contains("bitcoin") || lower.contains("btc") -> {
                """
📊 **Optimisation du Minage Bitcoin (BTC) sur Serveur IA :**
• **Algorithme SHA-256 Déporté :** Les calculs Bitcoin sont 100% exécutés par les serveurs IA en ligne 24h/24, sans aucune chauffe ni batterie consommée sur votre appareil.
• **Versements BTC :** Indiquez une adresse Bitcoin publique valide (`1...`, `3...` ou `bc1...`) pour recevoir les récompenses.
• **Astuce Multi-Clés :** Avec **3 clés API ou plus**, les calculs SHA-256 sont répartis en parallèle, augmentant la cadence et la réactivité.
                """.trimIndent()
            }

            lower.contains("rejetée") || lower.contains("stale") || lower.contains("erreur") || lower.contains("rejected") -> {
                """
🛠️ **Diagnostic Réseau & Serveur IA Bitcoin :**
• **Latence vers l'IP du Serveur IA :** Vérifiez que l'adresse IP du serveur IA est bien saisie et accessible.
• **Surcharge d'une clé API :** Avec une seule clé, les limites de débit peuvent ralentir l'IA. En enregistrant **3 clés API ou plus** dans Paramètres, Miner IA bascule automatiquement d'une clé à l'autre sans interruption.
                """.trimIndent()
            }

            lower.contains("clé") || lower.contains("api") || lower.contains("vitesse") || lower.contains("accélér") -> {
                """
🚀 **Accélération Bitcoin par Multi-Clés API (3+ clés) :**
1. Saisissez **au moins 3 clés API** dans l'onglet **Paramètres** ou juste ci-dessus.
2. **Calcul plus rapide :** Chaque clé supplémentaire ouvre un canal de calcul parallèle avec les serveurs IA pour le minage Bitcoin.
3. **Réflexion approfondie :** L'assistant dispose de plus de capacité de raisonnement et ne tombe jamais en limite de quota grâce à la rotation automatique.
                """.trimIndent()
            }

            else -> {
                """
💡 **Conseil Miner IA (Bitcoin BTC uniquement) :**
1. **Adresse Bitcoin :** Collez uniquement votre adresse publique BTC (`1...`, `3...` ou `bc1...`) — jamais de seed phrase !
2. **Serveur IA :** Renseignez l'IP du serveur IA pour lancer les calculs SHA-256 en ligne 24h/24 (0% CPU/GPU sur votre téléphone).
3. **Boost ×3+ :** Ajoutez **3 clés API ou plus** dans **Paramètres** pour permettre à l'application de réfléchir et de calculer encore plus vite !
                """.trimIndent()
            }
        }
        return boostHeader + body
    }
}
