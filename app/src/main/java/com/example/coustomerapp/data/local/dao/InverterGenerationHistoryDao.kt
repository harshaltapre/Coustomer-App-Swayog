package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.InverterGenerationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InverterGenerationHistoryDao {
    @Query("SELECT * FROM inverter_generation_history WHERE customerId = :customerId AND period = :period")
    fun getHistory(customerId: Int, period: String): Flow<List<InverterGenerationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(historyPoints: List<InverterGenerationHistoryEntity>)

    @Query("DELETE FROM inverter_generation_history WHERE customerId = :customerId AND period = :period")
    suspend fun clearHistory(customerId: Int, period: String)
}
