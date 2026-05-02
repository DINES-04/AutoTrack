package com.example.transaction.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.transaction.data.dao.AccountDao
import com.example.transaction.data.dao.SettingsDao
import com.example.transaction.data.dao.TransactionDao
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity

@Database(entities = [Account::class, TransactionEntity::class, SettingsEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // To keep it simple but secure, we use a fixed passphrase for encryption.
                // In a production app, this should be stored in Android Keystore.
                val passphrase = SQLiteDatabase.getBytes("TransactionSecureKey123".toCharArray())
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transaction_db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
