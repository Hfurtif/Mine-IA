package com.minerrelay.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cryptoSymbol: String = "BTC",
    val publicAddress: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
