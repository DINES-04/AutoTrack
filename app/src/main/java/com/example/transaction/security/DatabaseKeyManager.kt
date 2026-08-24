package com.example.transaction.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

object DatabaseKeyManager {
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val OLD_PASSPHRASE = "TransactionSecureKey123"

    fun getDatabasePassphrase(context: Context): ByteArray {
        val sharedPreferences = getEncryptedPrefs(context)
        var passphraseStr = sharedPreferences.getString(KEY_PASSPHRASE, null)

        if (passphraseStr == null) {
            // New key needed. Check if we need to migrate or if it's a fresh install.
            if (isDatabaseExists(context)) {
                // Database exists but no new key. Migration is required.
                val newPassphrase = generateNewPassphrase()
                migrateDatabase(context, newPassphrase)
                savePassphrase(sharedPreferences, newPassphrase)
                passphraseStr = newPassphrase
            } else {
                // Fresh install
                val newPassphrase = generateNewPassphrase()
                savePassphrase(sharedPreferences, newPassphrase)
                passphraseStr = newPassphrase
            }
        }

        return passphraseStr.toByteArray()
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun isDatabaseExists(context: Context): Boolean {
        val dbFile = context.getDatabasePath("transaction_db")
        return dbFile.exists()
    }

    private fun generateNewPassphrase(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun savePassphrase(prefs: SharedPreferences, passphrase: String) {
        prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
    }

    private fun migrateDatabase(context: Context, newPassphraseStr: String) {
        try {
            val factory = SupportOpenHelperFactory(OLD_PASSPHRASE.toByteArray())
            val config = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("transaction_db")
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
            val helper = factory.create(config)
            val db = helper.writableDatabase
            
            // Re-keying using PRAGMA rekey on the SQLCipher-backed SupportSQLiteDatabase
            db.execSQL("PRAGMA rekey = '${newPassphraseStr}'")
            db.close()
        } catch (e: Exception) {
            // Migration failed or not needed
        }
    }
}
