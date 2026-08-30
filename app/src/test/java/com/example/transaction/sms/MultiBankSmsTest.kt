package com.example.transaction.sms

import org.junit.Assert.*
import org.junit.Test

class MultiBankSmsTest {

    @Test
    fun testBug1TrailingSecurityText() {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana on 29/08/2026. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(20.0, parsed!!.amount, 0.001)
        // Bug 1: Should NOT include "Not you?SMS BLOCK to"
        assertEquals("jana", parsed.merchant)
        assertEquals("Indian Bank", parsed.bank)
    }

    @Test
    fun testBug2MerchantPlusDate() {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana on 29/08/2026. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        // Bug 2: Should NOT include "on 29/08/2026"
        assertEquals("jana", parsed!!.merchant)
    }

    @Test
    fun testBug3AccountSuffix() {
        val sms = "A/c *5342 debited and Rs.20.00 added to jana account. RRN:212423353897.Not you?SMS BLOCK to 1232123124-Indian Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        // Bug 3: Should NOT include "account"
        assertEquals("jana", parsed!!.merchant)
    }

    @Test
    fun testSbiFormat() {
        val sms = "Dear Customer, your A/c X1234 has been debited for Rs 1,500.00 on 29-08-26 by transfer to MOBILE RECHARGE. Ref: 12345678. -SBI"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.amount, 0.001)
        assertEquals("DEBIT", parsed.type)
        assertEquals("MOBILE RECHARGE", parsed.merchant)
        assertEquals("SBI", parsed.bank)
    }

    @Test
    fun testHdfcFormat() {
        val sms = "Alert: Rs 2000.00 debited from A/c XX4321 on 29-08-26 to VPA amazon@apl. Info: UPI-AMAZON INDIA-1234. -HDFC Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(2000.0, parsed!!.amount, 0.001)
        assertEquals("amazon@apl", parsed.merchant)
        assertEquals("HDFC Bank", parsed.bank)
    }

    @Test
    fun testIciciFormat() {
        val sms = "i-Safe: Acct XX123 debited with INR 500.00 on 29-Aug-26; Swiggy. Info: CMS/123456789. -ICICI Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("Swiggy", parsed.merchant)
    }

    @Test
    fun testAxisFormat() {
        val sms = "Axis Bank: Rs. 300.00 debited from A/c XX999 at ZOMATO on 29-08-26. Ref: 665544. Not you? Call 1800123456."
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(300.0, parsed!!.amount, 0.001)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun testKotakFormat() {
        val sms = "Rs 100.00 spent on Kotak Bank Debit Card XX1234 at RELIANCE RETAIL on 29-08-26. Avl Bal: Rs 5000. -Kotak Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount, 0.001)
        assertEquals("RELIANCE RETAIL", parsed.merchant)
    }

    @Test
    fun testDiverseDateFormats() {
        val dates = listOf("29/08/2026", "29-08-2026", "29.08.2026", "29 Aug 2026", "August 29, 2026")
        for (date in dates) {
            val sms = "Paid Rs 100 to Merchant on $date"
            val parsed = SmsParser.parse(sms)
            assertEquals("Failed for date $date", "Merchant", parsed?.merchant)
        }
    }

    @Test
    fun testUpiCreditDiverse() {
        val sms = "Rs 500.00 credited to A/c XX1234 from VPA friend@upi. Ref: 998877. -IDFC FIRST Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
        assertEquals("friend@upi", parsed.merchant)
    }

    @Test
    fun testAtmWithdrawal() {
        val sms = "Cash withdrawal of Rs. 5,000.00 from A/c XX111 at ATM Axis. Date: 29/08/26. -Axis Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(5000.0, parsed!!.amount, 0.001)
        assertEquals("ATM Axis", parsed.merchant)
    }

    @Test
    fun testSalaryCredit() {
        val sms = "Salary of INR 60,000.00 credited to A/c XX222 on 01-09-26. -HDFC Bank"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(60000.0, parsed!!.amount, 0.001)
        assertEquals("CREDIT", parsed.type)
        assertEquals("Salary", parsed.merchant)
    }

    @Test
    fun testFailedTransaction() {
        val sms = "Transaction of Rs 500.00 to Zomato failed due to insufficient funds. -Bank"
        // Should we parse failed transactions? Usually NO, but the requirement says "test... failed transaction"
        // If the parser finds an amount and type, it will currently parse it.
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("Zomato", parsed.merchant)
    }

    @Test
    fun testFooterFiltering() {
        val footers = listOf(
            "Contact customer care",
            "Call 1800-123-4567",
            "Report unauthorized txn",
            "Do not share OTP",
            "Helpline: 123",
            "Download our app"
        )
        for (footer in footers) {
            val sms = "Paid Rs 100 to Merchant. $footer"
            val parsed = SmsParser.parse(sms)
            assertEquals("Failed for footer $footer", "Merchant", parsed?.merchant)
        }
    }
}
