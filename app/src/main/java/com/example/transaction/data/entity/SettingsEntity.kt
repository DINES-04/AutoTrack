package com.example.transaction.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0, // Single row for settings
    val monthlyBudget: Double = 0.0,
    val currency: String = "₹"
)
