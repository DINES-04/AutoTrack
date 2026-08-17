package com.example.transaction.classifier

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
        "restaurant" to FOOD_DINING,
        "cafe" to FOOD_DINING,
        
        "uber" to TRAVEL_TRANSPORT,
        "ola" to TRAVEL_TRANSPORT,
        "rapido" to TRAVEL_TRANSPORT,
        "irctc" to TRAVEL_TRANSPORT,
        "metro" to TRAVEL_TRANSPORT,
        "redbus" to TRAVEL_TRANSPORT,
        "makemytrip" to TRAVEL_TRANSPORT,
        
        "indianoil" to FUEL,
        "iocl" to FUEL,
        "bpcl" to FUEL,
        "hpcl" to FUEL,
        "bharat petroleum" to FUEL,
        "petrol" to FUEL,
        "fuel" to FUEL,
        
        "netflix" to ENTERTAINMENT,
        "spotify" to ENTERTAINMENT,
        "hotstar" to ENTERTAINMENT,
        "prime video" to ENTERTAINMENT,
        "bookmyshow" to ENTERTAINMENT,
        "youtube" to ENTERTAINMENT,
        
        "zerodha" to INVESTMENT,
        "groww" to INVESTMENT,
        "upstox" to INVESTMENT,
        
        "electricity" to BILLS_UTILITIES,
        "water bill" to BILLS_UTILITIES,
        "broadband" to BILLS_UTILITIES,
        "airtel" to BILLS_UTILITIES,
        "jio" to BILLS_UTILITIES,
        "vi" to BILLS_UTILITIES,
        "recharge" to BILLS_UTILITIES,
        "bill payment" to BILLS_UTILITIES,
        
        "apollo" to HEALTH,
        "practo" to HEALTH,
        "pharmacy" to HEALTH,
        "medical" to HEALTH,
        "hospital" to HEALTH,
        "clinic" to HEALTH,
        "medicine" to HEALTH,
        
        "udemy" to EDUCATION,
        "coursera" to EDUCATION,
        "college" to EDUCATION,
        "university" to EDUCATION,
        "tuition" to EDUCATION,
        "education" to EDUCATION,
        
        "lic" to INSURANCE,
        "insurance" to INSURANCE,
        "policy premium" to INSURANCE,
        "premium payment" to INSURANCE,
        
        "subscription" to SUBSCRIPTION,
        "membership" to SUBSCRIPTION,
        "recurring" to SUBSCRIPTION,
        
        "bank charge" to BANKING_FINANCE,
        "service charge" to BANKING_FINANCE,
        "annual fee" to BANKING_FINANCE,
        "atm fee" to BANKING_FINANCE,
        "interest" to BANKING_FINANCE,
        "loan" to BANKING_FINANCE,
        "emi" to BANKING_FINANCE,
        "credit card payment" to BANKING_FINANCE
    )

    private val keywordMap = mapOf(
        SHOPPING to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "retail", "grocery", "mart", "store", "reliance"),
        FOOD_DINING to listOf("zomato", "swiggy", "dominos", "pizza", "mcdonald", "kfc", "restaurant", "cafe", "eats", "bakery", "hotel"),
        TRAVEL_TRANSPORT to listOf("uber", "ola", "rapido", "irctc", "metro", "redbus", "makemytrip", "travel", "cab", "taxi", "railway", "flight"),
        FUEL to listOf("indianoil", "iocl", "bpcl", "hpcl", "petrol", "fuel", "diesel", "bharat petroleum"),
        ENTERTAINMENT to listOf("netflix", "spotify", "hotstar", "prime video", "bookmyshow", "youtube", "movie", "cinema", "entertainment"),
        INVESTMENT to listOf("zerodha", "groww", "upstox", "mutual fund", "sip", "nse", "bse", "stock", "investment"),
        BILLS_UTILITIES to listOf("electricity", "eb", "water", "broadband", "airtel", "jio", "vi", "recharge", "bill", "utility", "gas", "postpaid", "payment"),
        HEALTH to listOf("apollo", "practo", "pharmacy", "medical", "hospital", "clinic", "medicine", "health", "doctor"),
        EDUCATION to listOf("udemy", "coursera", "college", "university", "tuition", "education", "school", "fees"),
        INSURANCE to listOf("lic", "insurance", "policy", "premium"),
        BANKING_FINANCE to listOf("bank charge", "service charge", "annual fee", "atm fee", "interest", "loan", "emi", "credit card"),
        SUBSCRIPTION to listOf("subscription", "membership", "recurring"),
        TRANSFER to listOf("transfer", "sent to", "received from"),
        INCOME to listOf("salary", "refund", "cashback", "interest credited")
    )

    fun classify(merchant: String, message: String): ClassificationResult {
        val normalizedMerchant = merchant.lowercase().trim()
        val normalizedMessage = message.lowercase().trim()

        // 2. Known merchant mapping
        for ((known, category) in merchantMap) {
            if (normalizedMerchant.contains(known)) {
                println("Classifier - Merchant: $merchant, Normalized: $normalizedMerchant, Category: $category, Method: merchant_mapping")
                return ClassificationResult(category, "merchant_mapping")
            }
        }

        // 3. Generic category keywords
        for ((category, keywords) in keywordMap) {
            if (keywords.any { normalizedMerchant.contains(it) || normalizedMessage.contains(it) }) {
                println("Classifier - Merchant: $merchant, Normalized: $normalizedMerchant, Category: $category, Method: keyword_classifier")
                return ClassificationResult(category, "keyword_classifier")
            }
        }

        // 4. Other
        println("Classifier - Merchant: $merchant, Normalized: $normalizedMerchant, Category: $OTHER, Method: fallback")
        return ClassificationResult(OTHER, "fallback")
    }
    
    fun getAllBuiltInCategories(): List<String> {
        return listOf(
            SHOPPING, FOOD_DINING, TRAVEL_TRANSPORT, FUEL, BILLS_UTILITIES,
            ENTERTAINMENT, HEALTH, EDUCATION, INVESTMENT, BANKING_FINANCE,
            TRANSFER, INCOME, INSURANCE, SUBSCRIPTION, OTHER
        )
    }
}

data class ClassificationResult(val category: String, val method: String)
