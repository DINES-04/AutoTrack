package com.example.transaction

import android.app.Application
import net.zetetic.database.sqlcipher.SQLiteDatabase

class TransactionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load SQLCipher native library at startup
        System.loadLibrary("sqlcipher")
    }
}
