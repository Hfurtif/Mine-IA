package com.minerrelay.app.compute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class ComputeBatchRequest(
    val jobId: String,
    val headerPrefix76BytesHex: String,
    val targetHex: String,
    val nonceStart: Long,
    val nonceEnd: Long
)

sealed class ComputeBatchResult {
    data class Success(
        val candidateNonces: List<Long>,
        val latencyMs: Long,
        val batchCostUsd: Double?
    ) : ComputeBatchResult()

    data class InvalidApiKey(val message: String) : ComputeBatchResult()
    data class QuotaExceeded(val message: String) : ComputeBatchResult()
    data class InsufficientBalance(val message: String) : ComputeBatchResult()
    data class ServiceUnavailable(val message: String) : ComputeBatchResult()
    data class IncompleteApiConfiguration(val message: String) : ComputeBatchResult()
}

sealed class ComputeHealthResult {
    data class Healthy(val latencyMs: Long, val detail: String) : ComputeHealthResult()
    data class Unhealthy(val reason: String) : ComputeHealthResult()
    data class IncompleteConfiguration(val reason: String) : ComputeHealthResult()
}

/**
 * Interface vers le service de calcul SHA-256 distant.
 * Reçoit l'en-tête de bloc (ou préfixe 76 octets) + cible + intervalle de nonce,
 * et renvoie les nonces candidats à re-vérifier localement par double SHA-256.
 */
interface RemoteComputeClient {
    suspend fun solveBatch(request: ComputeBatchRequest, apiKey: String): ComputeBatchResult
    suspend fun checkHealth(apiKey: String): ComputeHealthResult
}

/**
 * Implémentation HTTP basée sur le client OkHttp unique partagé.
 *
 * IMPORTANT : La documentation complète de l'API du service de calcul distant n'ayant pas été
 * fournie dans le cahier des charges ([COLLE ICI LA DOCUMENTATION COMPLÈTE...]), aucun endpoint
 * ni schéma propriétaire n'a été inventé. Complétez les TODO ci-dessous dès réception de la doc.
 */
