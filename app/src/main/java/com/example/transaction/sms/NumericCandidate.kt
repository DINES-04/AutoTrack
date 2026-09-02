package com.example.transaction.sms

/**
 * Represents the type of a numeric candidate found in an SMS.
 */
enum class CandidateType {
    TRANSACTION_AMOUNT,
    ACCOUNT_IDENTIFIER,
    REFERENCE_NUMBER,
    PHONE_NUMBER,
    OTP,
    DATE,
    TIME,
    BALANCE,
    OTHER
}

/**
 * Represents a numeric value found in an SMS with its associated metadata.
 */
data class NumericCandidate(
    val rawValue: String,
    val normalizedValue: Double? = null,
    val start: Int,
    val end: Int,
    val type: CandidateType,
    val confidence: Double = 0.0,
    val context: String = "",
    val method: ExtractionMethod = ExtractionMethod.REGEX
)
