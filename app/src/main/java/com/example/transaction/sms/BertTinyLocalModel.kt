package com.example.transaction.sms

import android.content.Context
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier
import java.io.IOException

/**
 * A real implementation of a BERT-based on-device model using TensorFlow Lite Task Library.
 */
class BertTinyLocalModel(private val context: Context) : LocalTransactionModel {

    private var classifier: BertNLClassifier? = null
    private var _status = ModelStatus.DISABLED
    override val status: ModelStatus get() = _status

    private val MODEL_FILE = "bert_tiny_transaction.tflite"

    override suspend fun load() {
        if (_status == ModelStatus.AVAILABLE || _status == ModelStatus.LOADING) return
        
        _status = ModelStatus.LOADING
        try {
            val options = BertNLClassifier.BertNLClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().setNumThreads(2).build())
                .build()
            
            // This call actually loads the .tflite from assets and initializes the native JNI layer
            classifier = BertNLClassifier.createFromFileAndOptions(context, MODEL_FILE, options)
            _status = ModelStatus.AVAILABLE
        } catch (e: Exception) {
            _status = ModelStatus.ERROR
            // Failed to load real model, will fall back to deterministic pipeline
        }
    }

    override suspend fun infer(input: TransactionModelInput): LocalModelResult? {
        val currentClassifier = classifier
        if (_status != ModelStatus.AVAILABLE || currentClassifier == null) {
            return null
        }

        return try {
            val sanitized = ModelInputSanitizer.sanitize(input.message)
            
            // Perform REAL neural network inference
            val results = currentClassifier.classify(sanitized)
            
            // Map result to LocalModelResult
            val topResult = results.maxByOrNull { it.score }
            
            if (topResult != null && topResult.score > 0.5) {
                LocalModelResult(
                    category = if (input.targetFields.contains("category")) topResult.label else null,
                    categoryConfidence = topResult.score.toDouble(),
                    transactionType = if (input.targetFields.contains("type")) mapLabelToType(topResult.label) else null,
                    transactionTypeConfidence = topResult.score.toDouble(),
                    modelName = "BERT-Tiny-Real",
                    modelVersion = "1.0.0"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLabelToType(label: String): String? {
        return when (label.uppercase()) {
            "DEBIT", "SPENT", "PAID" -> "DEBIT"
            "CREDIT", "RECEIVED", "ADDED" -> "CREDIT"
            else -> null
        }
    }

    override fun release() {
        classifier?.close()
        classifier = null
        _status = ModelStatus.DISABLED
    }
}
