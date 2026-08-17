package com.example.transaction.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantClassifierTest {

    @Test
    fun testShoppingCategory() {
        val result = MerchantClassifier.classify("Amazon", "Paid Rs 500 to Amazon")
        assertEquals(MerchantClassifier.SHOPPING, result.category)
        assertEquals("merchant_mapping", result.method)
    }

    @Test
    fun testFoodCategory() {
        val result1 = MerchantClassifier.classify("Zomato", "Paid to Zomato")
        assertEquals(MerchantClassifier.FOOD_DINING, result1.category)
        
        val result2 = MerchantClassifier.classify("Swiggy", "Paid to Swiggy")
        assertEquals(MerchantClassifier.FOOD_DINING, result2.category)
    }

    @Test
    fun testTravelCategory() {
        val result = MerchantClassifier.classify("Uber", "Uber trip")
        assertEquals(MerchantClassifier.TRAVEL_TRANSPORT, result.category)
    }

    @Test
    fun testFuelCategory() {
        val result = MerchantClassifier.classify("IndianOil", "Fuel at IndianOil")
        assertEquals(MerchantClassifier.FUEL, result.category)
    }

    @Test
    fun testEntertainmentCategory() {
        val result = MerchantClassifier.classify("Netflix", "Netflix subscription")
        assertEquals(MerchantClassifier.ENTERTAINMENT, result.category)
    }

    @Test
    fun testInvestmentCategory() {
        val result = MerchantClassifier.classify("Zerodha", "Fund transfer to Zerodha")
        assertEquals(MerchantClassifier.INVESTMENT, result.category)
    }

    @Test
    fun testUnknownMerchant() {
        val result = MerchantClassifier.classify("Unknown Corp", "Some random message")
        assertEquals(MerchantClassifier.OTHER, result.category)
        assertEquals("fallback", result.method)
    }

    @Test
    fun testCaseInsensitive() {
        val result = MerchantClassifier.classify("AMAZON", "AMAZON PAY INDIA")
        assertEquals(MerchantClassifier.SHOPPING, result.category)
    }

    @Test
    fun testKeywordMatching() {
        val result = MerchantClassifier.classify("Random Shop", "Purchase at grocery store")
        assertEquals(MerchantClassifier.SHOPPING, result.category)
        assertEquals("keyword_classifier", result.method)
    }
}
