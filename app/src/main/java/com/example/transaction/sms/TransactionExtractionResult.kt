package com.example.transaction.sms

/**
 * Represents the method used to extract or classify a specific field.
 */
enum class ExtractionMethod {
    /** Extracted using regular expression patterns. */
    REGEX,
    
    /** Extracted based on surrounding context rules. */
    CONTEXT_RULE,
    
    /** Extracted by the dedicated merchant extraction logic. */
    MERCHANT_EXTRACTOR,
    
    /** Matched against a known list of high-confidence merchants. */
    KNOWN_MERCHANT,
    
    /** Classified based on keyword matching. */
    KEYWORD_CLASSIFIER,
    
    /** Overridden by a user-defined mapping. */
    USER_MAPPING,
    
    /** Extracted by a local machine learning model (Future Use). */
    LOCAL_MODEL,
    
    /** No extraction method applied. */
    NONE
}

/**
 * A structured result containing all information extracted from a single SMS message.
 * This acts as a foundation for the offline transaction intelligence layer.
 */
data class TransactionExtractionResult(
    val amount: Double? = null,
    val amountConfidence: Double = 0.0,
    val amountMethod: ExtractionMethod = ExtractionMethod.NONE,

    val merchant: String? = null,
    val merchantConfidence: Double = 0.0,
    val merchantMethod: ExtractionMethod = ExtractionMethod.NONE,

    val transactionType: String? = null,
    val transactionTypeConfidence: Double = 0.0,
    val transactionTypeMethod: ExtractionMethod = ExtractionMethod.NONE,

    val bank: String? = null,
    val bankConfidence: Double = 0.0,
    val bankMethod: ExtractionMethod = ExtractionMethod.NONE,

    val accountIdentifier: String? = null,
    val accountIdentifierConfidence: Double = 0.0,
    val accountIdentifierMethod: ExtractionMethod = ExtractionMethod.NONE,

    val referenceNumber: String? = null,
    val referenceNumberConfidence: Double = 0.0,
    val referenceNumberMethod: ExtractionMethod = ExtractionMethod.NONE,

    val timestamp: Long? = null,
    val timestampConfidence: Double = 0.0,
    val timestampMethod: ExtractionMethod = ExtractionMethod.NONE,

    val category: String? = null,
    val categoryConfidence: Double = 0.0,
    val categoryMethod: ExtractionMethod = ExtractionMethod.NONE
)
