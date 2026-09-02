package com.example.transaction.sms.benchmark

/**
 * Difficulty levels for benchmark samples.
 */
enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD,
    ADVERSARIAL
}

/**
 * Ground truth annotations for a synthetic benchmark SMS.
 */
data class GroundTruthTransaction(
    val smsText: String,
    val expectedAmount: Double?,
    val expectedMerchant: String?,
    val expectedType: String?, // "DEBIT", "CREDIT", "UNKNOWN"
    val expectedBank: String?,
    val expectedCategory: String?,
    val expectedAccount: String? = null,
    val expectedReference: String? = null,
    val expectedDate: String? = null,
    val difficulty: DifficultyLevel = DifficultyLevel.EASY,
    val ambiguityReason: String? = null
)

/**
 * A single benchmark sample with an identifier.
 */
data class BenchmarkSample(
    val id: String,
    val groundTruth: GroundTruthTransaction
)
