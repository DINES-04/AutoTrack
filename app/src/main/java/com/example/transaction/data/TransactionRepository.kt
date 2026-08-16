package com.example.transaction.data

import com.example.transaction.data.dao.AccountDao
import com.example.transaction.data.dao.SettingsDao
import com.example.transaction.data.dao.TransactionDao
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val settingsDao: SettingsDao
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val settings: Flow<SettingsEntity?> = settingsDao.getSettings()

    fun getTransactionsByMonth(month: Int, year: Int) = 
        transactionDao.getTransactionsByMonth(month, year)

    fun getFilteredTransactions(accountId: Long, month: Int, year: Int) =
        transactionDao.getFilteredTransactions(accountId, month, year)

    suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    suspend fun renameAllTransactions(oldMerchant: String, newMerchant: String, newCategory: String) =
        transactionDao.renameAllTransactions(oldMerchant, newMerchant, newCategory)

    suspend fun insertAccount(account: Account) =
        accountDao.insertAccount(account)

    suspend fun deleteAccount(account: Account) =
        accountDao.deleteAccount(account)

    suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account)

    fun getTotalExpense(month: Int, year: Int) = transactionDao.getTotalExpense(month, year)
    fun getTotalCredit(month: Int, year: Int) = transactionDao.getTotalCredit(month, year)
    
    suspend fun getActiveAccounts() = accountDao.getActiveAccounts()

    suspend fun updateSettings(settings: SettingsEntity) = settingsDao.updateSettings(settings)
    
    suspend fun resetData() {
        settingsDao.resetAllTransactions()
        settingsDao.resetAllAccounts()
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)
}
