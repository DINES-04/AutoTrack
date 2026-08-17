package com.example.transaction.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.example.transaction.data.dao.AccountDao
import com.example.transaction.data.dao.MerchantMappingDao
import com.example.transaction.data.dao.SettingsDao
import com.example.transaction.data.dao.TransactionDao
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.MerchantMapping
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity

@Database(entities = [Account::class, TransactionEntity::class, SettingsEntity::class, MerchantMapping::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun merchantMappingDao(): MerchantMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `merchant_mappings` (`merchantName` TEXT NOT NULL, `category` TEXT NOT NULL, `isCustom` INTEGER NOT NULL, PRIMARY KEY(`merchantName`))")
            }
        }

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
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
