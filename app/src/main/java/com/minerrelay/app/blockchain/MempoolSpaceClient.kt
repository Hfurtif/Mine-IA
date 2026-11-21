package com.minerrelay.app.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class BlockchainTransaction(
    val txId: String,
    val timestampMs: Long,
    val amountSatoshis: Long,
    val blockHeight: Long?,
    val confirmed: Boolean
) {
    val amountBtc: Double
        get() = amountSatoshis / 100_000_000.0
}

data class MempoolAddressSummary(
    val transactions: List<BlockchainTransaction>,
    val totalReceivedBtc: Double,
    val btcPriceEur: Double?
)

/**
 * Client en lecture seule interrogeant l'API publique mempool.space
 * avec le client OkHttp unique partagé.
 */
class MempoolSpaceClient(
    private val client: OkHttpClient
) {
    suspend fun fetchAddressTransactions(btcAddress: String): Result<MempoolAddressSummary> =
        withContext(Dispatchers.IO) {
            val cleanAddress = btcAddress.trim()
            if (cleanAddress.isEmpty()) {
                return@withContext Result.success(MempoolAddressSummary(emptyList(), 0.0, null))
            }

            try {
                val txRequest = Request.Builder()
                    .url("https://mempool.space/api/address/$cleanAddress/txs")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val transactions = client.newCall(txRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("API mempool.space HTTP ${response.code}")
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    parseTransactions(cleanAddress, JSONArray(body))
                }

                val eurPrice = fetchBtcPriceEurOrNull()
                val totalSats = transactions.sumOf { it.amountSatoshis }
                Result.success(
                    MempoolAddressSummary(
                        transactions = transactions,
                        totalReceivedBtc = totalSats / 100_000_000.0,
                        btcPriceEur = eurPrice
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun fetchBtcPriceEurOrNull(): Double? {
        return try {
            val priceReq = Request.Builder()
                .url("https://mempool.space/api/v1/prices")
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(priceReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string().orEmpty())
                if (json.has("EUR")) json.optDouble("EUR") else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTransactions(address: String, array: JSONArray): List<BlockchainTransaction> {
        val result = mutableListOf<BlockchainTransaction>()
        val nowMs = System.currentTimeMillis()
        for (i in 0 until minOf(array.length(), 25)) {
            val tx = array.optJSONObject(i) ?: continue
            val txId = tx.optString("txid", "")
            val status = tx.optJSONObject("status")
            val confirmed = status?.optBoolean("confirmed", false) ?: false
            val blockHeight = if (status != null && status.has("block_height") && !status.isNull("block_height")) {
                status.optLong("block_height")
            } else {
                null
            }
            val blockTimeSec = status?.optLong("block_time", 0L) ?: 0L
            val timestampMs = if (blockTimeSec > 0L) blockTimeSec * 1000L else nowMs

            var incomingSats = 0L
            val vout = tx.optJSONArray("vout")
            if (vout != null) {
                for (j in 0 until vout.length()) {
                    val outObj = vout.optJSONObject(j) ?: continue
                    if (outObj.optString("scriptpubkey_address") == address) {
                        incomingSats += outObj.optLong("value", 0L)
                    }
                }
            }

            if (incomingSats > 0L && txId.isNotEmpty()) {
                result.add(
                    BlockchainTransaction(
                        txId = txId,
                        timestampMs = timestampMs,
                        amountSatoshis = incomingSats,
                        blockHeight = blockHeight,
                        confirmed = confirmed
                    )
                )
            }
        }
        return result
    }
}
