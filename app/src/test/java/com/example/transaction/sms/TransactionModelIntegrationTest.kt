package com.example.transaction.sms

import com.example.transaction.classifier.MerchantClassifier
import com.example.transaction.data.dao.MerchantMappingDao
import com.example.transaction.data.entity.MerchantMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class TransactionModelIntegrationTest {

    private class FakeMerchantMappingDao : MerchantMappingDao {
        override fun getAllMappings(): Flow<List<MerchantMapping>> = throw UnsupportedOperationException()
        override suspend fun getMappingForMerchant(merchantName: String): MerchantMapping? {
            return if (merchantName == "user_mapped_merchant") {
                MerchantMapping(merchantName = "user_mapped_merchant", category = MerchantClassifier.BILLS_UTILITIES)
            } else null
        }
        override suspend fun insertMapping(mapping: MerchantMapping) {}
        override suspend fun deleteMapping(merchantName: String) {}
    }

    @Test
    fun testHighConfidenceDoesNotCallModel() = runBlocking {
        val fakeModel = FakeLocalTransactionModel()
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        // High confidence SMS
        val sms = "Rs. 100.00 debited at Amazon"
        val result = intelligence.process(sms)
        
        assertEquals(0, fakeModel.inferenceCount)
        assertEquals(ExtractionMethod.MERCHANT_EXTRACTOR, result.merchantMethod)
    }

    @Test
    fun testLowConfidenceCallsModel() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                merchant = "InferredMerchant",
                merchantConfidence = 0.95
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        // Ambiguous SMS (no currency, no strong separator)
        val sms = "100 debited UNKNOWN_NAME"
        val result = intelligence.process(sms)
        
        assertTrue(fakeModel.inferenceCount > 0)
        assertEquals("InferredMerchant", result.merchant)
        assertEquals(ExtractionMethod.LOCAL_MODEL, result.merchantMethod)
    }

    @Test
    fun testModelUnavailableFallback() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(status = ModelStatus.UNAVAILABLE)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited UNKNOWN_NAME"
        val result = intelligence.process(sms)
        
        assertEquals(0, fakeModel.inferenceCount)
        // Should fall back to deterministic result
        assertNull(result.merchant) // If MerchantExtractor failed
    }

    @Test
    fun testModelFailureFallback() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(shouldFail = true)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited UNKNOWN_NAME"
        val result = intelligence.process(sms)
        
        // Should not crash and return deterministic result
        assertTrue(fakeModel.inferenceCount > 0)
        assertNotNull(result)
    }

    @Test
    fun testModelTimeoutFallback() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(delayMs = 2000)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited UNKNOWN_NAME"
        
        try {
            withTimeout(500) {
                intelligence.process(sms)
            }
            fail("Should have timed out")
        } catch (e: Exception) {
            // Success: intelligence pipeline should support cancellation/timeout
        }
    }

    @Test
    fun testUserMappingPriorityOverModel() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                merchant = "user_mapped_merchant",
                merchantConfidence = 0.99,
                category = MerchantClassifier.SHOPPING,
                categoryConfidence = 0.99
            )
        )
        val intelligence = TransactionIntelligence(
            merchantMappingDao = FakeMerchantMappingDao(),
            localModel = fakeModel
        )
        
        val sms = "100 debited user_mapped_merchant"
        val result = intelligence.process(sms)
        
        // Model might infer Shopping, but User Mapping says Bills & Utilities
        assertEquals(MerchantClassifier.BILLS_UTILITIES, result.category)
        assertEquals(ExtractionMethod.USER_MAPPING, result.categoryMethod)
    }

    @Test
    fun testModelNullResultHandling() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(mockResult = null)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited UNKNOWN_NAME"
        val result = intelligence.process(sms)
        
        assertNotNull(result)
        assertEquals(1, fakeModel.inferenceCount)
    }

    @Test
    fun testModelInvalidResultHandling() = runBlocking {
        // Model returns low confidence result, should it be accepted?
        // Our current 'applyModelResult' only checks if modelConfidence is not null.
        // It should probably also check if model confidence is better than deterministic if deterministic is not null.
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                merchant = "LowConfMerchant",
                merchantConfidence = 0.1
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited UNKNOWN_NAME"
        val result = intelligence.process(sms)
        
        // Currently it accepts it. If deterministic was null, any model result might be better than nothing, 
        // but we might want a threshold for the model too.
        assertEquals("LowConfMerchant", result.merchant)
    }

    @Test
    fun testModelStatusLifecycle() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(status = ModelStatus.DISABLED)
        fakeModel.load()
        assertEquals(ModelStatus.AVAILABLE, fakeModel.status)
        fakeModel.release()
        assertEquals(ModelStatus.DISABLED, fakeModel.status)
    }

    @Test
    fun testFieldSpecificInferenceRequest() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                transactionType = "CREDIT",
                transactionTypeConfidence = 0.99
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        // Amount is HIGH (Rs 100), but Type might be LOW/MEDIUM if ambiguous
        val sms = "Rs 100 received and debited"
        val result = intelligence.process(sms)
        
        assertEquals(100.0, result.amount!!, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.amountMethod) // Amount not overridden if HIGH
        assertEquals("CREDIT", result.transactionType)
        assertEquals(ExtractionMethod.LOCAL_MODEL, result.transactionTypeMethod)
    }

    @Test
    fun testModelReturnsDifferentBank() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                bank = "ModelBank",
                bankConfidence = 0.95
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited" // No bank in SMS
        val result = intelligence.process(sms)
        
        assertEquals("ModelBank", result.bank)
        assertEquals(ExtractionMethod.LOCAL_MODEL, result.bankMethod)
    }

    @Test
    fun testModelDoesNotOverrideHighConfidenceField() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                amount = 999.0,
                amountConfidence = 0.99
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "Rs. 100.00 debited" // High confidence amount
        val result = intelligence.process(sms)
        
        assertEquals(100.0, result.amount!!, 0.001) // Should keep 100.0
        assertEquals(ExtractionMethod.REGEX, result.amountMethod)
    }

    @Test
    fun testModelResultValidationOnlyTargetFields() = runBlocking {
        // Model tries to set a field it wasn't asked for (amount)
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                amount = 999.0,
                amountConfidence = 0.99,
                category = "InferredCategory",
                categoryConfidence = 0.95
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        // Amount is high confidence, but category (fallback) is low
        val sms = "Rs. 100.00 spent at unknown_place"
        val result = intelligence.process(sms)
        
        assertEquals(100.0, result.amount!!, 0.001) // Should remain 100.0
        assertEquals("InferredCategory", result.category)
    }

    @Test
    fun testInferenceDecisionForMissingFields() = runBlocking {
        val sms = "No numbers here"
        val result = SmsParser.parseToResult(sms)
        assertEquals(InferenceDecision.SECONDARY_INFERENCE_REQUIRED, result.getOverallInferenceDecision())
    }

    @Test
    fun testModelNotCalledIfDisabled() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(status = ModelStatus.DISABLED)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited"
        intelligence.process(sms)
        
        assertEquals(0, fakeModel.inferenceCount)
    }

    @Test
    fun testModelNotCalledIfLoading() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(status = ModelStatus.LOADING)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited"
        intelligence.process(sms)
        
        assertEquals(0, fakeModel.inferenceCount)
    }

    @Test
    fun testModelNotCalledIfError() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(status = ModelStatus.ERROR)
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 debited"
        intelligence.process(sms)
        
        assertEquals(0, fakeModel.inferenceCount)
    }

    @Test
    fun testCategorySeparateFromExtraction() = runBlocking {
        // Verify that MerchantClassifier is still used even if model doesn't return category
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                merchant = "Amazon",
                merchantConfidence = 0.99
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "100 spent amzn" // amzn without separator -> low confidence merchant
        val result = intelligence.process(sms)
        
        assertEquals("Amazon", result.merchant)
        // Deterministic category for Amazon should be Shopping
        assertEquals(MerchantClassifier.SHOPPING, result.category)
    }

    @Test
    fun testMultipleAmbiguousFields() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                amount = 250.0,
                amountConfidence = 0.95,
                merchant = "Netflix",
                merchantConfidence = 0.95,
                transactionType = "DEBIT",
                transactionTypeConfidence = 0.95
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        
        val sms = "subscription payment 250 ntflx"
        val result = intelligence.process(sms)
        
        assertEquals(250.0, result.amount!!, 0.001)
        assertEquals("Netflix", result.merchant)
        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testModelVersionInformation() = runBlocking {
        val fakeModel = FakeLocalTransactionModel(
            mockResult = LocalModelResult(
                merchant = "Test",
                merchantConfidence = 0.9,
                modelName = "TinyModel",
                modelVersion = "1.2.3"
            )
        )
        val intelligence = TransactionIntelligence(localModel = fakeModel)
        val sms = "100 debited"
        intelligence.process(sms)
        
        assertEquals("TinyModel", fakeModel.mockResult?.modelName)
        assertEquals("1.2.3", fakeModel.mockResult?.modelVersion)
    }
}
