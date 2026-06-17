package com.example.coustomerapp.data.local.dao

import androidx.room.*
import com.example.coustomerapp.data.local.entities.ServiceRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRequestDao {
    @Query("SELECT * FROM service_requests ORDER BY localId DESC")
    fun getAllRequests(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE isSynced = 0")
    suspend fun getUnsyncedRequests(): List<ServiceRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ServiceRequestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ServiceRequestEntity>)

    @Query("UPDATE service_requests SET isSynced = 1, serverId = :serverId WHERE localId = :localId")
    suspend fun markSynced(localId: Int, serverId: Int)
}
