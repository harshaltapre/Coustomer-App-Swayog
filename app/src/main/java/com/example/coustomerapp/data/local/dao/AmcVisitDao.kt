package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.AmcVisit
import kotlinx.coroutines.flow.Flow

@Dao
interface AmcVisitDao {
    @Query("SELECT * FROM amc_visits ORDER BY scheduledDate ASC")
    fun getAllAmcVisits(): Flow<List<AmcVisit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<AmcVisit>)

    @Query("DELETE FROM amc_visits")
    suspend fun clearAll()
}
