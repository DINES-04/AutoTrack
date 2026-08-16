package com.example.transaction.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.example.transaction.data.dao.AccountDao
import com.example.transaction.data.dao.SettingsDao
import com.example.transaction.data.dao.TransactionDao
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity

@Database(entities = [Account::class, TransactionEntity::class, SettingsEntity::class], version = 3, exportSchema = false)
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
                val passphrase = "TransactionSecureKey123".toByteArray()
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transaction_db",
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
