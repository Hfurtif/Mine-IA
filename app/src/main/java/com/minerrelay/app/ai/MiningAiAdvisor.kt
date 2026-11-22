package com.minerrelay.app.ai

import com.minerrelay.app.mining.MiningTelemetry
import com.minerrelay.app.security.EncryptedKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

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

/**
 * Assistant Gemini optionnel servant UNIQUEMENT à expliquer les métriques réelles du relais.
 * - Sa clé API est strictement séparée de celle du service de calcul.
 * - Ne prétend jamais miner ni verser de gains.
 */
class MiningAiAdvisor(
    private val client: OkHttpClient
) {

    suspend fun verifyGeminiKey(apiKey: String): ApiKeyHealth = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return@withContext ApiKeyHealth(ApiKeyState.UNCHECKED, "Aucune clé Gemini saisie")
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash?key=$trimmed"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> ApiKeyHealth(ApiKeyState.HEALTHY, "Clé Gemini valide")
                    response.code == 400 || response.code == 401 || response.code == 403 ->
                        ApiKeyHealth(ApiKeyState.UNRESPONSIVE, "Clé Gemini invalide (HTTP ${response.code})")
                    response.code == 429 ->
                        ApiKeyHealth(ApiKeyState.UNRESPONSIVE, "Quota Gemini dépassé (HTTP 429)")
                    else ->
                        ApiKeyHealth(ApiKeyState.UNRESPONSIVE, "Erreur HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            ApiKeyHealth(ApiKeyState.UNRESPONSIVE, "Injoignable (${e.javaClass.simpleName})")
        }
    }

    suspend fun explainTelemetry(
        userPrompt: String,
        geminiApiKey: String,
        telemetry: MiningTelemetry
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = geminiApiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Aucune clé API Gemini configurée dans Réglages. La clé Gemini est distincte de la clé du service de calcul.")
            )
        }

        val stateSummary = buildString {
            appendLine("ÉTAT RÉEL DU RELAIS BITCOIN (STRATUM v1) :")
            appendLine("- Statut relais : ${telemetry.status.labelFr}")
            appendLine("- Connexion pool : ${telemetry.poolConnectionState.labelFr} (${telemetry.poolDetail})")
            if (telemetry.activePoolEndpoint.isNotBlank()) {
                appendLine("- Endpoint actif : ${telemetry.activePoolEndpoint}")
            }
            appendLine("- Parts acceptées : ${telemetry.acceptedShares}")
            appendLine("- Parts rejetées : ${telemetry.rejectedShares}")
            appendLine("- Parts périmées (stale) : ${telemetry.staleShares}")
            appendLine("- Candidats invalides bloqués localement : ${telemetry.locallyRejectedCandidates}")
            appendLine("- Hashrate mesuré (15 min) : ${String.format(Locale.US, "%.2f", telemetry.hashrate15mHps)} H/s")
            appendLine("- Hashrate mesuré (24 h) : ${String.format(Locale.US, "%.2f", telemetry.hashrate24hHps)} H/s")
            appendLine("- Difficulté pool active : ${telemetry.currentDifficulty}")
            appendLine("- Latence service de calcul : ${telemetry.serviceLatencyMs?.let { "$it ms" } ?: "N/A"}")
            appendLine("- Coût cumulé service de calcul : ${telemetry.accumulatedServiceCostUsd?.let { "$${String.format(Locale.US, "%.4f", it)}" } ?: "Non fourni par l'API"}")
            appendLine("- Paiements BTC reçus sur mempool.space : ${String.format(Locale.US, "%.8f", telemetry.totalConfirmedPayoutsBtc)} BTC")
            appendLine("- Erreur service de calcul : ${telemetry.computeErrorMessage ?: "Aucune"}")
            appendLine("- Mode test faible difficulté : ${if (telemetry.isTestMode) "Activé" else "Désactivé"}")
        }

        val systemInstruction = "Tu es un assistant technique pédagogique en français. " +
            "Ton SEUL rôle est d'expliquer les chiffres réels ci-dessous observés par l'application relais Stratum v1 Bitcoin. " +
            "RÈGLES STRICTES : " +
            "1) Tu ne mines PAS Bitcoin et tu ne verses AUCUN gain. " +
            "2) L'application est un intermédiaire Stratum v1 <-> service de calcul distant avec re-vérification locale double SHA-256. " +
            "3) Ne promets jamais de rendement financier ni de minage lorsque l'app est fermée. " +
            "4) Base-toi uniquement sur les chiffres fournis (si un chiffre est 0 ou N/A, explique-le honnêtement)."

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$cleanKey"
        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", "$stateSummary\nQuestion : $userPrompt")))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val maskedErr = EncryptedKeyStore.redactSensitive(
                        "Erreur HTTP ${response.code} lors de l'appel Gemini.",
                        listOf(cleanKey),
                        emptyList()
                    )
                    return@withContext Result.failure(IllegalStateException(maskedErr))
                }
                val raw = response.body?.string().orEmpty()
                val text = JSONObject(raw)
                    .optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "")
                    .orEmpty()
                if (text.isBlank()) {
                    Result.failure(IllegalStateException("Réponse vide de Gemini."))
                } else {
                    Result.success(text)
                }
            }
        } catch (e: Exception) {
            val safeMsg = EncryptedKeyStore.redactSensitive(
                e.message ?: e.javaClass.simpleName,
                listOf(cleanKey),
                emptyList()
            )
            Result.failure(IllegalStateException("Erreur Gemini : $safeMsg"))
        }
    }
}
