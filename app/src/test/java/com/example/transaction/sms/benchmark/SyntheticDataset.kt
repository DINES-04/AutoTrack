package com.example.transaction.sms.benchmark

import com.example.transaction.sms.TransactionExtractionResult

object SyntheticDataset {

    fun generate(): List<BenchmarkSample> {
        val samples = mutableListOf<BenchmarkSample>()
        
        val bankConfigs = listOf(
            BankConfig("SBI", "SBI"),
            BankConfig("HDFC", "HDFC Bank"),
            BankConfig("ICICI", "ICICI Bank"),
            BankConfig("Axis", "Axis Bank"),
            BankConfig("Kotak", "Kotak Mahindra Bank"),
            BankConfig("IndianBank", "Indian Bank"),
            BankConfig("Canara", "Canara Bank"),
            BankConfig("UnionBank", "Union Bank"),
            BankConfig("BoB", "Bank of Baroda"),
            BankConfig("PNB", "Punjab National Bank"),
            BankConfig("Federal", "Federal Bank"),
            BankConfig("IDFC", "IDFC FIRST Bank"),
            BankConfig("IndusInd", "IndusInd Bank"),
            BankConfig("YesBank", "Yes Bank"),
            BankConfig("AU", "AU Small Finance Bank")
        )

        val paymentTypes = listOf("UPI_DEBIT", "UPI_CREDIT", "CARD", "ATM", "SALARY", "REFUND", "BILL_PAY")
        val merchants = listOf("Amazon", "Zomato", "Swiggy", "Uber", "Flipkart", "Zepto", "Blinkit", "Myntra", "Reliance Retail", "Indian Oil", "Netflix", "Google", "Apple", "Big Bazaar", "Jio")

        var idCounter = 1

        // Generate systematic combinations
        for (bank in bankConfigs) {
            for (type in paymentTypes) {
                for (i in 1..5) { // 5 samples per bank/type combo = 15 * 7 * 5 = 525 samples
                    val merchant = merchants.random()
                    val amount = ((10..5000).random() + ((0..99).random() / 100.0))
                    val acct = (1000..9999).random().toString()
                    val rrn = (100000000000L..999999999999L).random().toString()
                    
                    samples.add(generateSample(idCounter++, bank, type, merchant, amount, acct, rrn))
                }
            }
        }

        // Add 100 Adversarial samples
        for (i in 1..100) {
            samples.add(generateAdversarialSample(idCounter++))
        }

        // Add 50 Malformed samples
        samples.addAll(generateMalformedSamples(idCounter))

        return samples
    }

    private data class BankConfig(val sender: String, val fullName: String)

    private fun generateSample(id: Int, bank: BankConfig, type: String, merchant: String, amount: Double, acct: String, rrn: String): BenchmarkSample {
        val difficulty = when(type) {
            "UPI_DEBIT", "CARD" -> Difficulty.MEDIUM
            "ATM", "SALARY", "REFUND" -> Difficulty.HARD
            else -> Difficulty.EASY
        }

        val sms = when(type) {
            "UPI_DEBIT" -> "A/c XX$acct debited by Rs.$amount for UPI txn to $merchant. Ref $rrn. - ${bank.fullName}"
            "UPI_CREDIT" -> "Rs.$amount credited to A/c XX$acct from $merchant via UPI. Ref $rrn. - ${bank.fullName}"
            "CARD" -> "Transaction of Rs $amount at $merchant on your ${bank.fullName} Card XX$acct. Bal: Rs 45000."
            "ATM" -> "ATM withdrawal of Rs.$amount from A/c XX$acct at ${bank.fullName} ATM. Bal: Rs 12000."
            "SALARY" -> "${bank.sender}: Salary of Rs. $amount credited to A/c XX$acct on 01-Jan."
            "REFUND" -> "Refund of Rs.$amount for transaction at $merchant credited to A/c XX$acct. Ref $rrn."
            "BILL_PAY" -> "Bill payment of Rs.$amount to $merchant successful from A/c XX$acct. - ${bank.fullName}"
            else -> "Paid Rs.$amount to $merchant"
        }

        return BenchmarkSample(
            id = "S$id",
            sms = sms,
            difficulty = difficulty,
            groundTruth = TransactionExtractionResult(
                amount = amount,
                merchant = if (type == "ATM") "ATM" else if (type == "SALARY") "Salary" else merchant,
                transactionType = if (type.contains("CREDIT") || type == "SALARY" || type == "REFUND") "CREDIT" else "DEBIT",
                bank = bank.fullName,
                accountIdentifier = acct,
                referenceNumber = rrn
            ),
            unsafeNumbers = listOf(acct, rrn, "45000", "12000")
        )
    }

    private fun generateAdversarialSample(id: Int): BenchmarkSample {
        val amount = (100..500).random().toDouble()
        val acct = "5342"
        val rrn = "212423353897"
        val phone = "1232123124"
        val date = "29/08/2026"
        
        val sms = "A/c *$acct debited and Rs.$amount.00 added to jana on $date. RRN:$rrn. Not you? SMS BLOCK to $phone - Indian Bank"
        
        return BenchmarkSample(
            id = "ADV$id",
            sms = sms,
            difficulty = Difficulty.ADVERSARIAL,
            groundTruth = TransactionExtractionResult(
                amount = amount,
                merchant = "jana",
                transactionType = "DEBIT",
                bank = "Indian Bank",
                accountIdentifier = acct,
                referenceNumber = rrn
            ),
            expectedDate = date,
            unsafeNumbers = listOf(acct, rrn, phone, "1232123124")
        )
    }

    private fun generateMalformedSamples(startId: Int): List<BenchmarkSample> {
        return listOf(
            BenchmarkSample("MAL${startId}", "", TransactionExtractionResult(), Difficulty.ADVERSARIAL, description = "Empty SMS"),
            BenchmarkSample("MAL${startId+1}", "   ", TransactionExtractionResult(), Difficulty.ADVERSARIAL, description = "Whitespace SMS"),
            BenchmarkSample("MAL${startId+2}", "Rs. 100", TransactionExtractionResult(amount = 100.0), Difficulty.MEDIUM, description = "Partial SMS"),
            BenchmarkSample("MAL${startId+3}", "!!! Rs. 500 @@@ Merchant ???", TransactionExtractionResult(amount = 500.0, merchant = "Merchant"), Difficulty.HARD, description = "Noise SMS")
        )
    }
}
