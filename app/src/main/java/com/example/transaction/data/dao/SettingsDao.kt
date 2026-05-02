package com.example.transaction.data.dao

import androidx.room.*
import com.example.transaction.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun getSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: SettingsEntity)

    @Query("DELETE FROM transactions")
    suspend fun resetAllTransactions()

    @Query("DELETE FROM accounts")
    suspend fun resetAllAccounts()
}
