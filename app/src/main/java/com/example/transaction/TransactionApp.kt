package com.example.transaction

import android.app.Application

class TransactionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load SQLCipher native library at startup
        System.loadLibrary("sqlcipher")
    }
}
