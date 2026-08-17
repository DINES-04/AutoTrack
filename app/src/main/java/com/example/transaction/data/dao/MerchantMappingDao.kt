package com.example.transaction.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.transaction.data.entity.MerchantMapping
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantMappingDao {
    @Query("SELECT * FROM merchant_mappings")
    fun getAllMappings(): Flow<List<MerchantMapping>>

    @Query("SELECT * FROM merchant_mappings WHERE merchantName = :merchantName LIMIT 1")
    suspend fun getMappingForMerchant(merchantName: String): MerchantMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantMapping)

    @Query("DELETE FROM merchant_mappings WHERE merchantName = :merchantName")
    suspend fun deleteMapping(merchantName: String)
}
