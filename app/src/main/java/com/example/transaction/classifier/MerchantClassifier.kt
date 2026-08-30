package com.example.transaction.classifier

import com.example.transaction.sms.MerchantNormalizer

object MerchantClassifier {
    
    const val SHOPPING = "Shopping"
    const val FOOD_DINING = "Food & Dining"
    const val TRAVEL_TRANSPORT = "Travel & Transport"
    const val FUEL = "Fuel"
    const val BILLS_UTILITIES = "Bills & Utilities"
    const val ENTERTAINMENT = "Entertainment"
    const val HEALTH = "Health"
    const val EDUCATION = "Education"
    const val INVESTMENT = "Investment"
    const val BANKING_FINANCE = "Banking & Finance"
    const val TRANSFER = "Transfer"
    const val INCOME = "Income"
    const val INSURANCE = "Insurance"
    const val SUBSCRIPTION = "Subscription"
    const val OTHER = "Other"

    // High-confidence exact merchant mappings (Lowercased keys)
    private val merchantMap = mapOf(
        "amazon" to SHOPPING,
        "amazon pay" to SHOPPING,
        "flipkart" to SHOPPING,
        "myntra" to SHOPPING,
        "ajio" to SHOPPING,
        "meesho" to SHOPPING,
        "reliance retail" to SHOPPING,
        "zomato" to FOOD_DINING,
        "swiggy" to FOOD_DINING,
        "dominos" to FOOD_DINING,
        "pizza hut" to FOOD_DINING,
        "mcdonald" to FOOD_DINING,
        "kfc" to FOOD_DINING,
        "uber" to TRAVEL_TRANSPORT,
        "ola" to TRAVEL_TRANSPORT,
        "rapido" to TRAVEL_TRANSPORT,
        "irctc" to TRAVEL_TRANSPORT,
        "indianoil" to FUEL,
        "iocl" to FUEL,
        "bpcl" to FUEL,
        "hpcl" to FUEL,
        "netflix" to ENTERTAINMENT,
        "spotify" to ENTERTAINMENT,
        "hotstar" to ENTERTAINMENT,
        "zerodha" to INVESTMENT,
        "groww" to INVESTMENT,
        "airtel" to BILLS_UTILITIES,
        "jio" to BILLS_UTILITIES,
        "vi" to BILLS_UTILITIES,
        "lic" to INSURANCE
    )

    // Keywords with associated categories and priority
    // Ordered by specificity (deterministic priority)
    private val categoryKeywords = listOf(
        CategoryKeywords(FOOD_DINING, listOf("restaurant", "cafe", "bakery", "eats", "hotel", "dining", "pizza", "burger", "coffee", "day")),
        CategoryKeywords(SHOPPING, listOf("grocery", "mart", "store", "retail", "supermarket", "mall", "fashion")),
        CategoryKeywords(TRAVEL_TRANSPORT, listOf("travel", "cab", "taxi", "railway", "flight", "metro", "bus", "trip")),
        CategoryKeywords(FUEL, listOf("petrol", "fuel", "diesel", "bharat petroleum")),
        CategoryKeywords(BILLS_UTILITIES, listOf("electricity", "eb", "water", "broadband", "recharge", "utility", "gas", "postpaid")),
        CategoryKeywords(ENTERTAINMENT, listOf("movie", "cinema", "prime video", "bookmyshow", "youtube", "game", "gaming")),
        CategoryKeywords(HEALTH, listOf("apollo", "practo", "pharmacy", "medical", "hospital", "clinic", "medicine", "health", "doctor")),
        CategoryKeywords(EDUCATION, listOf("udemy", "coursera", "college", "university", "tuition", "school", "fees")),
        CategoryKeywords(BANKING_FINANCE, listOf("bank charge", "service charge", "annual fee", "atm fee", "interest", "loan", "emi", "credit card")),
        CategoryKeywords(INVESTMENT, listOf("mutual fund", "sip", "nse", "bse", "stock")),
        CategoryKeywords(INCOME, listOf("salary", "cashback", "interest credited", "dividend")),
        CategoryKeywords(TRANSFER, listOf("transfer", "sent to", "received from"))
    )

    fun classify(merchant: String, message: String): ClassificationResult {
        val normalizedMerchant = MerchantNormalizer.normalize(merchant)
        val normalizedMessage = message.lowercase()

        // 1. Exact/Partial Known Merchant Mapping (Confidence: 0.95+)
        for ((known, category) in merchantMap) {
            if (normalizedMerchant.contains(known)) {
                return ClassificationResult(category, "known_merchant", 0.95, known)
            }
        }

        // 2. Keyword Classification (Confidence: 0.80)
        // Iterates through list to ensure deterministic priority
        for (ck in categoryKeywords) {
            if (ck.keywords.any { normalizedMerchant.contains(it) || normalizedMessage.contains(it) }) {
                // If matched in merchant name, higher confidence
                val confidence = if (ck.keywords.any { normalizedMerchant.contains(it) }) 0.85 else 0.75
                val matchedKeyword = ck.keywords.first { normalizedMerchant.contains(it) || normalizedMessage.contains(it) }
                return ClassificationResult(ck.category, "keyword_classifier", confidence, matchedKeyword)
            }
        }

        // 3. Fallback (Confidence: Low)
        return ClassificationResult(OTHER, "fallback", 0.10)
    }
    
    fun getAllBuiltInCategories(): List<String> {
        return listOf(
            SHOPPING, FOOD_DINING, TRAVEL_TRANSPORT, FUEL, BILLS_UTILITIES,
            ENTERTAINMENT, HEALTH, EDUCATION, INVESTMENT, BANKING_FINANCE,
            TRANSFER, INCOME, INSURANCE, SUBSCRIPTION, OTHER
        )
    }

    private data class CategoryKeywords(val category: String, val keywords: List<String>)
}

data class ClassificationResult(
    val category: String, 
    val method: String, 
    val confidence: Double = 0.0,
    val matchedTerm: String? = null
)
