package com.example.transaction.sms.benchmark

import com.example.transaction.sms.TransactionExtractionResult
import com.example.transaction.sms.TransactionIntelligence
import kotlinx.coroutines.runBlocking

/**
 * Overall benchmark results.
 */
data class AggregatedBenchmarkResult(
    val modelName: String,
    val totalSamples: Int,
    val overallAccuracy: Double,
    val fieldMetrics: List<FieldMetrics>,
    val metricsByDifficulty: Map<Difficulty, Double>,
    val metricsByBank: Map<String, Double>,
    val amountFalsePositiveRate: Double,
    val averageLatencyMs: Double,
    val totalTimeMs: Long,
    val peakRamMb: Double = 0.0
)

/**
 * Engine for running benchmarks against the transaction intelligence pipeline.
 */
class ModelBenchmark(
    private val intelligence: TransactionIntelligence
) {
    /**
     * Runs the benchmark on a list of samples and returns the results.
     */
    fun run(samples: List<BenchmarkSample>): AggregatedBenchmarkResult {
        var correctCount = 0
        val results = mutableListOf<Pair<BenchmarkSample, TransactionExtractionResult>>()
        
        val startTime = System.currentTimeMillis()
        
        runBlocking {
            samples.forEach { sample ->
                val result = intelligence.process(sample.sms)
                results.add(sample to result)
                
                if (isMatch(sample.groundTruth, result)) {
                    correctCount++
                }
            }
        }
        
        val totalTimeMs = System.currentTimeMillis() - startTime

        return AggregatedBenchmarkResult(
            modelName = "Deterministic Pipeline",
            totalSamples = samples.size,
            overallAccuracy = correctCount.toDouble() / samples.size,
            fieldMetrics = calculateFieldMetrics(results),
            metricsByDifficulty = calculateAccuracyByDifficulty(results),
            metricsByBank = calculateAccuracyByBank(results),
            amountFalsePositiveRate = calculateAmountFalsePositiveRate(results),
            averageLatencyMs = totalTimeMs.toDouble() / samples.size,
            totalTimeMs = totalTimeMs
        )
    }

    private fun isMatch(truth: TransactionExtractionResult, pred: TransactionExtractionResult): Boolean {
        // Critical fields for overall match
        return truth.amount == pred.amount &&
                truth.merchant?.lowercase() == pred.merchant?.lowercase() &&
                truth.transactionType == pred.transactionType
    }

    private fun calculateAccuracyByDifficulty(results: List<Pair<BenchmarkSample, TransactionExtractionResult>>): Map<Difficulty, Double> {
        return results.groupBy { it.first.difficulty }
            .mapValues { (_, group) ->
                group.count { isMatch(it.first.groundTruth, it.second) }.toDouble() / group.size
            }
    }

    private fun calculateAccuracyByBank(results: List<Pair<BenchmarkSample, TransactionExtractionResult>>): Map<String, Double> {
        return results.groupBy { it.first.groundTruth.bank ?: "Unknown" }
            .mapValues { (_, group) ->
                group.count { it.first.groundTruth.bank == it.second.bank }.toDouble() / group.size
            }
    }

    internal fun calculateAmountFalsePositiveRate(results: List<Pair<BenchmarkSample, TransactionExtractionResult>>): Double {
        val samplesWithUnsafe = results.filter { it.first.unsafeNumbers.isNotEmpty() }
        if (samplesWithUnsafe.isEmpty()) return 0.0
        
        val falsePositives = samplesWithUnsafe.count { (sample, pred) ->
            pred.amount != null && sample.unsafeNumbers.any { unsafe -> 
                val amountStr = pred.amount.toString()
                amountStr.contains(unsafe) || (unsafe.length >= 4 && unsafe.contains(amountStr.substringBefore(".")))
            }
        }
        return falsePositives.toDouble() / samplesWithUnsafe.size
    }

    internal fun calculateFieldMetrics(
        results: List<Pair<BenchmarkSample, TransactionExtractionResult>>
    ): List<FieldMetrics> {
        val fields = listOf("amount", "merchant", "transactionType", "bank", "category")
        return fields.map { field ->
            var truePositives = 0
            var falsePositives = 0
            var falseNegatives = 0
            var trueNegatives = 0

            results.forEach { (sample, pred) ->
                val truthValue = getFieldValue(sample.groundTruth, field)
                val predValue = getFieldValue(pred, field)

                if (truthValue != null && predValue != null) {
                    if (truthValue.toString().lowercase() == predValue.toString().lowercase()) truePositives++ else falsePositives++
                } else if (truthValue != null && predValue == null) {
                    falseNegatives++
                } else if (truthValue == null && predValue != null) {
                    falsePositives++
                } else {
                    trueNegatives++
                }
            }

            val precision = if (truePositives + falsePositives > 0) 
                truePositives.toDouble() / (truePositives + falsePositives) else 0.0
            val recall = if (truePositives + falseNegatives > 0) 
                truePositives.toDouble() / (truePositives + falseNegatives) else 0.0
            val f1 = if (precision + recall > 0) 
                2 * (precision * recall) / (precision + recall) else 0.0
            
            FieldMetrics(
                fieldName = field,
                accuracy = (truePositives + trueNegatives).toDouble() / results.size,
                precision = precision,
                recall = recall,
                f1Score = f1,
                falsePositiveRate = 0.0 // Managed at aggregate level now
            )
        }
    }

    internal fun getFieldValue(result: TransactionExtractionResult, field: String): Any? {
        return when (field) {
            "amount" -> result.amount
            "merchant" -> result.merchant
            "transactionType" -> result.transactionType
            "bank" -> result.bank
            "category" -> result.category
            else -> null
        }
    }
}
