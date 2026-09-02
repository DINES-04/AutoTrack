package com.example.transaction.sms

/**
 * Represents the availability and lifecycle state of the local transaction model.
 */
enum class ModelStatus {
    AVAILABLE,
    UNAVAILABLE,
    LOADING,
    ERROR,
    DISABLED
}

/**
 * Input for the local transaction model.
 * Contains sanitized context and deterministic results for refinement.
 */
data class TransactionModelInput(
    val message: String,
    val currentResult: TransactionExtractionResult,
    val targetFields: List<String> = emptyList()
)

/**
 * Result from the local transaction model inference.
 */
data class LocalModelResult(
    val amount: Double? = null,
    val amountConfidence: Double? = null,
    
    val merchant: String? = null,
    val merchantConfidence: Double? = null,
    
    val transactionType: String? = null,
    val transactionTypeConfidence: Double? = null,
    
    val category: String? = null,
    val categoryConfidence: Double? = null,
    
    val bank: String? = null,
    val bankConfidence: Double? = null,
    
    val modelName: String = "unknown",
    val modelVersion: String = "0.0.0",
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Abstraction for a future lightweight on-device transaction model.
 * This model acts as a secondary inference mechanism for ambiguous fields.
 */
interface LocalTransactionModel {
    /**
     * Current status of the model.
     */
    val status: ModelStatus

    /**
     * Performs inference on the given input to resolve ambiguous fields.
     * Must be called from a background thread (implemented by the concrete class).
     */
    suspend fun infer(input: TransactionModelInput): LocalModelResult?

    /**
     * Requests the model to load its resources if it's currently unavailable or released.
     */
    suspend fun load()

    /**
     * Releases model resources.
     */
    fun release()
}
