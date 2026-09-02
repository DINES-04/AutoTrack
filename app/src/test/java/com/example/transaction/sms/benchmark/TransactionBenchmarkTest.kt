package com.example.transaction.sms.benchmark

import com.example.transaction.sms.TransactionIntelligence
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Executes the Phase 1F-6 Comprehensive Benchmark.
 */
class TransactionBenchmarkTest {

    @Test
    fun runPhase1F6Benchmark() {
        val samples = SyntheticDataset.generate()
        val intelligence = TransactionIntelligence()
        val benchmark = ModelBenchmark(intelligence)
        
        val result = benchmark.run(samples)
        
        val report = StringBuilder()
        report.append("=== Phase 1F-6 COMPREHENSIVE BENCHMARK REPORT ===\n")
        report.append("Total Samples: ${result.totalSamples}\n")
        report.append("Overall Accuracy: ${"%.2f".format(result.overallAccuracy * 100)}%\n")
        report.append("Total Time: ${result.totalTimeMs} ms\n")
        report.append("Avg Latency: ${"%.2f".format(result.averageLatencyMs)} ms\n")
        report.append("Amount False Positive Rate: ${"%.2f".format(result.amountFalsePositiveRate * 100)}%\n")
        report.append("\n--- Accuracy by Difficulty ---\n")
        result.metricsByDifficulty.forEach { (diff, acc) ->
            report.append("${diff.name.padEnd(12)}: ${"%.2f".format(acc * 100)}%\n")
        }
        
        report.append("\n--- Field-Level Metrics ---\n")
        result.fieldMetrics.forEach { 
            report.append("Field: ${it.fieldName.padEnd(15)} | Acc: ${"%.2f".format(it.accuracy * 100)}% | F1: ${"%.2f".format(it.f1Score * 100)}%\n")
        }
        
        report.append("\n--- Bank Coverage Accuracy ---\n")
        result.metricsByBank.toSortedMap().forEach { (bank, acc) ->
            report.append("${bank.padEnd(25)}: ${"%.2f".format(acc * 100)}%\n")
        }
        report.append("==================================================\n")
        
        val counts = samples.groupBy { it.difficulty }.mapValues { it.value.size }
        report.append("\nSample Distribution:\n")
        counts.forEach { (d, c) -> report.append("${d.name}: $c\n") }

        java.io.File("benchmark_report.txt").writeText(report.toString())
        
        println(report.toString())

        assertTrue("Dataset should have at least 500 samples", samples.size >= 500)
        assertTrue("Should cover all difficulty levels", counts.size == 4)
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
