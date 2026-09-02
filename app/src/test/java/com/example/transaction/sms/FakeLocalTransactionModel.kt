package com.example.transaction.sms

import kotlinx.coroutines.delay

/**
 * A test double for the local transaction model to simulate various inference scenarios.
 */
class FakeLocalTransactionModel(
    override var status: ModelStatus = ModelStatus.AVAILABLE,
    var mockResult: LocalModelResult? = null,
    var shouldFail: Boolean = false,
    var delayMs: Long = 0
) : LocalTransactionModel {

    var inferenceCount = 0
        private set

    override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
        inferenceCount++
        if (shouldFail) throw RuntimeException("Simulated model failure")
        if (delayMs > 0) delay(delayMs)
        return mockResult
    }

    override suspend fun load() {
        status = ModelStatus.AVAILABLE
    }

    override fun release() {
        status = ModelStatus.DISABLED
    }
}
