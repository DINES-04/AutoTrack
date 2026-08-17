package com.example.transaction.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.transaction.data.dao.TransactionDao
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransactionUpdateTest {
    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private var testAccountId: Long = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
        
        val account = Account(name = "Test Bank", bankKeyword = "TEST")
        db.accountDao().insertAccount(account)
        val accounts = db.accountDao().getActiveAccounts()
        testAccountId = accounts[0].id
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun updateTransaction_updatesCategoryAndMerchant() = runBlocking {
        val transaction = TransactionEntity(
            amount = 100.0,
            type = "DEBIT",
            merchant = "Old Merchant",
            category = "Old Category",
            accountId = testAccountId,
            date = 123456789L,
            month = 1,
            year = 2024
        )
        transactionDao.insertTransaction(transaction)
        
        val inserted = transactionDao.getAllTransactions().first()[0]
        val updated = inserted.copy(merchant = "New Merchant", category = "New Category")
        
        transactionDao.updateTransaction(updated)
        
        val result = transactionDao.getAllTransactions().first()[0]
        assertEquals("New Merchant", result.merchant)
        assertEquals("New Category", result.category)
        assertEquals(inserted.id, result.id)
    }

    @Test
    fun updateTransaction_updatesNote() = runBlocking {
        val transaction = TransactionEntity(
            amount = 100.0,
            type = "DEBIT",
            merchant = "Merchant",
            category = "Category",
            accountId = testAccountId,
            date = 123456789L,
            month = 1,
            year = 2024,
            note = "Old Note"
        )
        transactionDao.insertTransaction(transaction)
        
        val inserted = transactionDao.getAllTransactions().first()[0]
        val updated = inserted.copy(note = "New Note")
        
        transactionDao.updateTransaction(updated)
        
        val result = transactionDao.getAllTransactions().first()[0]
        assertEquals("New Note", result.note)
    }

    @Test
    fun updateTransaction_regression_sameAmountDateAccount() = runBlocking {
        val transaction = TransactionEntity(
            amount = 500.0,
            type = "DEBIT",
            merchant = "Original",
            category = "Other",
            accountId = testAccountId,
            date = 987654321L,
            month = 1,
            year = 2024
        )
        transactionDao.insertTransaction(transaction)
        
        val inserted = transactionDao.getAllTransactions().first()[0]
        val updated = inserted.copy(category = "Shopping")
        
        transactionDao.updateTransaction(updated)
        
        val result = transactionDao.getAllTransactions().first()[0]
        assertEquals("Shopping", result.category)
        assertEquals(500.0, result.amount, 0.0)
    }
    
    @Test
    fun insertDuplicate_isIgnored() = runBlocking {
        val t1 = TransactionEntity(
            amount = 200.0,
            type = "DEBIT",
            merchant = "M1",
            category = "C1",
            accountId = testAccountId,
            date = 111111L,
            month = 1,
            year = 2024
        )
        transactionDao.insertTransaction(t1)
        
        val t2 = TransactionEntity(
            amount = 200.0,
            type = "DEBIT",
            merchant = "M2",
            category = "C2",
            accountId = testAccountId,
            date = 111111L,
            month = 1,
            year = 2024
        )
        transactionDao.insertTransaction(t2)
        
        val list = transactionDao.getAllTransactions().first()
        assertEquals(1, list.size)
        assertEquals("M1", list[0].merchant)
    }

    @Test
    fun updateTransaction_doesNotModifyOtherTransactions() = runBlocking {
        val t1 = TransactionEntity(
            amount = 10.0,
            type = "DEBIT",
            merchant = "M1",
            category = "C1",
            accountId = testAccountId,
            date = 1000L,
            month = 1,
            year = 2024
        )
        val t2 = TransactionEntity(
            amount = 20.0,
            type = "DEBIT",
            merchant = "M2",
            category = "C2",
            accountId = testAccountId,
            date = 2000L,
            month = 1,
            year = 2024
        )
        transactionDao.insertTransaction(t1)
        transactionDao.insertTransaction(t2)
        
        val list = transactionDao.getAllTransactions().first()
        val trans1 = list.find { it.merchant == "M1" }!!
        val trans2 = list.find { it.merchant == "M2" }!!
        
        val updatedT1 = trans1.copy(category = "Updated C1")
        transactionDao.updateTransaction(updatedT1)
        
        val newList = transactionDao.getAllTransactions().first()
        assertEquals("Updated C1", newList.find { it.id == trans1.id }?.category)
        assertEquals("C2", newList.find { it.id == trans2.id }?.category)
    }

    @Test
    fun renameAllTransactions_updatesAllMatches() = runBlocking {
        val t1 = TransactionEntity(
            amount = 100.0,
            type = "DEBIT",
            merchant = "OldName",
            category = "C1",
            accountId = testAccountId,
            date = 1000L,
            month = 1,
            year = 2024
        )
        val t2 = TransactionEntity(
            amount = 200.0,
            type = "DEBIT",
            merchant = "OldName",
            category = "C2",
            accountId = testAccountId,
            date = 2000L,
            month = 1,
            year = 2024
        )
        transactionDao.insertTransaction(t1)
        transactionDao.insertTransaction(t2)
        
        transactionDao.renameAllTransactions("OldName", "NewName", "NewCategory")
        
        val list = transactionDao.getAllTransactions().first()
        assertTrue(list.all { it.merchant == "NewName" && it.category == "NewCategory" })
    }
}
