package com.example.transaction.classifier

import com.example.transaction.sms.MerchantNormalizer
import org.junit.Assert.*
import org.junit.Test

class MerchantClassifierHardeningTest {

    @Test
    fun testMerchantNormalization() {
        assertEquals("amazon", MerchantNormalizer.normalize("  Amazon  "))
        assertEquals("amazon pay", MerchantNormalizer.normalize("Amazon   Pay"))
        assertEquals("zomato", MerchantNormalizer.normalize("Zomato!"))
        assertEquals("zomato", MerchantNormalizer.normalize("@Zomato"))
        assertEquals("merchant name", MerchantNormalizer.normalize("'Merchant Name'"))
    }

    @Test
    fun testClassificationPriority() {
        // Amazon is in both merchantMap and keywordMap
        val result = MerchantClassifier.classify("Amazon India", "Paid to Amazon")
        assertEquals(MerchantClassifier.SHOPPING, result.category)
        assertEquals("known_merchant", result.method)
        assertTrue(result.confidence >= 0.95)
    }

    @Test
    fun testAliasMatching() {
        // Zomato aliases
        val variations = listOf("Zomato", "Zomato Order", "Zomato Payments", "Zomato@upi")
        for (v in variations) {
            val result = MerchantClassifier.classify(v, "Transaction at $v")
            assertEquals("Failed for $v", MerchantClassifier.FOOD_DINING, result.category)
            assertEquals("known_merchant", result.method)
        }
    }

    @Test
    fun testKeywordDeterministicPriority() {
        // "Google Pay" - could be Shopping (if 'pay' was a broad shopping keyword) or Utilities
        // With current keywords: "Google" is not a keyword, but "Pay" might be.
        // Let's test a real collision if possible. 
        // "Restaurant Supply Store" -> contains 'Restaurant' (Food) and 'Store' (Shopping)
        val result = MerchantClassifier.classify("Restaurant Supply Store", "Paid for supplies")
        // Since Food & Dining is higher in our categoryKeywords list than Shopping, it should win.
        assertEquals(MerchantClassifier.FOOD_DINING, result.category)
    }

    @Test
    fun testAvoidBroadKeywordMatches() {
        // "Bank of Example" should NOT match Bills & Utilities just because of 'bank'
        // In our new implementation, 'bank' is NOT in the global keyword list, only specific terms like 'bank charge'
        val result = MerchantClassifier.classify("Bank of Example", "Transfer")
        assertNotEquals(MerchantClassifier.BILLS_UTILITIES, result.category)
        
        // "Amazon Business" matches 'amazon' (Known Merchant)
        val result2 = MerchantClassifier.classify("Amazon Business", "Spent")
        assertEquals(MerchantClassifier.SHOPPING, result2.category)
    }

    @Test
    fun testConfidenceScores() {
        // Known Merchant
        val res1 = MerchantClassifier.classify("Flipkart", "Order")
        assertEquals(0.95, res1.confidence, 0.01)

        // Keyword Match in Merchant
        val res2 = MerchantClassifier.classify("The Pizza Shop", "Dinner")
        assertEquals(0.85, res2.confidence, 0.01)

        // Keyword Match in Message only
        val res3 = MerchantClassifier.classify("Unknown", "Dinner at restaurant")
        assertEquals(0.75, res3.confidence, 0.01)

        // Fallback
        val res4 = MerchantClassifier.classify("RandomXYZ", "Something")
        assertEquals(0.10, res4.confidence, 0.01)
    }

    @Test
    fun testUnknownMerchantFallback() {
        val result = MerchantClassifier.classify("Unknown", "Some message")
        assertEquals(MerchantClassifier.OTHER, result.category)
        assertEquals("fallback", result.method)
    }

    @Test
    fun testCategoryDeterministicPriorityOrder() {
        // "Bakery and Grocery Store"
        // 'Bakery' -> Food & Dining (index 0)
        // 'Grocery' -> Shopping (index 1)
        val result = MerchantClassifier.classify("The Bakery Grocery", "Spent some money")
        assertEquals(MerchantClassifier.FOOD_DINING, result.category)
    }
}
