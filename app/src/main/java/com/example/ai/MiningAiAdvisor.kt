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

class MiningAiAdvisor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Resolves the active API key: user-provided in settings, or BuildConfig fallback.
     */
    fun getEffectiveApiKey(customKey: String): String {
        return customKey.trim().ifEmpty {
            try {
                // If BuildConfig.GEMINI_API_KEY is available and valid
                val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
                val key = keyField.get(null) as? String ?: ""
                if (key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")) key else ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    suspend fun askAdvisor(
        prompt: String,
        customApiKey: String,
        currentCrypto: String? = null,
        currentHashrate: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)

        if (apiKey.isNotBlank()) {
            try {
                val response = callGeminiApi(prompt, apiKey, currentCrypto, currentHashrate)
                if (response.isNotBlank()) return@withContext response
            } catch (e: Exception) {
                // In case of network or API error, fall through to specialized expert engine
            }
        }

        // High-fidelity fallback expert knowledge engine for beginners
        generateExpertOfflineResponse(prompt, currentCrypto)
    }

    private fun callGeminiApi(
        userPrompt: String,
        apiKey: String,
        currentCrypto: String?,
        currentHashrate: String?
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val systemContext = "Tu es l'Assistant IA expert de l'application Miner IA. " +
                "Ton rôle est d'expliquer le minage de cryptomonnaies à des débutants en français de manière claire, bienveillante et sécurisée. " +
                "RÈGLES ABSOLUES DE SÉCURITÉ : " +
                "1. Ne demande JAMAIS de clé privée ni de seed phrase (12-24 mots). " +
                "2. Rappelle toujours que l'IA ne calcule AUCUN hash (seul le logiciel comme XMRig le fait). " +
                "3. Rappelle toujours les coûts d'électricité et la rentabilité réelle. " +
                "Contexte actuel : Crypto = ${currentCrypto ?: "Non définie"}, Hashrate = ${currentHashrate ?: "0"}.\n"

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
                return "Erreur API (${response.code}) : Vérifiez votre clé API ou quota."
            }
            val responseBody = response.body?.string() ?: return ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        return ""
    }

    private fun generateExpertOfflineResponse(prompt: String, currentCrypto: String?): String {
        val lower = prompt.lowercase()

        return when {
            lower.contains("rentable") || lower.contains("meilleure crypto") || lower.contains("choisir") -> {
                """
📊 **Conseil de Rentabilité Matérielle :**
• **Si vous minez sur processeur (CPU) :** **Monero (XMR)** est de loin le choix numéro 1. Son algorithme *RandomX* utilise le cache L3 de votre processeur (très performant sur AMD Ryzen 3000/5000/7000/9000).
• **Si vous disposez d'une carte graphique (GPU Nvidia ou AMD) :** **Kaspa (KAS)**, **Ravencoin (RVN)** ou **Ergo (ERG)** sont les plus réputés. Ergo a l'avantage de consommer moins d'énergie thermique.
• **⚠️ Règle d'or de l'électricité :** En France et en Europe, le kWh coûte généralement entre 0.20€ et 0.25€. Si votre machine consomme 200W, elle dépense environ 1.00€ d'électricité par jour. Vérifiez que vos gains journaliers dépassent ce montant !
                """.trimIndent()
            }

            lower.contains("rejetée") || lower.contains("stale") || lower.contains("erreur") || lower.contains("rejected") -> {
                """
🛠️ **Diagnostic des Shares Rejetées :**
Une "share" rejetée par le pool peut s'expliquer par 3 causes principales :
1. **Stale share (Share périmée) :** Votre connexion Internet a trop de latence (ping élevé vers le pool). Le bloc a changé avant que votre calcul n'arrive. *Solution :* Choisissez un pool plus proche de chez vous (ex: serveurs EU au lieu de US).
2. **Invalid share :** Overclocking trop agressif de la mémoire ou du processeur qui provoque des erreurs de calcul silencieuses. *Solution :* Réduisez légèrement la fréquence ou augmentez la tension.
3. **Difficulté inadaptée :** Le port du pool a une difficulté statique trop élevée pour votre hashrate. *Solution :* Utilisez le port avec difficulté variable (VarDiff).
                """.trimIndent()
            }

            lower.contains("consommation") || lower.contains("watt") || lower.contains("température") || lower.contains("chauff") -> {
                """
⚡ **Optimisation Électrique & Température :**
• **Undervolting :** Réduire la tension (voltage) du CPU ou GPU permet souvent de baisser la consommation de 20% à 30% tout en conservant 95% du hashrate !
• **Seuil de température de sécurité :** Dans Miner IA, nous vous conseillons de maintenir le seuil à **75°C max**. Au-delà de 80°C, les composants s'usent prématurément.
• **Curseur de charge CPU :** Réglez le curseur à 50% ou 75% pour garder un système réactif et frais, ou 100% uniquement si vous avez un refroidissement dédié.
                """.trimIndent()
            }

            lower.contains("vps") || lower.contains("serveur") || lower.contains("ssh") || lower.contains("installer") -> {
                """
🖥️ **Déploiement sur Serveur Distant / VPS :**
1. Connectez-vous à votre serveur Linux via SSH : `ssh utilisateur@ip_serveur`
2. Rendez-vous dans l'onglet **Machines Distantes** de l'application Miner IA.
3. Choisissez votre crypto et votre wallet : Miner IA génère la commande clé-en-main complète !
4. Copiez et collez la commande dans votre terminal : le mineur officiel (XMRig ou lolMiner) est téléchargé, vérifié par son empreinte SHA256 et lancé en arrière-plan.
                """.trimIndent()
            }

            else -> {
                """
💡 **Conseil Miner IA :**
Pour bien débuter le minage :
1. **Wallet :** Assurez-vous d'avoir enregistré une adresse publique valide dans l'onglet **Portefeuilles** (ne collez jamais de seed phrase !).
2. **Choix :** Pour un CPU standard, privilégiez **Monero (XMR)** avec le mineur XMRig.
3. **Surveillance :** Gardez un œil sur la température et le compteur de rentabilité nette (Gains - Électricité) dans le Tableau de Bord.

*Note : Vous pouvez renseigner votre clé API Gemini dans les réglages pour bénéficier de réponses personnalisées en direct.*
                """.trimIndent()
            }
        }
    }
}
