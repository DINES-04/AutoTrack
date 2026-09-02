package com.example.transaction.sms

import org.junit.Assert.*
import org.junit.Test

class NumericContextResolutionTest {

    @Test
    fun testAccountNumberDetection() {
        val sms = "A/c *5342 debited"
        val result = SmsParser.parseToResult(sms)
        assertEquals("5342", result.accountIdentifier)
        assertNull(result.amount)
    }

    @Test
    fun testAccountNumberDetectionXX() {
        val sms = "A/C XX5342 debited"
        val result = SmsParser.parseToResult(sms)
        assertEquals("5342", result.accountIdentifier)
    }

    @Test
    fun testReferenceNumberDetectionRRN() {
        val sms = "RRN:212423353897"
        val result = SmsParser.parseToResult(sms)
        assertEquals("212423353897", result.referenceNumber)
        assertNull(result.amount)
    }

    @Test
    fun testReferenceNumberDetectionUTR() {
        val sms = "UTR No. 123456789012"
        val result = SmsParser.parseToResult(sms)
        assertEquals("123456789012", result.referenceNumber)
    }

    @Test
    fun testPhoneNumberDetection() {
        val sms = "SMS BLOCK to 1232123124"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testHelplineNumberDetection() {
        val sms = "Call 1800123456 for help"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testDateDetection() {
        val sms = "On 29/08/2026 transaction occurred"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
        assertEquals(0.90, result.timestampConfidence, 0.001)
    }

    @Test
    fun testTimeDetection() {
        val sms = "At 10:32 AM debited"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testAmountVsBalance() {
        val sms = "Rs.500 debited. Available balance Rs.10,000."
        val result = SmsParser.parseToResult(sms)
        assertEquals(500.0, result.amount!!, 0.001)
    }

    @Test
    fun testAmountVsMultipleNumbers() {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana on 29/08/2026. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        val result = SmsParser.parseToResult(sms)
        assertEquals(20.0, result.amount!!, 0.001)
        assertEquals("5342", result.accountIdentifier)
        assertEquals("212423353897", result.referenceNumber)
        assertEquals("Indian Bank", result.bank)
    }

    @Test
    fun testOTPIdentification() {
        val sms = "Your OTP is 123456"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testCurrencyFormats() {
        val formats = listOf("Rs. 100", "Rs 100", "INR 100", "₹100", "100.00 Rs")
        for (f in formats) {
            val result = SmsParser.parseToResult("$f debited")
            assertEquals("Format $f failed", 100.0, result.amount!!, 0.001)
        }
    }

    @Test
    fun testDebitAndAdded() {
        val sms = "A/c *5342 debited and Rs.250 added to UPI Lite"
        val result = SmsParser.parseToResult(sms)
        assertEquals(250.0, result.amount!!, 0.001)
        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testCreditAndReceived() {
        val sms = "Rs 1000 credited and received in A/c XX1234"
        val result = SmsParser.parseToResult(sms)
        assertEquals(1000.0, result.amount!!, 0.001)
        assertEquals("CREDIT", result.transactionType)
    }

    @Test
    fun testRefund() {
        val sms = "Refund of Rs 200 processed"
        val result = SmsParser.parseToResult(sms)
        assertEquals(200.0, result.amount!!, 0.001)
        assertEquals("CREDIT", result.transactionType)
    }

    @Test
    fun testSalary() {
        val sms = "Salary of Rs 50000 credited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(50000.0, result.amount!!, 0.001)
        assertEquals("CREDIT", result.transactionType)
    }

    @Test
    fun testUpiLiteLoad() {
        val sms = "Debited Rs. 50 from A/c XX1234 for UPI Lite"
        val result = SmsParser.parseToResult(sms)
        assertEquals(50.0, result.amount!!, 0.001)
        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testAtmWithdrawal() {
        val sms = "Cash of Rs 2000 withdrawn from ATM"
        val result = SmsParser.parseToResult(sms)
        assertEquals(2000.0, result.amount!!, 0.001)
        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testCardSpent() {
        val sms = "Spent Rs 1500 on Card XX9999"
        val result = SmsParser.parseToResult(sms)
        assertEquals(1500.0, result.amount!!, 0.001)
        assertEquals("DEBIT", result.transactionType)
    }

    @Test
    fun testEmptySms() {
        val result = SmsParser.parseToResult("")
        assertNull(result.amount)
    }

    @Test
    fun testMalformedSms() {
        val sms = "Hello this is a random message with 1234567890 number"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testMultipleMonetaryValuesWithBalanceFirst() {
        val sms = "Balance Rs 1000. Last txn Rs 50 debited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(50.0, result.amount!!, 0.001)
    }

    @Test
    fun testDateFormats() {
        val dates = listOf("29/08/2026", "29-08-2026", "29.08.2026", "29 Aug 2026", "29-Aug-26")
        for (d in dates) {
            val result = SmsParser.parseToResult("On $d transaction of Rs 100")
            assertEquals(100.0, result.amount!!, 0.001)
        }
    }

    @Test
    fun testTimeFormats() {
        val times = listOf("10:32", "10:32 AM", "22:45", "09:15 PM")
        for (t in times) {
            val result = SmsParser.parseToResult("At $t spent Rs 100")
            assertEquals(100.0, result.amount!!, 0.001)
        }
    }

    @Test
    fun testShortCodes() {
        val sms = "Send SMS to 56767"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testDecimalAmount() {
        val sms = "Rs. 1250.50 debited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(1250.50, result.amount!!, 0.001)
    }

    @Test
    fun testCommaAmount() {
        val sms = "Rs. 1,25,000.00 credited"
        val result = SmsParser.parseToResult(sms)
        assertEquals(125000.0, result.amount!!, 0.001)
    }
    
    @Test
    fun testTxnIdMixed() {
        val sms = "Txn ID 400123456789 debited Rs 10"
        val result = SmsParser.parseToResult(sms)
        assertEquals(10.0, result.amount!!, 0.001)
        assertEquals("400123456789", result.referenceNumber)
    }

    @Test
    fun testAvailableLimit() {
        val sms = "Spent Rs 500. Avl Limit Rs 2000"
        val result = SmsParser.parseToResult(sms)
        assertEquals(500.0, result.amount!!, 0.001)
    }

    @Test
    fun testMinimumBalance() {
        val sms = "Maintain min bal Rs 5000. Your bal is Rs 1000"
        val result = SmsParser.parseToResult(sms)
        assertNull(result.amount)
    }

    @Test
    fun testBenchmark1000Sms() {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana on 29/08/2026. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        
        // Warm up
        repeat(100) { SmsParser.parseToResult(sms) }

        val startTime = System.currentTimeMillis()
        repeat(1000) {
            SmsParser.parseToResult(sms)
        }
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        println("SmsParser.parseToResult 1000 SMS in ${duration}ms")

        val startTimeResolve = System.currentTimeMillis()
        repeat(1000) {
            NumericContextResolver.resolve(sms)
        }
        val endTimeResolve = System.currentTimeMillis()
        println("NumericContextResolver.resolve 1000 SMS in ${endTimeResolve - startTimeResolve}ms")
        
        assertTrue("Benchmark should be under 2000ms", duration < 2000)
    }
}
