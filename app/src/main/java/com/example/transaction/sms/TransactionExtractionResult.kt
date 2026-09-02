package com.example.transaction.sms

/**
 * Represents the method used to extract or classify a specific field.
 */
enum class ExtractionMethod {
    REGEX,
    CONTEXT_RULE,
    MERCHANT_EXTRACTOR,
    KNOWN_MERCHANT,
    KEYWORD_CLASSIFIER,
    USER_MAPPING,
    LOCAL_MODEL,
    NONE
}

/**
 * Represents the decision for secondary inference (e.g., calling an ML model).
 */
enum class InferenceDecision {
    NO_SECONDARY_INFERENCE_REQUIRED,
    SECONDARY_INFERENCE_REQUIRED
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
) {
    /**
     * Evaluates field-level confidence for critical transaction fields.
     */
    fun getFieldConfidence(fieldName: String): FieldConfidence {
        val score = when (fieldName.lowercase()) {
            "amount" -> amountConfidence
            "merchant" -> merchantConfidence
            "type", "transactiontype" -> transactionTypeConfidence
            "bank" -> bankConfidence
            "category" -> categoryConfidence
            "account" -> accountIdentifierConfidence
            "reference" -> referenceNumberConfidence
            "timestamp" -> timestampConfidence
            else -> 0.0
        }
        val method = when (fieldName.lowercase()) {
            "amount" -> amountMethod
            "merchant" -> merchantMethod
            "type", "transactiontype" -> transactionTypeMethod
            "bank" -> bankMethod
            "category" -> categoryMethod
            "account" -> accountIdentifierMethod
            "reference" -> referenceNumberMethod
            "timestamp" -> timestampMethod
            else -> ExtractionMethod.NONE
        }

        return FieldConfidence(
            score = score,
            level = ConfidenceThresholds.getLevel(score),
            method = method,
            requiresSecondaryInference = ConfidenceThresholds.requiresInference(score)
        )
    }

    /**
     * Determines if the overall extraction result or specific critical fields require secondary inference.
     */
    fun getOverallInferenceDecision(): InferenceDecision {
        val criticalFields = listOf("amount", "merchant", "type", "category")
        val needsInference = criticalFields.any { getFieldConfidence(it).requiresSecondaryInference } ||
                amount == null || merchant == null
        
        return if (needsInference) {
            InferenceDecision.SECONDARY_INFERENCE_REQUIRED
        } else {
            InferenceDecision.NO_SECONDARY_INFERENCE_REQUIRED
        }
    }
    
    /**
     * Checks if a specific field requires secondary inference.
     */
    fun requiresSecondaryInference(fieldName: String): Boolean {
        return getFieldConfidence(fieldName).requiresSecondaryInference
    }
}
