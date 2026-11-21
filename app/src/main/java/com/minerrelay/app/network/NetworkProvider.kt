package com.minerrelay.app.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Client réseau HTTP unique partagé par toute l'application
 * (RemoteComputeClient, MempoolSpaceClient, MiningAiAdvisor).
 */
object NetworkProvider {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
