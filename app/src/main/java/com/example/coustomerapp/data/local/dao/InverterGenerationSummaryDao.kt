package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InverterGenerationSummaryDao {
    @Query("SELECT * FROM inverter_generation_summary WHERE customerId = :customerId LIMIT 1")
    fun getSummary(customerId: Int): Flow<InverterGenerationSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: InverterGenerationSummaryEntity)

    @Query("DELETE FROM inverter_generation_summary")
    suspend fun clearAll()
}
