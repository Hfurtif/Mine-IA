package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cryptoSymbol: String,
    val publicAddress: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
