package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerProfileDao {
    @Query("SELECT * FROM customer_profile LIMIT 1")
    fun getCustomerProfile(): Flow<CustomerProfileEntity?>

    @Query("SELECT * FROM customer_profile LIMIT 1")
    suspend fun getCustomerProfileDirect(): CustomerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CustomerProfileEntity)

    @Query("DELETE FROM customer_profile")
    suspend fun clearProfile()
}
