package com.example.transaction.sms.benchmark

import com.example.transaction.sms.TransactionIntelligence
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider

/**
 * Executes the Phase 1F-6 Comprehensive Benchmark.
 */
@RunWith(RobolectricTestRunner::class)
class TransactionBenchmarkTest {

    @Test
    fun runPhase1F8HybridBenchmark() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val samples = SyntheticDataset.generate()
        
        // 1. Deterministic Only
        val detIntelligence = TransactionIntelligence(useMl = false)
        val detBenchmark = ModelBenchmark(detIntelligence)
        val detResult = detBenchmark.run(samples)
        
        // 2. Hybrid (Deterministic + BERT-Tiny)
        val model = com.example.transaction.sms.BertTinyLocalModel(context)
        val hybridIntelligence = TransactionIntelligence(localModel = model, useMl = true)
        val hybridBenchmark = ModelBenchmark(hybridIntelligence)
        val hybridResult = hybridBenchmark.run(samples)
        
        val report = StringBuilder()
        report.append("=== Phase 1F-8 HYBRID BENCHMARK REPORT ===\n")
        report.append("Total Samples: ${samples.size}\n\n")
        
        report.append("METRIC              | DETERMINISTIC | HYBRID (ML)   | IMPROVEMENT\n")
        report.append("--------------------|---------------|---------------|------------\n")
        report.append("Overall Accuracy    | ${formatAcc(detResult.overallAccuracy)} | ${formatAcc(hybridResult.overallAccuracy)} | ${formatImp(detResult.overallAccuracy, hybridResult.overallAccuracy)}\n")
        report.append("Amount Accuracy     | ${formatField(detResult, "amount")} | ${formatField(hybridResult, "amount")} | --\n")
        report.append("Merchant Accuracy   | ${formatField(detResult, "merchant")} | ${formatField(hybridResult, "merchant")} | ${formatImp(getFieldAcc(detResult, "merchant"), getFieldAcc(hybridResult, "merchant"))}\n")
        report.append("Bank Accuracy       | ${formatField(detResult, "bank")} | ${formatField(hybridResult, "bank")} | ${formatImp(getFieldAcc(detResult, "bank"), getFieldAcc(hybridResult, "bank"))}\n")
        report.append("Amount FPR          | ${formatAcc(detResult.amountFalsePositiveRate)} | ${formatAcc(hybridResult.amountFalsePositiveRate)} | --\n")
        
        report.append("\n--- Accuracy by Difficulty (Hybrid) ---\n")
        hybridResult.metricsByDifficulty.toSortedMap().forEach { (diff, acc) ->
            val detAcc = detResult.metricsByDifficulty[diff] ?: 0.0
            report.append("${diff.name.padEnd(12)}: ${formatAcc(acc)} (was ${formatAcc(detAcc)})\n")
        }
        
        report.append("\n==================================================\n")
        java.io.File("benchmark_hybrid_report.txt").writeText(report.toString())
        println(report.toString())
    }

    private fun formatAcc(acc: Double) = "${"%.2f".format(acc * 100)}%"
    private fun formatImp(old: Double, new: Double): String {
        val diff = (new - old) * 100
        return if (diff >= 0) "+${"%.2f".format(diff)}%" else "${"%.2f".format(diff)}%"
    }
    private fun formatField(res: AggregatedBenchmarkResult, field: String): String {
        val acc = res.fieldMetrics.find { it.fieldName == field }?.accuracy ?: 0.0
        return formatAcc(acc)
    }
    private fun getFieldAcc(res: AggregatedBenchmarkResult, field: String): Double {
        return res.fieldMetrics.find { it.fieldName == field }?.accuracy ?: 0.0
    }

    @Test
    fun testDatasetQuality() {
        val samples = SyntheticDataset.generate()
        samples.forEach { sample ->
            assertTrue("Sample ID missing", sample.id.isNotEmpty())
            assertTrue("SMS text missing", sample.sms.isNotEmpty() || sample.description == "Empty SMS")
            
            // Ensure unsafe numbers are not equal to the expected amount
            sample.unsafeNumbers.forEach { unsafe ->
                val amountStr = sample.groundTruth.amount?.toString()
                if (amountStr != null) {
                    assertTrue("Unsafe number $unsafe should not equal amount $amountStr", 
                        unsafe != amountStr && !unsafe.startsWith(amountStr) || unsafe.length > amountStr.length)
                }
            }
        }
    }
}
