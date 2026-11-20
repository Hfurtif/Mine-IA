package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_rigs")
data class RemoteRigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String = "miner",
    val assignedCrypto: String = "BTC",
    val workerName: String = "rig1",
    val notes: String = "",
    val isOnline: Boolean = false,
    val lastPingTimestamp: Long = 0L
)
