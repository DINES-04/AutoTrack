package com.example.transaction.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.transaction.classifier.MerchantClassifier
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.MerchantMapping
import com.example.transaction.data.entity.TransactionEntity
import com.example.transaction.sms.MerchantNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MerchantMappingTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testUserMappingOverride() = runBlocking {
        val merchant = "Amazon India"
        val normalized = MerchantNormalizer.normalize(merchant)
        
        // 1. Automatic classification would be SHOPPING
        val autoClassification = MerchantClassifier.classify(merchant, "Paid to Amazon")
        assertEquals(MerchantClassifier.SHOPPING, autoClassification.category)

        // 2. Add user mapping: Amazon -> Business
        val userCategory = "Business"
        db.merchantMappingDao().insertMapping(MerchantMapping(normalized, userCategory, true))

        // 3. Simulate SmsReceiver lookup
        val mapping = db.merchantMappingDao().getMappingForMerchant(normalized)
        assertNotNull(mapping)
        assertEquals(userCategory, mapping!!.category)
        
        val finalCategory = mapping.category
        assertEquals("Business", finalCategory)
    }

    @Test
    fun testCaseInsensitiveMappingLookup() = runBlocking {
        val merchant = "Zomato"
        val normalized = MerchantNormalizer.normalize(merchant)
        
        db.merchantMappingDao().insertMapping(MerchantMapping(normalized, "Food", true))
        
        // Lookup with different case should still work because we normalize before lookup
        val lookupName = "ZOMATO"
        val lookupNormalized = MerchantNormalizer.normalize(lookupName)
        val mapping = db.merchantMappingDao().getMappingForMerchant(lookupNormalized)
        
        assertNotNull(mapping)
        assertEquals("Food", mapping!!.category)
    }
}
