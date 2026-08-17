package com.example.transaction.data.dao

import androidx.room.*
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE month = :month AND year = :year ORDER BY date DESC")
    fun getTransactionsByMonth(month: Int, year: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND month = :month AND year = :year ORDER BY date DESC")
    fun getFilteredTransactions(accountId: Long, month: Int, year: Int): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET merchant = :newMerchant, category = :newCategory WHERE merchant = :oldMerchant")
    suspend fun renameAllTransactions(oldMerchant: String, newMerchant: String, newCategory: String)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND month = :month AND year = :year")
    fun getTotalExpense(month: Int, year: Int): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND month = :month AND year = :year")
    fun getTotalCredit(month: Int, year: Int): Flow<Double?>
}
