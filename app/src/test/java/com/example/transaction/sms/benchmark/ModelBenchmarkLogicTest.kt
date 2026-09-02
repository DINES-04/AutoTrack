package com.example.transaction.sms.benchmark

import com.example.transaction.sms.TransactionExtractionResult
import com.example.transaction.sms.TransactionIntelligence
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelBenchmarkLogicTest {

    @Test
    fun testFieldMetricCalculation() {
        val benchmark = ModelBenchmark(TransactionIntelligence())
        
        val sample = BenchmarkSample(
            id = "1",
            sms = "Test",
            difficulty = Difficulty.EASY,
            groundTruth = TransactionExtractionResult(amount = 100.0, merchant = "M1")
        )
        val pred = TransactionExtractionResult(amount = 100.0, merchant = "M2")
        
        val metrics = benchmark.calculateFieldMetrics(listOf(sample to pred))
        
        val amountMetrics = metrics.find { it.fieldName == "amount" }!!
        assertEquals(1.0, amountMetrics.accuracy, 0.001)
        
        val merchantMetrics = metrics.find { it.fieldName == "merchant" }!!
        assertEquals(0.0, merchantMetrics.accuracy, 0.001)
    }

    @Test
    fun testAmountFalsePositiveCalculation() {
        val benchmark = ModelBenchmark(TransactionIntelligence())
        
        val sample = BenchmarkSample(
            id = "test",
            sms = "A/c XX1234 debited Rs 500",
            difficulty = Difficulty.EASY,
            groundTruth = TransactionExtractionResult(amount = 500.0, accountIdentifier = "1234"),
            unsafeNumbers = listOf("1234")
        )
        
        val pred = TransactionExtractionResult(amount = 1234.0)
        val fpr = benchmark.calculateAmountFalsePositiveRate(listOf(sample to pred))
        
        assertEquals(1.0, fpr, 0.001)
    }
}
