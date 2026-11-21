package com.minerrelay.app.service

import android.content.Context
import com.minerrelay.app.ai.MiningAiAdvisor
import com.minerrelay.app.blockchain.MempoolSpaceClient
import com.minerrelay.app.compute.HttpRemoteComputeClient
import com.minerrelay.app.data.local.AppPreferences
import com.minerrelay.app.mining.MiningEngine
import com.minerrelay.app.network.NetworkProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Conteneur applicatif unique permettant au ForegroundService et au ViewModel
 * de partager le même MiningEngine et le client OkHttp unique.
 */
object RelayCoordinator {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var preferencesInstance: AppPreferences? = null

    @Volatile
    private var engineInstance: MiningEngine? = null

    val mempoolClient: MempoolSpaceClient by lazy {
        MempoolSpaceClient(NetworkProvider.okHttpClient)
    }

    val aiAdvisor: MiningAiAdvisor by lazy {
        MiningAiAdvisor(NetworkProvider.okHttpClient)
    }

    fun getPreferences(context: Context): AppPreferences {
        return preferencesInstance ?: synchronized(this) {
            preferencesInstance ?: AppPreferences(context.applicationContext).also {
                preferencesInstance = it
            }
        }
    }

    fun getComputeClient(context: Context): HttpRemoteComputeClient {
        val prefs = getPreferences(context)
        return HttpRemoteComputeClient(
            sharedOkHttpClient = NetworkProvider.okHttpClient,
            endpointUrlProvider = { prefs.computeApiUrl }
        )
    }

    fun getEngine(context: Context): MiningEngine {
        return engineInstance ?: synchronized(this) {
            engineInstance ?: MiningEngine(
                scope = appScope,
                computeClient = getComputeClient(context),
                mempoolClient = mempoolClient
            ).also { engineInstance = it }
        }
    }
}
