package com.example.transaction.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.transaction.data.AppDatabase
import com.example.transaction.data.TransactionRepository
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    
    val allAccounts: Flow<List<Account>>
    val settings: Flow<SettingsEntity?>

    // Dashboard Filters (Default to Current Month/Year)
    private val _dashMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val dashMonth = _dashMonth.asStateFlow()
    private val _dashYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val dashYear = _dashYear.asStateFlow()
    private val _dashAccountId = MutableStateFlow<Long?>(null)
    val dashAccountId = _dashAccountId.asStateFlow()

    // History Filters
    private val _histMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val histMonth = _histMonth.asStateFlow()
    private val _histYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val histYear = _histYear.asStateFlow()
    private val _histAccountId = MutableStateFlow<Long?>(null)
    val histAccountId = _histAccountId.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.accountDao(), db.transactionDao(), db.settingsDao(), db.merchantMappingDao())
        allAccounts = repository.allAccounts
        settings = repository.settings
    }

    // Dashboard Data
    val dashExpense: Flow<Double> = combine(dashMonth, dashYear, dashAccountId) { month, year, accId ->
        if (accId == null) repository.getTotalExpense(month, year)
        else repository.getFilteredTransactions(accId, month, year).map { list -> list.filter { it.type == "DEBIT" }.sumOf { it.amount } }
    }.flatMapLatest { it }.map { it ?: 0.0 }

    val dashCredit: Flow<Double> = combine(dashMonth, dashYear, dashAccountId) { month, year, accId ->
        if (accId == null) repository.getTotalCredit(month, year)
        else repository.getFilteredTransactions(accId, month, year).map { list -> list.filter { it.type == "CREDIT" }.sumOf { it.amount } }
    }.flatMapLatest { it }.map { it ?: 0.0 }

    val lastMonthExpense: Flow<Double> = combine(dashMonth, dashYear) { month, year ->
        val lastMonth = if (month == 1) 12 else month - 1
        val lastYear = if (month == 1) year - 1 else year
        repository.getTotalExpense(lastMonth, lastYear)
    }.flatMapLatest { it }.map { it ?: 0.0 }

    val dashNetBalance: Flow<Double> = combine(dashExpense, dashCredit) { expense, credit ->
        credit - expense
    }

    val remainingBudget: Flow<Double> = combine(settings, dashExpense) { settings, expense ->
        val budget = settings?.monthlyBudget ?: 0.0
        if (budget > 0) budget - expense else 0.0
    }

    // Dashboard Transactions (Filtered by Dash Filters)
    val dashTransactions: Flow<List<TransactionEntity>> = combine(
        dashMonth, dashYear, dashAccountId
    ) { month, year, accountId ->
        if (accountId == null) {
            repository.getTransactionsByMonth(month, year)
        } else {
            repository.getFilteredTransactions(accountId, month, year)
        }
    }.flatMapLatest { it }

    // History Data
    val historyTransactions: Flow<List<TransactionEntity>> = combine(
        histMonth, histYear, histAccountId
    ) { month, year, accountId ->
        if (accountId == null) {
            repository.getTransactionsByMonth(month, year)
        } else {
            repository.getFilteredTransactions(accountId, month, year)
        }
    }.flatMapLatest { it }

    // Analytics Data
    val categoryWiseSpending: Flow<Map<String, Double>> = historyTransactions.map { list ->
        list.filter { it.type == "DEBIT" }.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val merchantWiseSpending: Flow<Map<String, Double>> = historyTransactions.map { list ->
        list.filter { it.type == "DEBIT" }.groupBy { it.merchant }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val accountWiseSpending: Flow<Map<String, Double>> = combine(historyTransactions, allAccounts) { transactions, accounts ->
        transactions.filter { it.type == "DEBIT" }
            .groupBy { it.accountId }
            .mapKeys { entry -> accounts.find { it.id == entry.key }?.name ?: "Unknown" }
            .mapValues { it.value.sumOf { t -> t.amount } }
    }

    // Setters
    fun setDashMonth(month: Int) { _dashMonth.value = month }
    fun setDashYear(year: Int) { _dashYear.value = year }
    fun setDashAccount(accountId: Long?) { _dashAccountId.value = accountId }

    fun setHistMonth(month: Int) { _histMonth.value = month }
    fun setHistYear(year: Int) { _histYear.value = year }
    fun setHistAccount(accountId: Long?) { _histAccountId.value = accountId }

    // Other actions
    fun addAccount(name: String, keyword: String) {
        viewModelScope.launch {
            repository.insertAccount(Account(name = name, bankKeyword = keyword))
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch { repository.updateAccount(account) }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    fun addTransaction(amount: Double, type: String, merchant: String, accountId: Long, category: String = "Others", note: String = "") {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            repository.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    type = type,
                    merchant = merchant,
                    category = category,
                    accountId = accountId,
                    date = System.currentTimeMillis(),
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR),
                    note = note
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            // Ensure month and year are synced with the date
            val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
            val updated = transaction.copy(
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )
            repository.updateTransaction(updated)
        }
    }

    fun renameAllTransactions(oldMerchant: String, newMerchant: String, newCategory: String) {
        viewModelScope.launch {
            val normalizedNew = com.example.transaction.sms.MerchantNormalizer.normalize(newMerchant)
            val normalizedOld = com.example.transaction.sms.MerchantNormalizer.normalize(oldMerchant)
            
            repository.renameAllTransactions(oldMerchant, newMerchant, newCategory)
            repository.insertMerchantMapping(com.example.transaction.data.entity.MerchantMapping(normalizedNew, newCategory, true))
            if (normalizedOld != normalizedNew) {
                repository.insertMerchantMapping(com.example.transaction.data.entity.MerchantMapping(normalizedOld, newCategory, true))
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    fun updateBudget(budget: Double) {
        viewModelScope.launch {
            repository.updateSettings(SettingsEntity(id = 0, monthlyBudget = budget))
        }
    }

    fun resetData() {
        viewModelScope.launch { repository.resetData() }
    }
}
