package com.example.transaction.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.transaction.data.AppDatabase
import com.example.transaction.data.entity.SettingsEntity
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive triggered: action = ${intent.action}")
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    if (messages.isNullOrEmpty()) {
                        Log.w("SmsReceiver", "No messages found in intent")
                        return@launch
                    }

                    val fullMessage = StringBuilder()
                    var sender = ""
                    var timestamp = System.currentTimeMillis()

                    for (smsMessage in messages) {
                        fullMessage.append(smsMessage.displayMessageBody)
                        sender = smsMessage.displayOriginatingAddress ?: ""
                        timestamp = smsMessage.timestampMillis
                    }

                    if (fullMessage.isNotEmpty()) {
                        Log.d("SmsReceiver", "Processing message from $sender, time: $timestamp")
                        processSms(context.applicationContext, sender, fullMessage.toString(), timestamp)
                        
                        // Update last sync time so the manual sync doesn't pick it up
                        val db = AppDatabase.getDatabase(context.applicationContext)
                        val settings = db.settingsDao().getSettings().firstOrNull() ?: SettingsEntity()
                        db.settingsDao().updateSettings(settings.copy(lastSyncTime = timestamp))
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error in async processing", e)
                } finally {
                    Log.d("SmsReceiver", "Finishing async pending result")
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun processSms(context: Context, sender: String, body: String, timestamp: Long) {
        val db = AppDatabase.getDatabase(context)
        val accounts = db.accountDao().getActiveAccounts()
        
        Log.d("SmsReceiver", "Checking match for $sender against ${accounts.size} accounts")
        
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
            Log.d("SmsReceiver", "Matched account: ${matchingAccount.name}")
            val parsed = SmsParser.parse(body)
            if (parsed != null) {
                val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
                val transaction = TransactionEntity(
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    category = parsed.category,
                    accountId = matchingAccount.id,
                    date = timestamp,
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                db.transactionDao().insertTransaction(transaction)
                Log.d("SmsReceiver", "SUCCESS: Transaction saved: ${parsed.amount} ${parsed.type} at ${parsed.merchant}")
            } else {
                Log.w("SmsReceiver", "FAILED: Parser could not understand message format")
            }
        } else {
            Log.d("SmsReceiver", "SKIPPED: No account keyword matched this SMS. Keywords checked in: $cleanBody")
        }
    }
}
