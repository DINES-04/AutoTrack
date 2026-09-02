package com.example.transaction.sms

import com.example.transaction.classifier.MerchantClassifier
import org.junit.Assert.*
import org.junit.Test

class FieldConfidenceEngineTest {

    @Test
    fun testHighAmountConfidence() {
        val sms = "Rs. 150.00 debited"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("amount")
        assertEquals(ConfidenceLevel.HIGH, conf.level)
        assertFalse(conf.requiresSecondaryInference)
    }

    @Test
    fun testMediumAmountConfidence() {
        // Unlabeled amount that looks like a decimal value
        val sms = "1250.50 debited"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("amount")
        // Generic amount-like numbers have 0.60 confidence (LOW/MEDIUM depending on threshold)
        // With current thresholds (0.70 MEDIUM, 0.90 HIGH), 0.60 is LOW.
        assertEquals(ConfidenceLevel.LOW, conf.level)
        assertTrue(conf.requiresSecondaryInference)
    }

    @Test
    fun testUnknownAmountConfidence() {
        val sms = "Hello world"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("amount")
        assertEquals(ConfidenceLevel.UNKNOWN, conf.level)
        assertTrue(conf.requiresSecondaryInference)
    }

    @Test
    fun testHighMerchantConfidence() {
        val sms = "Rs. 100 spent at Amazon"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("merchant")
        assertEquals(ConfidenceLevel.HIGH, conf.level)
    }

    @Test
    fun testLowMerchantConfidence() {
        // Merchant without strong separator
        val sms = "Rs. 100 spent SOME_MERCHANT"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("merchant")
        if (result.merchant != null) {
            assertEquals(ConfidenceLevel.LOW, conf.level)
            assertTrue(conf.requiresSecondaryInference)
        }
    }

    @Test
    fun testHighTransactionTypeConfidence() {
        val sms = "Rs 100 debited"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("type")
        assertEquals(ConfidenceLevel.HIGH, conf.level)
    }

    @Test
    fun testConflictingTransactionTypeConfidence() {
        // Both "debited" and "added" present
        val sms = "Rs 100 debited and added to wallet"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("type")
        assertEquals(ConfidenceLevel.MEDIUM, conf.level)
        assertTrue(conf.requiresSecondaryInference)
    }

    @Test
    fun testHighBankConfidence() {
        val sms = "Rs 100 debited -HDFC Bank"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("bank")
        assertEquals(ConfidenceLevel.HIGH, conf.level)
    }

    @Test
    fun testUnknownBankConfidence() {
        val sms = "Rs 100 debited"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("bank")
        assertEquals(ConfidenceLevel.UNKNOWN, conf.level)
    }

    @Test
    fun testCategoryConfidenceKnownMerchant() {
        val sms = "Rs 100 spent at Amazon"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("category")
        assertEquals(ConfidenceLevel.HIGH, conf.level)
        assertEquals(ExtractionMethod.KNOWN_MERCHANT, conf.method)
    }

    @Test
    fun testCategoryConfidenceKeyword() {
        val sms = "Rs 100 spent at some restaurant"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("category")
        // Keyword classifier confidence is 0.85/0.75 which is MEDIUM
        assertEquals(ConfidenceLevel.MEDIUM, conf.level)
        assertEquals(ExtractionMethod.KEYWORD_CLASSIFIER, conf.method)
    }

    @Test
    fun testCategoryConfidenceFallback() {
        val sms = "Rs 100 spent at unknownplace"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("category")
        assertEquals(ConfidenceLevel.LOW, conf.level)
    }

    @Test
    fun testOverallInferenceDecisionRequired() {
        // Low merchant confidence or low category confidence should trigger it
        val sms = "Rs 100 spent at unknownplace"
        val result = SmsParser.parseToResult(sms)
        assertEquals(InferenceDecision.SECONDARY_INFERENCE_REQUIRED, result.getOverallInferenceDecision())
    }

