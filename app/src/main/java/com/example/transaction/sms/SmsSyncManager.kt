package com.example.transaction.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.example.transaction.data.AppDatabase
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class SmsSyncManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)

    suspend fun syncMissedSms() {
        try {
            val settings = db.settingsDao().getSettings().firstOrNull() ?: SettingsEntity()
            val lastSync = settings.lastSyncTime
            val currentTime = System.currentTimeMillis()

            // If it's the first time, don't sync everything from the past, just start from now
            // or maybe sync last 24 hours. Let's do last 7 days or since lastSync.
            val startTime = if (lastSync == 0L) {
                currentTime - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
            } else {
                lastSync + 1
            }

            Log.d("SmsSyncManager", "Starting sync from $startTime to $currentTime")
            
            val accounts = db.accountDao().getActiveAccounts()
            if (accounts.isEmpty()) {
                Log.d("SmsSyncManager", "No active accounts, skipping sync")
                return
            }

            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(startTime.toString()),
                "${Telephony.Sms.DATE} ASC"
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                var count = 0
                var latestTimestamp = startTime
                while (it.moveToNext()) {
                    val sender = it.getString(addressIndex)
                    val body = it.getString(bodyIndex)
                    val date = it.getLong(dateIndex)

                    if (processSingleSms(sender, body, date, accounts)) {
                        count++
                    }
                    if (date > latestTimestamp) {
                        latestTimestamp = date
                    }
                }
                Log.d("SmsSyncManager", "Sync complete. Processed $count transactions")
                
                // Update last sync time to the latest processed message timestamp
                db.settingsDao().updateSettings(settings.copy(lastSyncTime = latestTimestamp))
            }

        } catch (e: Exception) {
            Log.e("SmsSyncManager", "Error during SMS sync", e)
        }
    }

    private suspend fun processSingleSms(sender: String, body: String, date: Long, accounts: List<com.example.transaction.data.entity.Account>): Boolean {
        val cleanBody = body.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val cleanSender = sender.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()

        val matchingAccount = accounts.find { account ->
            val keywords = account.bankKeyword.split(",")
                .map { it.trim().lowercase().replace(Regex("[^a-zA-Z0-9]"), "") }
                .filter { it.isNotEmpty() }
            
            keywords.any { keyword ->
                cleanBody.contains(keyword) || cleanSender.contains(keyword)
            }
        }

        if (matchingAccount != null) {
            val parsed = SmsParser.parse(body)
            if (parsed != null) {
                // Check if this transaction already exists (optional but good for de-duplication)
                // For now, we rely on the timestamp being after lastSync, but a more robust check 
                // would be a unique constraint or checking amount/merchant/time.
                
                val calendar = Calendar.getInstance().apply { timeInMillis = date }
                val transaction = TransactionEntity(
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    category = parsed.category,
                    accountId = matchingAccount.id,
                    date = date,
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                db.transactionDao().insertTransaction(transaction)
                return true
            }
        }
        return false
    }
}
