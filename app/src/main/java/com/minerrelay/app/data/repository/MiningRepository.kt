package com.minerrelay.app.data.repository

import com.minerrelay.app.data.local.AppPreferences
import com.minerrelay.app.data.local.WalletDao
import com.minerrelay.app.data.local.WalletEntity
import kotlinx.coroutines.flow.Flow

class MiningRepository(
    private val walletDao: WalletDao,
    val preferences: AppPreferences
) {
    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWallets()

    suspend fun saveWallet(wallet: WalletEntity): Long {
        if (wallet.isDefault) {
            walletDao.clearDefaultWalletsForCrypto(wallet.cryptoSymbol)
        }
        return walletDao.insertWallet(wallet)
    }

    suspend fun deleteWallet(wallet: WalletEntity) {
        walletDao.deleteWallet(wallet)
    }
}
