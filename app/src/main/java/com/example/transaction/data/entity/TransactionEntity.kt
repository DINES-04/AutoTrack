package com.example.transaction.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["amount", "date", "accountId"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val merchant: String,
    val category: String,
    val accountId: Long,
    val date: Long, // Timestamp
    val month: Int, // 1-12
    val year: Int
)
