package com.example.transaction.sms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking

/**
 * Tests for the REAL BERT-Tiny model integration.
 */
@RunWith(RobolectricTestRunner::class)
class BertTinyRealModelTest {

    @Test
    fun testRealModelLoadingFailsIfFileMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = BertTinyLocalModel(context)
        
        assertEquals(ModelStatus.DISABLED, model.status)
        
        // Since bert_tiny_transaction.tflite is not actually in assets (only README), this should fail
        model.load()
        
        assertEquals(ModelStatus.ERROR, model.status)
    }
}
