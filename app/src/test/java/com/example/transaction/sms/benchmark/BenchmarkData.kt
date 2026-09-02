package com.example.transaction.sms.benchmark

import com.example.transaction.sms.TransactionExtractionResult

/**
 * Difficulty level for benchmark samples.
 */
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD,
    ADVERSARIAL
}

/**
 * A single benchmark sample consisting of the SMS text and its ground truth.
 */
data class BenchmarkSample(
    val id: String,
    val sms: String,
    val groundTruth: TransactionExtractionResult,
    val difficulty: Difficulty,
    val expectedDate: String? = null,
    val unsafeNumbers: List<String> = emptyList(),
    val ambiguityFlags: List<String> = emptyList(),
    val description: String = ""
)

/**
 * Field-level metrics for a benchmark run.
 */
data class FieldMetrics(
    val fieldName: String,
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val falsePositiveRate: Double = 0.0
)

/**
 * Overall benchmark results.
 */
data class BenchmarkResult(
    val modelName: String,
    val totalSamples: Int,
    val overallAccuracy: Double,
    val fieldMetrics: List<FieldMetrics>,
    val averageLatencyMs: Double,
    val peakRamMb: Double,
    val loadTimeMs: Long
)
