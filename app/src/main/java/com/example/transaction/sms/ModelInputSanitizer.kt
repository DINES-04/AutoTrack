package com.example.transaction.sms

/**
 * Sanitizes sensitive information from SMS messages before passing them to the local model.
 */
object ModelInputSanitizer {

    /**
     * Sanitizes a raw SMS message.
     * Replaces sensitive patterns with generic placeholders while preserving context.
     */
    fun sanitize(message: String): String {
        var sanitized = message
        
        // 1. Mask long numbers (likely RRN, UTR, Phone numbers)
        sanitized = sanitized.replace(Regex("\\b\\d{10,}\\b"), "[ID]")
        
        // 2. Mask OTPs
        sanitized = sanitized.replace(Regex("(?i)\\b(otp|code|is)\\b\\s*[:\\-]?\\s*\\d{4,8}\\b"), "OTP [MASK]")
        
        // 3. Mask potential VPA/UPI IDs (if present)
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9.\\-_]{2,25}@[a-zA-Z]{2,10}"), "[VPA]")
        
        // 4. Preserve account identifiers like XX1234 or *5342 but mask others
        // (Handled by keeping 4 digits usually, but for sanitization we might want to be stricter)
        
        return sanitized.trim()
    }
}
