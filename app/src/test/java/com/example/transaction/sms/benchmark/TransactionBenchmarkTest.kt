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
        
        println("=== Phase 1F-6 COMPREHENSIVE BENCHMARK REPORT ===")
        println("Total Samples: ${result.totalSamples}")
        println("Overall Accuracy: ${"%.2f".format(result.overallAccuracy * 100)}%")
        println("Total Time: ${result.totalTimeMs} ms")
        println("Avg Latency: ${"%.2f".format(result.averageLatencyMs)} ms")
        println("Amount False Positive Rate: ${"%.2f".format(result.amountFalsePositiveRate * 100)}%")
        println("\n--- Accuracy by Difficulty ---")
        result.metricsByDifficulty.forEach { (diff, acc) ->
            println("${diff.name.padEnd(12)}: ${"%.2f".format(acc * 100)}%")
        }
        
        println("\n--- Field-Level Metrics ---")
        result.fieldMetrics.forEach { 
            println("Field: ${it.fieldName.padEnd(15)} | Acc: ${"%.2f".format(it.accuracy * 100)}% | F1: ${"%.2f".format(it.f1Score * 100)}%")
        }
        
        println("\n--- Bank Coverage Accuracy ---")
        result.metricsByBank.toSortedMap().forEach { (bank, acc) ->
            println("${bank.padEnd(25)}: ${"%.2f".format(acc * 100)}%")
        }
        println("==================================================")
        
        // Distribution checks
        val counts = samples.groupBy { it.difficulty }.mapValues { it.value.size }
        println("\nSample Distribution:")
        counts.forEach { (d, c) -> println("${d.name}: $c") }

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
