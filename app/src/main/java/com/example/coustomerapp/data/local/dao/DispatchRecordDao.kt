package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.DispatchRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DispatchRecordDao {
    @Query("SELECT * FROM dispatch_records ORDER BY dispatchedAt DESC")
    fun getAllDispatches(): Flow<List<DispatchRecordEntity>>

    @Query("SELECT * FROM dispatch_records WHERE itemName LIKE '%' || :searchQuery || '%' ORDER BY dispatchedAt DESC")
    fun searchDispatches(searchQuery: String): Flow<List<DispatchRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dispatches: List<DispatchRecordEntity>)

    @Query("DELETE FROM dispatch_records")
    suspend fun clearAll()
}
