package com.example.transaction.sms

import com.example.transaction.data.dao.MerchantMappingDao

/**
 * Orchestrates the transaction intelligence pipeline:
 * SMS -> Extraction -> Confidence Check -> [Local Model] -> User Mapping -> Final Category
 */
class TransactionIntelligence(private val merchantMappingDao: MerchantMappingDao? = null) {

    /**
     * Processes an SMS message through the full intelligence pipeline.
     * Currently follows Phase 1F-1 logic.
     */
    suspend fun process(message: String): TransactionExtractionResult {
        // 1. EXTRACTION & INITIAL CLASSIFICATION
        // This corresponds to "Context Parser" and "Category Classifier" in the diagram
        var result = SmsParser.parseToResult(message)

        // 2. CONFIDENCE CHECK & LOCAL MODEL
        // To be implemented in Phase 1F-2+
        // if (result.amountConfidence < 0.8 || result.merchantConfidence < 0.5) {
        //     result = localModel.refine(message, result)
        // }

        // 3. USER MAPPING (Highest Priority)
        result = applyUserMapping(result)

        return result
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
