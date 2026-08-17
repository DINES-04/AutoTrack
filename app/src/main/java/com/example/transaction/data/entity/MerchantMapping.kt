package com.example.transaction.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMapping(
    @PrimaryKey val merchantName: String, // Normalized merchant name
    val category: String,
    val isCustom: Boolean = false
)
