package com.example.transaction.sms

import com.example.transaction.data.dao.MerchantMappingDao
import com.example.transaction.data.entity.MerchantMapping
import com.example.transaction.classifier.MerchantClassifier
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.*
import org.junit.Test

class TransactionIntelligenceTest {

    private class FakeMerchantMappingDao : MerchantMappingDao {
        override fun getAllMappings(): Flow<List<MerchantMapping>> = throw UnsupportedOperationException()
        override suspend fun getMappingForMerchant(merchantName: String): MerchantMapping? {
            return if (merchantName == "unknownmerchant") {
                MerchantMapping(merchantName = "unknownmerchant", category = MerchantClassifier.FOOD_DINING)
            } else {
                null
            }
        }
        override suspend fun insertMapping(mapping: MerchantMapping) {}
        override suspend fun deleteMapping(merchantName: String) {}
    }

    @Test
    fun testPipelineWithUserMapping() = runBlocking {
        val fakeDao = FakeMerchantMappingDao()
        val sms = "Rs. 100 debited at UnknownMerchant"
        
        val intelligence = TransactionIntelligence(fakeDao)
        val result = intelligence.process(sms)
        
        assertEquals(MerchantClassifier.FOOD_DINING, result.category)
        assertEquals(1.0, result.categoryConfidence, 0.001)
        assertEquals(ExtractionMethod.USER_MAPPING, result.categoryMethod)
    }

    @Test
    fun testPipelineWithoutUserMapping() = runBlocking {
        val fakeDao = FakeMerchantMappingDao()
        val sms = "Rs. 100 debited at Amazon"
        
        val intelligence = TransactionIntelligence(fakeDao)
        val result = intelligence.process(sms)
        
        assertEquals(MerchantClassifier.SHOPPING, result.category)
        assertEquals(ExtractionMethod.KNOWN_MERCHANT, result.categoryMethod)
    }
}
