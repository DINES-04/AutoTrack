package com.example.transaction.sms

import com.example.transaction.classifier.MerchantClassifier
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class SmsParserTest {

    @Test
    fun testUpiDebit() {
        val sms = "Rs. 150.00 debited from A/c XX1234 to VPA merchant@upi on 01-01-24. Ref 400123456789."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(150.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("merchant@upi", parsed.merchant)
    }

    @Test
    fun testUpiCredit() {
        val sms = "Rs. 500.00 credited to A/c XX1234 from VPA sender@upi on 02-01-24. Ref 400123456790."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
        assertEquals("sender@upi", parsed.merchant)
    }

    @Test
    fun testBankDebit() {
        val sms = "Your A/c XX4321 is debited for Rs 2500.00 by Cafe Coffee Day. Ref: 123456."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(2500.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("Cafe Coffee Day", parsed.merchant) 
    }

    @Test
    fun testBankCredit() {
        val sms = "Dear Customer, your A/c XX4321 has been credited with INR 45,000.00 on 01-Jan-24 towards Salary. Ref 987654321."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(45000.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
    }

    @Test
    fun testCardPayment() {
        val sms = "Alert: You have spent Rs.1,250.50 on your Credit Card XX9999 at AMAZON INDIA on 05-JAN-24."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(1250.5, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("AMAZON INDIA", parsed.merchant)
        assertEquals(MerchantClassifier.SHOPPING, parsed.category)
    }

    @Test
    fun testAtmWithdrawal() {
        val sms = "Cash withdrawal of Rs. 2,000.00 from A/c XX1234 at ATM. Date: 06/01/24. Balance: Rs. 10,500."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(2000.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
    }

    @Test
    fun testRefund() {
        val sms = "Refund of Rs. 199.00 credited to your A/c XX1234 for transaction on Zomato. Ref: 112233."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(199.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
        assertEquals("Zomato", parsed.merchant)
    }

    // --- PHASE 1D.5 REGRESSION TESTS ---

    @Test
    fun testUpiLiteWalletLoad() {
        val sms = "A/c *5342 debited and Rs.20.00 added to your UPI Lite on icici bank google App. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(20.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("icici bank google App", parsed.merchant)
        assertEquals("Indian Bank", parsed.bank)
    }

    @Test
    fun testAccountNoVsAmount() {
        val sms = "A/c *1234 debited Rs.500 to Amazon"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("Amazon", parsed.merchant)
    }

    @Test
    fun testBankCreditSalary() {
        val sms = "A/c *5678 credited Rs.1000 from Salary"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(1000.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
    }

    @Test
    fun testDebitConflictingKeywords() {
        val sms = "A/c *4321 debited and Rs.250 added to UPI Lite on Google Pay"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("Google Pay", parsed.merchant)
    }

    @Test
    fun testDebitForMerchant() {
        val sms = "Rs.750.00 debited for Cafe Coffee Day"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(750.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("Cafe Coffee Day", parsed.merchant)
    }

    @Test
    fun testCreditFromCompany() {
        val sms = "Rs.1000 credited from ABC Company"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(1000.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
        assertEquals("ABC Company", parsed.merchant)
    }

    @Test
    fun testReferenceNoVsAmount() {
        val sms = "Paid Rs 500 to Merchant. RRN:123456789012"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertNull(SmsParser.parse("RRN:123456789012"))
    }

    @Test
    fun testAccountNoOnly() {
        val sms = "A/c *5342 debited"
        val parsed = SmsParser.parse(sms)
        assertNull(parsed)
    }

    @Test
    fun testPhoneNoVsAmount() {
        val sms = "Spent Rs 100. SMS BLOCK to 1232123124"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount, 0.001)
        assertNull(SmsParser.parse("SMS BLOCK to 1232123124"))
    }

    @Test
    fun testRefundFromAmazon() {
        val sms = "Refund of Rs.500 from Amazon"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
        assertEquals("Amazon", parsed.merchant)
    }

    // --- EDGE CASE TESTS ---

    @Test
    fun testEmptySms() {
        assertNull(SmsParser.parse(""))
    }

    @Test
    fun testMalformedSms() {
        val sms = "Your OTP for login is 123456. Do not share it with anyone."
        val parsed = SmsParser.parse(sms)
        assertNull(parsed)
    }

    @Test
    fun testIncompleteSms() {
        val sms = "Rs 100 debited"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount, 0.001)
    }

    @Test
    fun testUnexpectedWhitespace() {
        val sms = "  Rs   500   paid  to  MerchantName  "
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("MerchantName", parsed.merchant)
    }

    @Test
    fun testCaseVariations() {
        val sms = "RS 100 DEBITED BY RELIANCE"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("RELIANCE", parsed.merchant)
    }

    @Test
    fun testCommaFormattedAmount() {
        val sms = "₹1,25,000.50 debited from account."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(125000.5, parsed!!.amount, 0.001)
    }

    @Test
    fun testAmountWithSuffix() {
        val sms = "100.00 Rs debited."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount, 0.001)
    }

    @Test
    fun testParsingPerformance() {
        val syntheticSmsList = List(1000) { index ->
            "Rs. ${index + 1}.00 debited from A/c XX1234 to Merchant_$index on 01-01-24. Ref ${400000000000L + index}."
        }
        
        val time = measureTimeMillis {
            syntheticSmsList.forEach { sms ->
                SmsParser.parse(sms)
            }
        }
        
        println("Processed 1000 SMS in $time ms")
        assertTrue("Parsing 1000 messages should be fast", time < 1000)
    }
}
