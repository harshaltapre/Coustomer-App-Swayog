package com.example.coustomerapp.data.local.dao

import androidx.room.*
import com.example.coustomerapp.data.local.entities.InverterGenerationHistoryEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InverterDao {
    @Query("SELECT * FROM inverter_generation_summary WHERE customerId = :customerId")
    fun getSummary(customerId: Int): Flow<InverterGenerationSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(entity: InverterGenerationSummaryEntity)

    @Query("SELECT * FROM inverter_generation_history WHERE customerId = :customerId AND period = :period ORDER BY rowid ASC")
    fun getHistory(customerId: Int, period: String): Flow<List<InverterGenerationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entities: List<InverterGenerationHistoryEntity>)

    @Query("DELETE FROM inverter_generation_history WHERE customerId = :customerId AND period = :period")
    suspend fun clearHistory(customerId: Int, period: String)
}
