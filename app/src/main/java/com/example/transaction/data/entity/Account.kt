package com.example.transaction.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bankKeyword: String, // Keyword to match in SMS (e.g., "HDFC", "SBI")
    val isActive: Boolean = true
)
