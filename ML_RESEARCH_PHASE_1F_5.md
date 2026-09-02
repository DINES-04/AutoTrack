# Phase 1F-5: Local Model Selection & Benchmark Framework

## 1. Model Task Definition
The local model serves as a **secondary inference mechanism** for the AutoTrack transaction engine. It is triggered only when the primary deterministic parser (SmsParser) returns low-confidence results or ambiguous fields.

**Target Fields:**
- **Amount:** Extracting the exact monetary value of the transaction.
- **Merchant:** Identifying the recipient or source of funds.
- **Transaction Type:** Classifying as `DEBIT`, `CREDIT`, or `TRANSFER`.
- **Bank:** Identifying the financial institution (normalized name).
- **Category:** Predicting the spending category (Food, Shopping, etc.).

---

## 2. Model Requirements
| Requirement | Specification |
| :--- | :--- |
| **Model Type** | Specialized Transaction Extraction / Classification |
| **Execution** | Fully On-Device, 100% Offline |
| **Runtime** | CPU-first (preferred), NNAPI/GPU (optional) |
| **Target Size** | Ideal: < 50 MB | Acceptable: 50–100 MB |
| **Latency** | < 200ms on mid-range devices |
| **RAM** | < 150 MB during inference |
| **Security** | No network permissions, no sensitive data logging |

---

## 3. Model Evaluation Matrix
Candidates will be evaluated against the following criteria:

1.  **Merchant Extraction Accuracy** (Exact & Normalized)
2.  **Amount Extraction Accuracy** (Avoidance of Account/RRN confusion)
3.  **Debit/Credit Accuracy**
4.  **Bank Extraction Accuracy**
5.  **Category Classification Accuracy**
6.  **Ambiguous SMS Resolution** (Success rate on "Hard" cases)
7.  **False Positive Rate** (Incorrectly identifying non-transactions)
8.  **Model Size** (MB)
9.  **Runtime RAM** (MB)
10. **Cold-start Latency** (ms)
11. **Warm Inference Latency** (ms)
12. **CPU Utilization** (%)
13. **Battery Impact** (Estimated)
14. **Thermal Impact** (Estimated)
15. **Android Compatibility** (Min SDK, NDK requirements)
16. **Quantization Support** (INT8, INT4, FP16)
17. **Offline Support** (Built-in or requires downloads)
18. **License** (Apache 2.0, MIT, etc.)
19. **Maintenance Status** (Community support)
20. **Structured Output Capability** (JSON or field-level tensors)

---

## 4. Model Categories to Investigate

### A. Small Encoder Models (BERT-Tiny, BERT-Mini)
- **Pros:** Excellent contextual understanding; very small footprint (BERT-Tiny is ~17MB).
- **Cons:** Requires specialized tokenization; slightly higher latency than classical methods.

### B. Small Token-Classification Models (NER)
- **Pros:** Specifically designed for field extraction (Merchant, Bank).
- **Cons:** Can be brittle if the SMS structure varies significantly from training data.

### C. Small Sequence-to-Sequence (T5-Small)
- **Pros:** Can output structured text directly.
- **Cons:** Usually > 200MB; high RAM/latency for mobile. (Likely Unsuitable).

### D. Small Instruction/Causal LMs (Phi-1.5, Gemma-2b)
- **Pros:** Extremely high accuracy and reasoning.
- **Cons:** Usually > 1GB; requires significant RAM/GPU. (Unsuitable for this phase).

### E. Specialized NER/Information-Extraction
- **Pros:** Lightweight (e.g., CRF-based or Bi-LSTM).
- **Cons:** May lack the transformer-level context needed for complex "Adversarial" cases.

### F. Traditional ML/Classical NLP
- **Pros:** Extremely fast; < 1MB size; works on any CPU.
- **Cons:** Difficulty with varying word orders and unseen merchants.

---

## 5. Candidate Model Research (Preliminary)

| Model Name | Architecture | Params | Size (Approx) | Quantized | Suitability |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **BERT-Tiny** | Encoder | 4.4M | 17 MB | ~4 MB | **High** (Context-rich, tiny) |
| **BERT-Mini** | Encoder | 11M | 45 MB | ~11 MB | **High** (Balanced) |
| **MobileBERT** | Encoder | 25M | 100 MB | ~25 MB | **Medium** (Good performance) |
| **FastText** | N-gram | N/A | < 5 MB | < 1 MB | **High** (For Classification only) |
| **ML Kit Entity**| Google Custom | Unknown | Shared | N/A | **Medium** (Generic entities only) |

*Note: Accuracy figures are TO BE BENCHMARKED in Phase 1F-6.*

---

## 6. Dataset Design (Synthetic)
The benchmark dataset will consist of 500+ synthetic SMS samples covering:

**Banks:** SBI, HDFC, ICICI, Axis, Kotak, IDFC, IndusInd, Indian Bank, BoB, Canara, PNB, Federal, Yes, AU, etc.
**Types:** UPI (GPay, PhonePe, Paytm), Card (Debit/Credit), ATM, Salary, Refund, Transfers, Bill Pay, Subscriptions.

---

## 7. Difficult SMS Cases & Ground Truth
The benchmark specifically targets:
- **Multiple Values:** "Rs. 100 debited from A/c... Bal Rs. 5000"
- **Footers:** "Not you? Call 1800..." (Avoid identifying phone as amount)
- **Ambiguous Merchants:** "Paid to Jana" (Jana is the merchant, not the person).
- **Currency Symbols:** Rs, Rs., INR, ₹, and comma-formatting (1,200.00).

---

## 8. Benchmark Metrics & Comparison
The most important metric is **Deterministic + Model Accuracy**.
The model should only be used to resolve cases where the `SmsParser` is unsure.

**Safety Metric:** `AMOUNT_FALSE_POSITIVE_RATE`
The model **must not** extract an Account Number or Reference Number as an Amount.

---

## 9. Performance & Security
- **Inference Stability:** 100 consecutive inferences must not cause OOM or thermal throttling.
- **Privacy:** Absolutely no PII (Phone numbers, Full account numbers) in logs.
- **Verification:** Model integrity will be checked via SHA-256 hash.

---

## 10. Next Steps (Phase 1F-6)
- Develop the `ModelBenchmark` suite.
- Generate the synthetic dataset.
- Run baseline deterministic benchmarks.
- Evaluate the first candidate (BERT-Tiny/Mini).
