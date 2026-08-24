package com.example.transaction.security

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseSecurityTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Load library
        System.loadLibrary("sqlcipher")
    }

    private fun clearResources() {
        context.deleteDatabase("transaction_db")
        val prefsFile = File(context.filesDir.parent, "shared_prefs/secure_db_prefs.xml")
        if (prefsFile.exists()) {
            prefsFile.delete()
        }
    }

    @Test
    fun testKeyGenerationOnFreshInstall() {
        clearResources()

        val passphrase = DatabaseKeyManager.getDatabasePassphrase(context)
        assertNotNull(passphrase)
        assertTrue(passphrase.isNotEmpty())
        
        val passphrase2 = DatabaseKeyManager.getDatabasePassphrase(context)
        assertArrayEquals(passphrase, passphrase2)
    }

    @Test
    fun testMigrationFromOldKey() {
        clearResources()
        
        // 1. Create a database with the OLD hardcoded key
        val oldPassphrase = "TransactionSecureKey123".toByteArray()
        val factory = SupportOpenHelperFactory(oldPassphrase)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("transaction_db")
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE test_table (id INTEGER PRIMARY KEY, data TEXT)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        
        val helper = factory.create(config)
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO test_table (data) VALUES ('sensitive_info')")
        db.close()
        
        val dbFile = context.getDatabasePath("transaction_db")
        assertTrue("Database file should exist", dbFile.exists())
        
        // 2. Call getDatabasePassphrase which should trigger migration
        val newPassphrase = DatabaseKeyManager.getDatabasePassphrase(context)
        assertNotNull(newPassphrase)
        assertNotEquals("TransactionSecureKey123", String(newPassphrase))
        
        // 3. Verify we can open it with the NEW passphrase and data is preserved
        val factory2 = SupportOpenHelperFactory(newPassphrase)
        val helper2 = factory2.create(config)
        val db2 = helper2.readableDatabase
        val cursor = db2.query("SELECT data FROM test_table")
        assertTrue("Cursor should have data", cursor.moveToNext())
        assertEquals("sensitive_info", cursor.getString(0))
        cursor.close()
        db2.close()
        
        // 4. Verify opening with OLD passphrase now fails
        var openedWithOld = false
        try {
            val factoryOld = SupportOpenHelperFactory(oldPassphrase)
            val helperOld = factoryOld.create(config)
            val dbOld = helperOld.readableDatabase
            dbOld.query("SELECT count(*) FROM test_table").close()
            openedWithOld = true
            dbOld.close()
        } catch (e: Exception) {
            // Expected
        }
        assertFalse("Should not be able to open with old key after migration", openedWithOld)
    }
}
