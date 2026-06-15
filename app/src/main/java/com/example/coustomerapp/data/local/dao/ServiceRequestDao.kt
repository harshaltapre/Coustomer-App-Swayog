package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.ServiceRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRequestDao {
    @Query("SELECT * FROM service_requests ORDER BY createdAt DESC")
    fun getAllServiceRequests(): Flow<List<ServiceRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRequest(request: ServiceRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<ServiceRequestEntity>)

    @Query("SELECT * FROM service_requests WHERE isSynced = 0")
    suspend fun getUnsyncedRequests(): List<ServiceRequestEntity>

    @Query("UPDATE service_requests SET isSynced = 1, id = :remoteId WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long, remoteId: Int)

    @Query("DELETE FROM service_requests")
    suspend fun clearAll()
}
