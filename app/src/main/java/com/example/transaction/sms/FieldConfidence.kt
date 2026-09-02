package com.example.transaction.sms

/**
 * Represents the reliability levels for extracted transaction fields.
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

/**
 * Centralized configuration for confidence thresholds.
 */
object ConfidenceThresholds {
    const val HIGH_THRESHOLD = 0.90
    const val MEDIUM_THRESHOLD = 0.70

    fun getLevel(score: Double): ConfidenceLevel = when {
        score >= HIGH_THRESHOLD -> ConfidenceLevel.HIGH
        score >= MEDIUM_THRESHOLD -> ConfidenceLevel.MEDIUM
        score > 0.0 -> ConfidenceLevel.LOW
        else -> ConfidenceLevel.UNKNOWN
    }

    /**
     * Determines if a field with the given score and level requires secondary inference (e.g., from an ML model).
     */
    fun requiresInference(score: Double): Boolean {
        return score < HIGH_THRESHOLD
    }
}

/**
 * Detailed confidence information for a specific transaction field.
 */
data class FieldConfidence(
    val score: Double,
    val level: ConfidenceLevel,
    val method: ExtractionMethod,
    val requiresSecondaryInference: Boolean
)
