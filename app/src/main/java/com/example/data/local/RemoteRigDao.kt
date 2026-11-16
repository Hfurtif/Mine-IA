package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteRigDao {
    @Query("SELECT * FROM remote_rigs ORDER BY id DESC")
    fun getAllRigs(): Flow<List<RemoteRigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRig(rig: RemoteRigEntity): Long

    @Update
    suspend fun updateRig(rig: RemoteRigEntity)

    @Delete
    suspend fun deleteRig(rig: RemoteRigEntity)

    @Query("UPDATE remote_rigs SET isOnline = :isOnline, lastPingTimestamp = :timestamp WHERE id = :id")
    suspend fun updateRigStatus(id: Long, isOnline: Boolean, timestamp: Long)
}
