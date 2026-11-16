package com.example.data.repository

import com.example.data.local.AppPreferences
import com.example.data.local.RemoteRigDao
import com.example.data.local.RemoteRigEntity
import com.example.data.local.WalletDao
import com.example.data.local.WalletEntity
import kotlinx.coroutines.flow.Flow

class MiningRepository(
    private val walletDao: WalletDao,
    private val remoteRigDao: RemoteRigDao,
    val preferences: AppPreferences
) {
    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWallets()
    val allRigs: Flow<List<RemoteRigEntity>> = remoteRigDao.getAllRigs()

    fun getWalletsByCrypto(cryptoSymbol: String): Flow<List<WalletEntity>> =
        walletDao.getWalletsByCrypto(cryptoSymbol)

    suspend fun saveWallet(wallet: WalletEntity): Long {
        if (wallet.isDefault) {
            walletDao.clearDefaultWalletsForCrypto(wallet.cryptoSymbol)
        }
        return walletDao.insertWallet(wallet)
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        if (wallet.isDefault) {
            walletDao.clearDefaultWalletsForCrypto(wallet.cryptoSymbol)
        }
        walletDao.updateWallet(wallet)
    }

    suspend fun deleteWallet(wallet: WalletEntity) {
        walletDao.deleteWallet(wallet)
    }

    suspend fun saveRig(rig: RemoteRigEntity): Long = remoteRigDao.insertRig(rig)
    suspend fun updateRig(rig: RemoteRigEntity) = remoteRigDao.updateRig(rig)
    suspend fun deleteRig(rig: RemoteRigEntity) = remoteRigDao.deleteRig(rig)
    suspend fun updateRigStatus(id: Long, isOnline: Boolean) =
        remoteRigDao.updateRigStatus(id, isOnline, System.currentTimeMillis())

    suspend fun seedInitialWalletsIfEmpty(currentList: List<WalletEntity>) {
        // Left intentionally empty: the user enters their own wallet address directly.
    }

    suspend fun seedInitialRigsIfEmpty(currentRigs: List<RemoteRigEntity>) {
        if (currentRigs.isEmpty()) {
            remoteRigDao.insertRig(
                RemoteRigEntity(
                    name = "PC Tour Maison (Ryzen 9)",
                    host = "192.168.1.120",
                    port = 22,
                    username = "miner",
                    assignedCrypto = "XMR",
                    workerName = "desktop-ryzen",
                    notes = "XMRig configuré avec 16 threads, Huge Pages activées",
                    isOnline = true,
                    lastPingTimestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
