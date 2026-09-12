package com.example.transaction.sms

import com.example.transaction.data.dao.MerchantMappingDao
import com.example.transaction.data.entity.MerchantMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
class MLSafetyTest {

    @Test
    fun testAmountSafetyProtection() = runBlocking {
        // SMS where deterministic finds amount correctly, but model might return account number
        val sms = "A/c *5342 debited Rs. 100"
        
        // Mock model that returns the account identifier as amount (ADVERSARIAL BEHAVIOR)
        val maliciousModel = object : LocalTransactionModel {
            override val status = ModelStatus.AVAILABLE
            override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
                return LocalModelResult(amount = 5342.0, amountConfidence = 0.99)
            }
            override suspend fun load() {}
            override fun release() {}
        }

        val intelligence = TransactionIntelligence(localModel = maliciousModel, useMl = true)
        val result = intelligence.process(sms)

        // The high-confidence protection should reject 5342 and keep the correctly parsed amount or null
        assertTrue("Model should not overwrite amount with account number", result.amount != 5342.0)
    }

    @Test
    fun testUserMappingPriority() = runBlocking {
        val sms = "Paid Rs. 500 to Amazon"
        
        val fakeDao = object : MerchantMappingDao {
            override fun getAllMappings(): Flow<List<MerchantMapping>> = throw UnsupportedOperationException()
            override suspend fun getMappingForMerchant(merchantName: String): MerchantMapping? {
                return if (merchantName == "amazon") MerchantMapping("amazon", "BUSINESS") else null
            }
            override suspend fun insertMapping(mapping: MerchantMapping) {}
            override suspend fun deleteMapping(merchantName: String) {}
        }

        // Model returns SHOPPING
        val model = object : LocalTransactionModel {
            override val status = ModelStatus.AVAILABLE
            override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
                return LocalModelResult(category = "SHOPPING", categoryConfidence = 0.99)
            }
            override suspend fun load() {}
            override fun release() {}
        }

        val intelligence = TransactionIntelligence(fakeDao, model, useMl = true)
        val result = intelligence.process(sms)

        // User mapping must win
        assertEquals("BUSINESS", result.category)
        assertEquals(ExtractionMethod.USER_MAPPING, result.categoryMethod)
    }

    @Test
    fun testHighConfidenceAmountProtection() = runBlocking {
        val sms = "Rs. 100.00 debited" // High confidence deterministic
        
        val maliciousModel = object : LocalTransactionModel {
            override val status = ModelStatus.AVAILABLE
            override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
                return LocalModelResult(amount = 999.0, amountConfidence = 0.99)
            }
            override suspend fun load() {}
            override fun release() {}
        }

        val intelligence = TransactionIntelligence(localModel = maliciousModel, useMl = true)
        val result = intelligence.process(sms)

        assertEquals(100.0, result.amount!!, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.amountMethod)
    }

    @Test
    fun testRRNAsAmountProtection() = runBlocking {
        val sms = "A/c *1234 debited Rs 50. Ref: 212423353897"
        
        val model = object : LocalTransactionModel {
            override val status = ModelStatus.AVAILABLE
            override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
                // Model incorrectly picks RRN
                return LocalModelResult(amount = 212423353897.0, amountConfidence = 0.99)
            }
            override suspend fun load() {}
            override fun release() {}
        }

        val intelligence = TransactionIntelligence(localModel = model, useMl = true)
        val result = intelligence.process(sms)

        assertTrue("Should reject RRN as amount", result.amount != 212423353897.0)
    }

    @Test
    fun testConflictingTypeResolution() = runBlocking {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana"
        
        val model = object : LocalTransactionModel {
            override val status = ModelStatus.AVAILABLE
            override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
                return LocalModelResult(transactionType = "DEBIT", transactionTypeConfidence = 0.95)
            }
            override suspend fun load() {}
            override fun release() {}
        }

        val intelligence = TransactionIntelligence(localModel = model, useMl = true)
        val result = intelligence.process(sms)

        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testConcurrencySafety() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val model = BertTinyLocalModel(context)
        val intelligence = TransactionIntelligence(localModel = model, useMl = true)
        
        // Rapid fire ambiguous SMS
        val jobs = mutableListOf<Deferred<TransactionExtractionResult>>()
        repeat(10) { i ->
            jobs.add(async {
                intelligence.process("Ambiguous message $i")
            })
        }
        
        val results = jobs.awaitAll()
        assertEquals(10, results.size)
    }

    @Test
    fun testModelTimeoutFallback(): Unit = runBlocking {
        val slowModel = object : LocalTransactionModel {
            override val status = ModelStatus.AVAILABLE
            override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
                delay(2000)
                return LocalModelResult(merchant = "Slow")
            }
            override suspend fun load() {}
            override fun release() {}
        }

        val intelligence = TransactionIntelligence(localModel = slowModel, useMl = true)
        
        val result = kotlinx.coroutines.withTimeoutOrNull(500) {
            intelligence.process("Ambiguous")
        }
        
        assertNull("Pipeline should be cancellable by external timeout", result)
    }
}
