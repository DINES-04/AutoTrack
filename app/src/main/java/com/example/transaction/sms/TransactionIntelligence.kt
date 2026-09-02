package com.example.transaction.sms

import com.example.transaction.data.dao.MerchantMappingDao

/**
 * Orchestrates the transaction intelligence pipeline:
 * SMS -> Extraction -> Confidence Check -> [Local Model] -> User Mapping -> Final Category
 */
class TransactionIntelligence(
    private val merchantMappingDao: MerchantMappingDao? = null,
    private val localModel: LocalTransactionModel? = null
) {

    /**
     * Processes an SMS message through the full intelligence pipeline.
     */
    suspend fun process(message: String): TransactionExtractionResult {
        // 1. EXTRACTION & INITIAL CLASSIFICATION
        var result = SmsParser.parseToResult(message)

        // 2. CONFIDENCE CHECK & LOCAL MODEL
        if (result.getOverallInferenceDecision() == InferenceDecision.SECONDARY_INFERENCE_REQUIRED) {
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

        modelResult.amount?.let {
            if (result.requiresSecondaryInference("amount") && modelResult.amountConfidence != null) {
                updated = updated.copy(
                    amount = it,
                    amountConfidence = modelResult.amountConfidence,
                    amountMethod = ExtractionMethod.LOCAL_MODEL
                )
            }
        }

        modelResult.merchant?.let {
            if (result.requiresSecondaryInference("merchant") && modelResult.merchantConfidence != null) {
                updated = updated.copy(
                    merchant = it,
                    merchantConfidence = modelResult.merchantConfidence,
                    merchantMethod = ExtractionMethod.LOCAL_MODEL
                )
                // If merchant changed, re-classify category deterministically if model didn't provide one
                if (modelResult.category == null) {
                    val reClassification = com.example.transaction.classifier.MerchantClassifier.classify(it, result.merchant ?: "")
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
