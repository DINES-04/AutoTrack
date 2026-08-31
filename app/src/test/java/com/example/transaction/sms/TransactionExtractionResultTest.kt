package com.example.transaction.sms

import com.example.transaction.classifier.MerchantClassifier
import org.junit.Assert.*
import org.junit.Test

class TransactionExtractionResultTest {

    @Test
    fun testCompleteValidExtraction() {
        val sms = "Rs. 150.00 debited from A/c XX1234 to Amazon on 01-01-24. Ref 400123456789. -HDFC Bank"
        val result = SmsParser.parseToResult(sms)
        
        assertEquals(150.0, result.amount!!, 0.001)
        assertEquals(0.98, result.amountConfidence, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.amountMethod)
        
        assertEquals("Amazon", result.merchant)
        assertEquals(0.90, result.merchantConfidence, 0.001)
        assertEquals(ExtractionMethod.MERCHANT_EXTRACTOR, result.merchantMethod)
        
        assertEquals("DEBIT", result.transactionType)
        assertEquals(0.95, result.transactionTypeConfidence, 0.001)
        assertEquals(ExtractionMethod.CONTEXT_RULE, result.transactionTypeMethod)
        
        assertEquals("HDFC Bank", result.bank)
        assertEquals(0.90, result.bankConfidence, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.bankMethod)
        
        assertEquals("1234", result.accountIdentifier)
        assertEquals(0.95, result.accountIdentifierConfidence, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.accountIdentifierMethod)
        
        assertEquals("400123456789", result.referenceNumber)
        assertEquals(0.95, result.referenceNumberConfidence, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.referenceNumberMethod)
        
        assertEquals(MerchantClassifier.SHOPPING, result.category)
        assertEquals(0.95, result.categoryConfidence, 0.001)
        assertEquals(ExtractionMethod.KNOWN_MERCHANT, result.categoryMethod)

        assertEquals(0.80, result.timestampConfidence, 0.001)
        assertEquals(ExtractionMethod.REGEX, result.timestampMethod)
    }

    @Test
    fun testMissingMerchant() {
        val sms = "Rs. 150.00 debited from A/c XX1234. -HDFC Bank"
        val result = SmsParser.parseToResult(sms)
        
        assertEquals(150.0, result.amount!!, 0.001)
        assertNull(result.merchant)
        assertEquals(0.0, result.merchantConfidence, 0.001)
        assertEquals(ExtractionMethod.NONE, result.merchantMethod)
    }

    @Test
    fun testMissingAmount() {
        val sms = "Your A/c XX1234 was accessed from new device."
        val result = SmsParser.parseToResult(sms)
        
        assertNull(result.amount)
        assertEquals(0.0, result.amountConfidence, 0.001)
        assertEquals(ExtractionMethod.NONE, result.amountMethod)
    }

    @Test
    fun testMissingBank() {
        val sms = "Rs. 150.00 debited from A/c XX1234 to Amazon."
        val result = SmsParser.parseToResult(sms)
        
        assertNull(result.bank)
        assertEquals(0.0, result.bankConfidence, 0.001)
        assertEquals(ExtractionMethod.NONE, result.bankMethod)
    }

    @Test
    fun testMissingAccount() {
        val sms = "Rs. 150.00 debited to Amazon."
        val result = SmsParser.parseToResult(sms)
        
        assertNull(result.accountIdentifier)
        assertEquals(0.0, result.accountIdentifierConfidence, 0.001)
        assertEquals(ExtractionMethod.NONE, result.accountIdentifierMethod)
    }

    @Test
    fun testMissingReference() {
        val sms = "Rs. 150.00 debited to Amazon from A/c XX1234."
        val result = SmsParser.parseToResult(sms)
        
        assertNull(result.referenceNumber)
        assertEquals(0.0, result.referenceNumberConfidence, 0.001)
        assertEquals(ExtractionMethod.NONE, result.referenceNumberMethod)
    }

    @Test
    fun testUnknownTransactionType() {
        // Current implementation defaults to DEBIT if no keywords matched
        val sms = "Rs. 150.00 something to Amazon."
        val result = SmsParser.parseToResult(sms)
        
        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testEmptyUnknownValues() {
        val result = TransactionExtractionResult()
        assertNull(result.amount)
        assertNull(result.merchant)
        assertEquals(ExtractionMethod.NONE, result.amountMethod)
    }

    @Test
    fun testConversionFromExistingParserOutput() {
        val sms = "Rs. 150.00 debited to Amazon."
        val parsed = SmsParser.parse(sms)
        val result = SmsParser.parseToResult(sms)
        
        assertNotNull(parsed)
        assertEquals(parsed!!.amount, result.amount!!, 0.001)
        assertEquals(parsed.merchant, result.merchant ?: "Unknown")
        assertEquals(parsed.type, result.transactionType)
        assertEquals(parsed.category, result.category)
    }
}
