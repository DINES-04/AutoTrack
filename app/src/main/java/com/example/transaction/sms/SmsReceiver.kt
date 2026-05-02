package com.example.transaction.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.example.transaction.data.AppDatabase
import com.example.transaction.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>
                val messages = arrayOfNulls<SmsMessage>(pdus.size)
                val fullMessage = StringBuilder()
                var sender = ""

                for (i in pdus.indices) {
                    messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                    fullMessage.append(messages[i]?.messageBody)
                    sender = messages[i]?.originatingAddress ?: ""
                }

                processSms(context, sender, fullMessage.toString())
            }
        }
    }

    private fun processSms(context: Context, sender: String, body: String) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val accounts = db.accountDao().getActiveAccounts()
            
            // Check if sender matches any bank keyword
            val matchingAccount = accounts.find { account -> 
                body.contains(account.bankKeyword, ignoreCase = true) || 
                sender.contains(account.bankKeyword, ignoreCase = true)
            }

            if (matchingAccount != null) {
                val parsed = SmsParser.parse(body)
                if (parsed != null) {
                    val calendar = Calendar.getInstance()
                    val transaction = TransactionEntity(
                        amount = parsed.amount,
                        type = parsed.type,
                        merchant = parsed.merchant,
                        category = "Others", // Auto-detection can be added later
                        accountId = matchingAccount.id,
                        date = System.currentTimeMillis(),
                        month = calendar.get(Calendar.MONTH) + 1,
                        year = calendar.get(Calendar.YEAR)
                    )
                    db.transactionDao().insertTransaction(transaction)
                }
            }
        }
    }
}
