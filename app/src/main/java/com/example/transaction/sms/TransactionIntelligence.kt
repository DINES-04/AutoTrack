package com.example.transaction.sms

import com.example.transaction.data.dao.MerchantMappingDao

/**
 * Orchestrates the transaction intelligence pipeline:
 * SMS -> Extraction -> Confidence Check -> [Local Model] -> User Mapping -> Final Category
 */
class TransactionIntelligence(
    private val merchantMappingDao: MerchantMappingDao? = null,
    private val localModel: LocalTransactionModel? = null,
    private val useMl: Boolean = true, // Feature flag for prototype stage
    private val lazyLoadModel: Boolean = true // Whether to load model automatically when needed
) {

    /**
     * Processes an SMS message through the full intelligence pipeline.
     */
    suspend fun process(message: String): TransactionExtractionResult {
        // 1. EXTRACTION & INITIAL CLASSIFICATION
        var result = SmsParser.parseToResult(message)

        // 2. CONFIDENCE CHECK & LOCAL MODEL
        if (useMl && result.getOverallInferenceDecision() == InferenceDecision.SECONDARY_INFERENCE_REQUIRED) {
            result = refineWithLocalModel(message, result)
        }

        // 3. USER MAPPING (Highest Priority)
        result = applyUserMapping(result)

        return result
    }

    private suspend fun refineWithLocalModel(
        message: String,
        result: TransactionExtractionResult
    ): TransactionExtractionResult {
        val model = localModel ?: return result
        
        // LAZY LOADING: Load model only if enabled and it hasn't been loaded yet
        if (lazyLoadModel && (model.status == ModelStatus.DISABLED || model.status == ModelStatus.UNAVAILABLE)) {
            model.load()
        }

        if (model.status != ModelStatus.AVAILABLE) return result

        val targetFields = mutableListOf<String>()
        val criticalFields = listOf("amount", "merchant", "type", "category")
        for (field in criticalFields) {
            if (result.requiresSecondaryInference(field)) {
                targetFields.add(field)
            }
        }

        if (targetFields.isEmpty()) return result

        return try {
            val input = TransactionModelInput(message, result, targetFields)
            val modelResult = model.infer(input)
            if (modelResult != null) {
                applyModelResult(result, modelResult)
            } else {
                result
            }
        } catch (e: Exception) {
            // Safe fallback: return original result on model failure
            result
        }
    }

    private fun applyModelResult(
        result: TransactionExtractionResult,
        modelResult: LocalModelResult
    ): TransactionExtractionResult {
        // Validation Layer: Only apply model results for fields that were requested or are uncertain
        var updated = result

        modelResult.amount?.let { modelAmount ->
            if (result.requiresSecondaryInference("amount") && modelResult.amountConfidence != null) {
                // AMOUNT SAFETY: Validate before applying
                if (isValidModelAmount(modelAmount, result)) {
                    updated = updated.copy(
                        amount = modelAmount,
                        amountConfidence = modelResult.amountConfidence,
                        amountMethod = ExtractionMethod.LOCAL_MODEL
                    )
                }
            }
        }

        modelResult.merchant?.let { modelMerchant ->
            if (result.requiresSecondaryInference("merchant") && modelResult.merchantConfidence != null) {
                // MERCHANT SAFETY: Clean and validate
                val cleanedMerchant = cleanMerchantName(modelMerchant)
                if (isValidModelMerchant(cleanedMerchant)) {
                    updated = updated.copy(
                        merchant = cleanedMerchant,
                        merchantConfidence = modelResult.merchantConfidence,
                        merchantMethod = ExtractionMethod.LOCAL_MODEL
                    )
                    // If merchant changed, re-classify category deterministically if model didn't provide one
                    if (modelResult.category == null) {
                        val reClassification = com.example.transaction.classifier.MerchantClassifier.classify(cleanedMerchant, result.merchant ?: "")
                        updated = updated.copy(
                            category = reClassification.category,
                            categoryConfidence = reClassification.confidence,
                            categoryMethod = when (reClassification.method) {
                                "known_merchant" -> ExtractionMethod.KNOWN_MERCHANT
                                "keyword_classifier" -> ExtractionMethod.KEYWORD_CLASSIFIER
                                else -> ExtractionMethod.NONE
                            }
                        )
                    }
                }
            }
        }

        modelResult.transactionType?.let {
            if (result.requiresSecondaryInference("type") && modelResult.transactionTypeConfidence != null) {
                updated = updated.copy(
                    transactionType = it,
                    transactionTypeConfidence = modelResult.transactionTypeConfidence,
                    transactionTypeMethod = ExtractionMethod.LOCAL_MODEL
                )
            }
        }

        modelResult.category?.let {
            if (result.requiresSecondaryInference("category") && modelResult.categoryConfidence != null) {
                updated = updated.copy(
                    category = it,
                    categoryConfidence = modelResult.categoryConfidence,
                    categoryMethod = ExtractionMethod.LOCAL_MODEL
                )
            }
        }
        
        modelResult.bank?.let {
            if (result.requiresSecondaryInference("bank") && modelResult.bankConfidence != null) {
                updated = updated.copy(
                    bank = it,
                    bankConfidence = modelResult.bankConfidence,
                    bankMethod = ExtractionMethod.LOCAL_MODEL
                )
            }
        }

        return updated
    }

    private fun isValidModelAmount(amount: Double, current: TransactionExtractionResult): Boolean {
        // 1. Must be positive
        if (amount <= 0) return false
        
        // 2. Reject if it matches sensitive numeric patterns (Safety Check)
        val amountStr = amount.toLong().toString()
        if (amountStr == current.accountIdentifier) return false
        if (amountStr == current.referenceNumber) return false
        
        // 3. Basic sanity check - extreme values
        if (amount > 10000000) return false 
        
        return true
    }

    private fun cleanMerchantName(name: String): String {
        return name.split(Regex("(?i) on | at | via | from | to | account | a/c | not you|sms block"))[0]
            .trim()
            .removeSuffix(".")
    }

    private fun isValidModelMerchant(name: String): Boolean {
        if (name.isBlank()) return false
        if (name.length < 2) return false
        // Reject if it looks like a number/ID
        if (name.matches(Regex("\\d+"))) return false
        return true
    }

    private suspend fun applyUserMapping(result: TransactionExtractionResult): TransactionExtractionResult {
        val merchant = result.merchant ?: return result
        val dao = merchantMappingDao ?: return result

        val mapping = dao.getMappingForMerchant(merchant.lowercase().trim())
        return if (mapping != null) {
            result.copy(
                category = mapping.category,
                categoryConfidence = 1.0,
                categoryMethod = ExtractionMethod.USER_MAPPING
            )
        } else {
            result
        }
    }
}