class HttpRemoteComputeClient(
    sharedOkHttpClient: OkHttpClient,
    private val endpointUrlProvider: () -> String
) : RemoteComputeClient {

    private val client: OkHttpClient = sharedOkHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun solveBatch(
        request: ComputeBatchRequest,
        apiKey: String
    ): ComputeBatchResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext ComputeBatchResult.InvalidApiKey(
                "Clé API du service de calcul manquante. Ajoutez-la dans Réglages."
            )
        }

        // TODO(API-DOC): Remplacer cette URL dynamique par l'URL officielle de l'endpoint de calcul SHA-256
        //  (ex. https://api.votre-service-calcul.example/v1/sha256/scan).
        val baseUrl = endpointUrlProvider().trim().ifEmpty { DEFAULT_UNCONFIGURED_URL }
        if (baseUrl.isEmpty()) {
            return@withContext ComputeBatchResult.IncompleteApiConfiguration(
                "Documentation/URL de l'API du service de calcul manquante (voir TODO HttpRemoteComputeClient)."
            )
        }

        // TODO(API-DOC): Vérifier le format exact du corps JSON attendu par l'API
        //  (noms des champs : header_hex, target_hex, nonce_start, nonce_end, encodage hex vs base64).
        val bodyJson = JSONObject().apply {
            put("job_id", request.jobId)
            put("header_prefix_hex", request.headerPrefix76BytesHex)
            put("target_hex", request.targetHex)
            put("nonce_start", request.nonceStart)
            put("nonce_end", request.nonceEnd)
        }

        var attempt = 0
        var lastUnavailable: ComputeBatchResult.ServiceUnavailable? = null

        while (attempt <= MAX_RETRIES) {
            val startedNs = System.nanoTime()
            try {
                // TODO(API-DOC): Confirmer le header d'authentification attendu
                //  (ex. "Authorization: Bearer <key>" ou "X-API-Key: <key>").
                val httpRequest = Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", "Bearer $cleanKey")
                    .header("Accept", "application/json")
                    .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    val latencyMs = ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)
                    val rawBody = response.body?.string().orEmpty()

                    when (response.code) {
                        in 200..299 -> {
                            return@withContext parseSuccessResponse(rawBody, latencyMs)
                        }
                        401, 403 -> {
                            return@withContext ComputeBatchResult.InvalidApiKey(
                                "Clé API invalide ou refusée (HTTP ${response.code})."
                            )
                        }
                        402 -> {
                            // TODO(API-DOC): Confirmer le code HTTP exact utilisé pour "solde épuisé" (402 ou champ JSON).
                            return@withContext ComputeBatchResult.InsufficientBalance(
                                "Solde du service de calcul épuisé (HTTP 402)."
                            )
                        }
                        429 -> {
                            // TODO(API-DOC): Lire Retry-After si l'API fournit une limite de débit par clé/minute.
                            return@withContext ComputeBatchResult.QuotaExceeded(
                                "Quota du service de calcul dépassé (HTTP 429)."
                            )
                        }
                        else -> {
                            lastUnavailable = ComputeBatchResult.ServiceUnavailable(
                                "Service de calcul indisponible (HTTP ${response.code})."
                            )
                        }
                    }
                }
            } catch (e: SocketTimeoutException) {
                lastUnavailable = ComputeBatchResult.ServiceUnavailable(
                    "Timeout du service de calcul (${e.message ?: "délai dépassé"})."
                )
            } catch (e: IOException) {
                lastUnavailable = ComputeBatchResult.ServiceUnavailable(
                    "Erreur réseau vers le service de calcul (${e.javaClass.simpleName})."
                )
            }

            attempt++
            if (attempt <= MAX_RETRIES) {
                delay(500L * attempt)
            }
        }

        lastUnavailable ?: ComputeBatchResult.ServiceUnavailable("Service de calcul injoignable.")
    }

    override suspend fun checkHealth(apiKey: String): ComputeHealthResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext ComputeHealthResult.Unhealthy("Clé API du service de calcul vide.")
        }
        val baseUrl = endpointUrlProvider().trim().ifEmpty { DEFAULT_UNCONFIGURED_URL }
        if (baseUrl.isEmpty()) {
            return@withContext ComputeHealthResult.IncompleteConfiguration(
                "Endpoint API non configuré (TODO : doc API du service de calcul manquante)."
            )
        }

        // TODO(API-DOC): Remplacer par l'endpoint officiel de santé/solde (ex. GET /v1/health ou /v1/account).
        val startedNs = System.nanoTime()
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer $cleanKey")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val latencyMs = ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)
                when {
                    response.isSuccessful -> ComputeHealthResult.Healthy(latencyMs, "HTTP ${response.code} (${latencyMs} ms)")
                    response.code == 401 || response.code == 403 -> ComputeHealthResult.Unhealthy("Clé invalide (HTTP ${response.code})")
                    response.code == 402 -> ComputeHealthResult.Unhealthy("Solde épuisé (HTTP 402)")
                    response.code == 429 -> ComputeHealthResult.Unhealthy("Quota dépassé (HTTP 429)")
                    else -> ComputeHealthResult.Unhealthy("Réponse inattendue HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            ComputeHealthResult.Unhealthy("Service injoignable (${e.javaClass.simpleName})")
        }
    }

    private fun parseSuccessResponse(rawBody: String, latencyMs: Long): ComputeBatchResult {
        return try {
            val json = JSONObject(rawBody)
            // TODO(API-DOC): Confirmer le nom exact du tableau de nonces candidats dans la réponse JSON
            //  (ex. "candidate_nonces" ou "nonces") ainsi que le champ de coût ("cost_usd").
            val noncesArray = json.optJSONArray("candidate_nonces")
                ?: json.optJSONArray("nonces")
                ?: JSONArray()

            val nonces = buildList {
                for (i in 0 until noncesArray.length()) {
                    val value = noncesArray.optLong(i, -1L)
                    if (value >= 0L) add(value)
                }
            }
            val costUsd = if (json.has("cost_usd") && !json.isNull("cost_usd")) {
                json.optDouble("cost_usd")
            } else {
                null
            }
            ComputeBatchResult.Success(
                candidateNonces = nonces,
                latencyMs = latencyMs,
                batchCostUsd = costUsd
            )
        } catch (e: Exception) {
            ComputeBatchResult.ServiceUnavailable(
                "Format de réponse JSON inattendu du service de calcul (${e.javaClass.simpleName})."
            )
        }
    }

    companion object {
        // TODO(API-DOC): Renseigner l'URL réelle de l'API du service de calcul ici lorsque la documentation est disponible.
        const val DEFAULT_UNCONFIGURED_URL = ""
        private const val MAX_RETRIES = 2
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