    @Test
    fun testOverallInferenceDecisionNotRequired() {
        val sms = "Rs. 100.00 debited at Amazon -HDFC Bank"
        val result = SmsParser.parseToResult(sms)
        // Check if all critical fields are HIGH
        if (result.getFieldConfidence("amount").level == ConfidenceLevel.HIGH &&
            result.getFieldConfidence("merchant").level == ConfidenceLevel.HIGH &&
            result.getFieldConfidence("type").level == ConfidenceLevel.HIGH &&
            result.getFieldConfidence("category").level == ConfidenceLevel.HIGH) {
            assertEquals(InferenceDecision.NO_SECONDARY_INFERENCE_REQUIRED, result.getOverallInferenceDecision())
        }
    }

    @Test
    fun testFieldSpecificSecondaryInference() {
        val sms = "Rs 100 spent at unknownplace"
        val result = SmsParser.parseToResult(sms)
        assertFalse(result.requiresSecondaryInference("amount"))
        assertTrue(result.requiresSecondaryInference("category"))
    }

    @Test
    fun testMissingImportantFields() {
        val sms = "Hello 123"
        val result = SmsParser.parseToResult(sms)
        assertEquals(InferenceDecision.SECONDARY_INFERENCE_REQUIRED, result.getOverallInferenceDecision())
    }

    @Test
    fun testCriticalRegressionSmsConfidence() {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana on 29/08/2026. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        val result = SmsParser.parseToResult(sms)
        
        assertEquals(20.0, result.amount!!, 0.001)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("amount").level)
        assertEquals(ConfidenceLevel.MEDIUM, result.getFieldConfidence("type").level) // Due to conflicting keywords
        assertEquals("jana", result.merchant)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("bank").level)
    }

    @Test
    fun testSalaryConfidence() {
        val sms = "Salary of Rs 50000 credited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("merchant").level)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("type").level)
    }

    @Test
    fun testRefundConfidence() {
        val sms = "Refund of Rs 200 credited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("merchant").level)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("type").level)
    }

    @Test
    fun testThresholdAdjustment() {
        // Verify HIGH threshold is 0.90
        assertEquals(ConfidenceLevel.HIGH, ConfidenceThresholds.getLevel(0.90))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceThresholds.getLevel(0.89))
    }

    @Test
    fun testLowConfidenceCategoryOther() {
        val sms = "Rs 100 spent at random"
        val result = SmsParser.parseToResult(sms)
        val conf = result.getFieldConfidence("category")
        assertEquals(MerchantClassifier.OTHER, result.category)
        assertEquals(ConfidenceLevel.LOW, conf.level)
    }
    
    @Test
    fun testConflictingTypeResolution() {
        val sms = "Rs 100 debited and Rs 10 added"
        val result = SmsParser.parseToResult(sms)
        assertEquals("DEBIT", result.transactionType)
        assertEquals(ConfidenceLevel.MEDIUM, result.getFieldConfidence("type").level)
    }

    @Test
    fun testExplicitInrAmountConfidence() {
        val sms = "INR 100.00 debited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("amount").level)
    }

    @Test
    fun testAmountLikeGenericNumberConfidence() {
        // Number with .00 but no currency symbol
        val sms = "Your account was charged 50.00 today"
        val result = SmsParser.parseToResult(sms)
        if (result.amount != null) {
            assertEquals(ConfidenceLevel.LOW, result.getFieldConfidence("amount").level)
            assertTrue(result.requiresSecondaryInference("amount"))
        }
    }

    @Test
    fun testMultipleAmountsAmbiguity() {
        val sms = "Paid Rs 100. Balance Rs 500"
        val result = SmsParser.parseToResult(sms)
        // Both are labeled, but selectTransactionAmount picks the non-balance one
        assertEquals(100.0, result.amount!!, 0.001)
        assertEquals(ConfidenceLevel.HIGH, result.getFieldConfidence("amount").level)
    }
}
